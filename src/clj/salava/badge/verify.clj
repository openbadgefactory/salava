(ns salava.badge.verify
  (:require [salava.badge.main :as b]
            [salava.core.http :as http]
            [salava.core.time :refer [iso8601-to-unix-time unix-time date-from-unix-time]]
            [clojure.tools.logging :as log]
            [salava.core.util :refer [get-db plugin-fun get-plugins str->epoch]]
            [yesql.core :refer [defqueries]]
            [salava.badge.parse :refer [str->badge]]
            [clojure.data.json :as json]
            [autoclave.core :refer [json-sanitize]]
            [slingshot.slingshot :refer :all]))

(defqueries "sql/badge/main.sql")

(defn url? [s]
  (not (clojure.string/blank? (re-find #"^http" (str s)))))

(defn- fetch-json-data [url]
  (log/info "fetch-json-data: GET" url)
  (try
    (http/json-get url)
    (catch Exception _
      (log/error (.getMessage _))
      (log/error "Error occured when getting json content with json-get! Using http-get instead")
      (as-> (http/http-get url) $
            (if (map? $) $ (-> $ (json-sanitize) (json/read-str :key-fn keyword)))))))

(defn fetch-image [url]
  (log/info "fetch image from " url)
  (try
    (http/http-req {:url url :method :get :throw-exceptions false :as :byte-array})
    (catch Exception e
      (log/error "failed to get image from " url " Message: " (.getMessage e))
      (if (re-find #"^data:image" url)
        {:status 200}
        {:status 500}))))

(defn fetch-url [url]
  (log/info "fetch response from " url)
  (try
    (http/http-req {:url url :method :get :throw-exceptions false})
    (catch Exception e
      (log/error "failed to get response from " url " Message: " (.getMessage e))
      {:status 500})))

(defn- assertion [assertion-url]
  (try
    (http/http-req  {:url assertion-url :as :json :method :get  :accept :json :throw-exceptions false})
    (catch Exception e
      (log/error "failed to get assertion url " assertion-url " Message: " (.getMessage e))
      {:status 500 :message (.getMessage e)})))

(defn- assertion-jws [b signature]
  (log/info "fetching assertion from signature")
  (try
    (str->badge {:id (:user_id b) :emails [(:email b)]} signature)
    (catch Exception e
      (log/error (.getMessage e))
      {:status 500 :message (.getMessage e)})))

(defn revoked? [badge-id revocation-list]
  (filter #(if (map? %) (= badge-id (:id %)) (= badge-id %)) revocation-list))

(defn expired? [t]
  (if (nil? t) nil (if (string? t) (< (iso8601-to-unix-time t) (unix-time)) (< t (unix-time)))))

(defn process-time [t]
  (try+
   (if (clojure.string/blank? t) nil (date-from-unix-time (* 1000 (str->epoch t)) "date"))
   #_(if (nil? t) nil (if (string? t) (date-from-unix-time (long (* 1000 (iso8601-to-unix-time t))) "date") (date-from-unix-time (long (* 1000 t)) "date")))
   (catch Object _
    (log/error "An error occured when parsing time")
    nil)))

(defn verify-badge [ctx id]
  (log/info "Badge verification initiated:")
  (let [badge (b/fetch-badge ctx id)]
    (if-let [f (empty? (-> badge (dissoc :content)))]
      (hash-map :assertion-status 500
                :asr ""
                :message "badge does not exist")

      (let [asr (if (clojure.string/blank? (:assertion_url badge)) (get-assertion-jws {:id (:id badge)} (into {:result-set-fn first :row-fn :assertion_jws} (get-db ctx))) (:assertion_url badge))
            result {}
            delete-user-metabadge (first (plugin-fun (get-plugins ctx) "db" "clear-user-metabadge!"))]
        (if (url? asr)
          (let [asr-response (assertion asr)]
            (case (:status asr-response)
              404 (assoc result :assertion-status 404
                         :asr asr)
              410 (do
                    (update-revoked! {:revoked 1 :id (:id badge)} (get-db ctx))
                    (update-visibility! {:visibility "private" :id (:id badge)} (get-db ctx))
                    (if delete-user-metabadge (delete-user-metabadge ctx id))
                    (assoc result :assertion-status 410
                           :asr asr
                           :revoked? true))
              500 (assoc result :assertion-status 500
                         :asr asr
                         :message (:message asr-response))
              200 (let [asr-data (:body asr-response)
                        badge-data (if (url? (:badge asr-data)) (fetch-json-data (:badge asr-data)) (:badge asr-data))
                        badge-image (if (map? (:image badge-data))
                                      (fetch-image (get-in badge-data [:image :id]))
                                      (fetch-image (:image badge-data)))
                        badge-criteria (if (and (map? (:criteria badge-data)) (contains? (:criteria badge-data) :id))
                                         (if-not (clojure.string/blank? (get-in badge-data [:criteria :id]))
                                            (fetch-url (get-in badge-data [:criteria :id]))
                                            (get-in badge-data [:criteria :narrative]))
                                         (if (url? (:criteria badge-data)) (fetch-url (:criteria badge-data)) {:status 800}))
                        badge-issuer (if (and (map? (:issuer badge-data)) (contains? (:issuer badge-data) :id))
                                       (fetch-url (get-in badge-data [:issuer :id]))
                                       (if (url? (:issuer badge-data)) (fetch-url (:issuer badge-data)) {:status 800}))
                        issuedOn {:issuedOn (process-time (:issuedOn asr-data))}
                        expires (if-let [exp (process-time (:expires asr-data))] {:expires exp} nil)

                        revoked? (or (= (:status asr-response) 410) (:revoked asr-data))
                        expired? (expired? (:expires asr-data)) #_(and (:expires asr-data) (< (iso8601-to-unix-time (:expires asr-data)) (unix-time)))]
                    (assoc result :assertion-status 200
                           :assertion (-> asr-data (merge issuedOn expires) (dissoc :related)) #_(merge asr-data issuedOn expires)
                           :asr asr
                           :badge-image-status (:status badge-image)
                           :badge-criteria-status (:status badge-criteria)
                           :badge-issuer-status (:status badge-issuer)
                           :revoked? revoked?
                           :expired? expired?))
              (assoc result :assertion-status 500
                     :asr asr
                     :message (:reason-phrase asr-response))))

          (let [jws-response (assertion-jws badge asr)]
            (if (= 500 (:status jws-response))
              (assoc result :assertion-status 500
                     :asr asr
                     :message (:message jws-response))
              (let [badge-id (or (:id (json/read-str (:assertion_json jws-response) :key-fn keyword)) (:uid (json/read-str (:assertion_json jws-response) :key-fn keyword)))
                    revocation-list-url (:revocation_list_url (first (get-in jws-response [:badge :issuer])))
                    revocation-list (if revocation-list-url (:revokedAssertions (fetch-json-data revocation-list-url)))
                    revoked (revoked? badge-id revocation-list)
                    revocation-reason (:revocationReason (first revoked))
                    assertion (json/read-str (:assertion_json jws-response) :key-fn keyword)
                    issuedOn {:issuedOn (process-time (:issuedOn assertion))}
                    expires (if-let [exp (process-time (:expires assertion))] {:expires exp} nil)]

                (when-not (empty? revoked)
                  (update-revoked! {:revoked 1 :id (:id badge)} (get-db ctx))
                  (update-visibility! {:visibility "private" :id (:id badge)} (get-db ctx)))

                (assoc result :assertion-status 200
                       :assertion (-> assertion (merge issuedOn expires) (dissoc :related))
                       :asr asr
                       :badge-image-status 200
                       :badge-criteria-status 200
                       :badge-issuer-status 200
                       :revoked? (not (empty? revoked))
                       :revocation_reason revocation-reason
                       :expired? (expired? (:expires_on jws-response)) #_(if (:expires_on jws-response) (< (:expires_on jws-response) (unix-time))))))))))))
