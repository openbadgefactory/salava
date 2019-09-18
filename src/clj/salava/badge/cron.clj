(ns salava.badge.cron
  (:require [clojure.tools.logging :as log]
            [clojure.java.jdbc :as jdbc]
            [clojure.string :as string]
            [clj-time.core :as t]
            [salava.core.util :as u]
            [salava.core.http :as http]
            [clojure.data.json :as json]))

#_(defn- assertion [assertion-url]
   (http/http-req {:socket-timeout 10000
                   :conn-timeout   10000
                   :method :get
                   :url assertion-url
                   :throw-exceptions false
                   :as :json
                   :accept :json}))

(defn- assertion [asr-url]
 (let [response (->> (http/http-req {:socket-timeout 10000
                                     :conn-timeout 10000
                                     :method :get
                                     :url asr-url
                                     :throw-exceptions false
                                     :accept :json})
                     (clojure.walk/keywordize-keys))]
   (if (clojure.string/includes? (get-in response [:headers :Content-Type]) "application/json")
       (assoc response :body (json/read-str (:body response) :key-fn keyword))
       nil)))

(defn- revoked? [asr]
  (or (= (:status asr) 410) (get-in asr [:body :revoked])))

#_(defn- remote-url [factory-url asr]
   (when (and (string? factory-url) (map? asr))
     (cond
       (string/starts-with? (get asr :id "") factory-url) factory-url
       (string/starts-with? (get-in asr [:verify :url] "") factory-url) factory-url
       (and (= (get-in asr [:related :type]) "Assertion")
            (string/starts-with? (get (http/json-get (get-in asr [:related :id])) :id "") factory-url)) factory-url
       :else nil)))

(defn- remote-url [factory-url asr]
 (when (and (string? factory-url) (not (clojure.string/blank? factory-url)) (map? asr))
  (let [related-asr (->> (get asr :related) first :id)]
   (if (and (not (clojure.string/blank? (:id asr)))
            (not (clojure.string/blank? related-asr))
            (= (second (clojure.string/split (get asr :id "") #"/v1")) (second (clojure.string/split related-asr #"/v1")))
            (= (first (clojure.string/split related-asr #"/v1")) factory-url))
     (first (clojure.string/split related-asr #"/v1"))
     (cond
       (string/starts-with? (get asr :id "") factory-url) factory-url
       (string/starts-with? (get-in asr [:verify :url] "") factory-url) factory-url
       (and (= (get-in asr [:related :type]) "Assertion")
            (string/starts-with? (get (http/json-get (get-in asr [:related :id])) :id "") factory-url)) factory-url
           :else nil)))))

#_(defn- issuer-verified? [factory-url asr]
   (when (and (string? factory-url) (map? asr) (string/starts-with? (get asr :id "") factory-url))
     (try
       (let [badge (if (map? (:badge asr)) (:badge asr) (http/json-get (:badge asr)))
             issuer-url (if (map? (:issuer badge)) (get-in badge [:issuer :id]) (:issuer badge))
             issuer-id (last (re-find #"/v1/client.*[?&]key=(\w+)" issuer-url))]
         (if issuer-id
           (:verified (http/json-get (str factory-url "/v1/client?key=" issuer-id)))
           false))
       (catch Throwable _ false))))

(defn- issuer-verified? [factory-url asr]
 (let [factory-url (remote-url factory-url asr)]
   (try
     (let [badge (if (map? (:badge asr)) (:badge asr) (http/json-get (:badge asr)))
           issuer-url (if (map? (:issuer badge)) (get-in badge [:issuer :id]) (:issuer badge))
           issuer-id (last (re-find #"/v1/client.*[?&]key=(\w+)" issuer-url))]
       (if issuer-id
         (:verified (http/json-get (str factory-url "/v1/client?key=" issuer-id)))
         false))
     (catch Throwable _ false))))

(defn- check-badge [ctx db-conn factory-url user-badge]
  (let [asr (assertion (:assertion_url user-badge))
        factory-url (remote-url factory-url (:body asr))
        delete-user-metabadge (first (u/plugin-fun (u/get-plugins ctx) "db" "clear-user-metabadge!"))
        check-metabadge (first (u/plugin-fun (u/get-plugins ctx) "db" "metabadge?!"))]
    (if (revoked? asr)
      (do
        (jdbc/execute! db-conn
                       ["UPDATE user_badge SET revoked = 1, last_checked = UNIX_TIMESTAMP() WHERE id = ?"
                        (:id user-badge)])
        (if delete-user-metabadge (delete-user-metabadge ctx (:id user-badge))))

      ;; still valid, check verified issuer
      (jdbc/with-db-transaction [t-con db-conn]
        (jdbc/execute! t-con ["UPDATE badge SET remote_url = ? WHERE id = ? AND remote_url IS NULL"
                              (remote-url factory-url (:body asr)) (:badge_id user-badge)])
        (jdbc/execute! t-con ["UPDATE badge SET issuer_verified = ? WHERE id = ? AND remote_url IS NOT NULL"
                              (if (issuer-verified? factory-url (:body asr))
                                1
                                0) (:badge_id user-badge)])
        (if (and check-metabadge (not (clojure.string/blank? factory-url))) (check-metabadge ctx factory-url user-badge))))))

(defn- check-badges [ctx db-conn factory-url]
  (log/info "check-badges: started working")
  (let [time-limit (+ (System/currentTimeMillis) (* 30 60 1000))
        sql "SELECT id, badge_id, assertion_url FROM user_badge
        WHERE assertion_url IS NOT NULL
        AND deleted = 0 AND revoked = 0 AND status = 'accepted'
        AND (expires_on IS NULL OR expires_on > UNIX_TIMESTAMP())
        ORDER BY last_checked LIMIT 1000"]
    (doseq [chunk (partition-all 10 (jdbc/query db-conn [sql]))]
      (when (> time-limit (System/currentTimeMillis))
        (doseq [user-badge chunk]
          (try
            ;(log/debug "check-badges: working on id " (:id user-badge))
            (check-badge ctx db-conn factory-url user-badge)
            (jdbc/execute! db-conn ["UPDATE user_badge SET last_checked = UNIX_TIMESTAMP() WHERE id = ?"
                                    (:id user-badge)])
            (Thread/sleep 100)
            (catch InterruptedException _)
            (catch Throwable ex
              (log/error "check-badges failed")
              (log/error (.toString ex)))))
        (try (Thread/sleep 1000) (catch InterruptedException _)))))
  (log/info "check-badges: done"))

;;;


(defn every-hour [ctx]
  (let [h (t/hour (t/now))]
    (when (and (not= h 14) (not= h 15)) ; skip during afternoon hours
      (check-badges ctx (:connection (u/get-db ctx)) (get-in ctx [:config :factory :url])))))
