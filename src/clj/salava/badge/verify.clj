(ns salava.badge.verify
  (:require [salava.badge.main :as b]
            [salava.core.http :as http]
            [salava.core.time :refer [iso8601-to-unix-time unix-time]]
            [clojure.tools.logging :as log]
            [salava.core.util :refer [get-db]]
            [yesql.core :refer [defqueries]]
            [salava.badge.parse :refer [str->badge]]
            [clojure.data.json :as json]))

(defqueries "sql/badge/main.sql")

(defn url? [s]
  (not (clojure.string/blank? (re-find #"^http" (str s)))))

(defn fetch-json-data [url]
  (log/info "fetch-badge-data: GET" url)
  (http/http-get url {:as :json :accept :json :throw-entire-message? false}))

(defn fetch-image [url]
  (log/info "fetch image from " url)
  (try
    (http/http-req {:url url :method :get :throw-exceptions false :as :byte-array})
    (catch Exception e
      (log/error "failed to get image from " url " Message: " (.getMessage e))
      {:status 500})))

(defn fetch-url [url]
  (log/info "fetch response from " url)
  (try
    (http/http-req {:url url :method :get :throw-exceptions false})
    (catch Exception e
      (log/error "failed to get response from " url " Message: " (.getMessage e))
      {:status 500})))


(defn- assertion [assertion-url]
  (try
    (http/http-req  {:url assertion-url :as :json :method :get  :accept :json :throw-exceptions false })
    (catch Exception e
      (log/error "failed to get assertion url " assertion-url " Message: "(.getMessage e) )
      {:status 500})))

(defn- assertion-jws [b signature]
  (log/info "fetching assertion from signature")
  (try
    (str->badge {:id (:user_id b) :emails [(:email b)]} signature)
    (catch Exception e
      (log/error (.getMessage e))
      {:status 500 :message (.getMessage e)})))

(defn revoked? [badge-id revocation-list]
  (filter #(if (map? %) (= badge-id (:id %)) (= badge-id %)) revocation-list))

(defn verify-badge [ctx badge_id user-id]
  (log/info "Badge verification initiated:")
  (let [badge (b/get-badge ctx badge_id user-id)
        asr (if (clojure.string/blank? (:assertion_url badge)) (get-assertion-jws {:id badge_id} (into {:result-set-fn first :row-fn :assertion_jws} (get-db ctx))) (:assertion_url badge))
        result {}]
    (if (url? asr)
      (let [asr-response (assertion asr)]
        (case (:status asr-response)
          410 (assoc result
                :assertion-status 410
                :revoked? true)
          500 (assoc result :assertion-status 500
                :asr asr
                :badge-status "Broken assertion url, badge can't be verified")
          200 (let [asr-data (:body asr-response)
                    badge-data (fetch-json-data (:badge asr-data))
                    badge-image (if (map? (:image badge-data)) (fetch-image (get-in badge-data [:image :id])) (fetch-image (:image badge-data)))
                    badge-criteria (if (and (map? (:criteria badge-data))(contains? (:criteria badge-data) :id))
                                     (fetch-url (get-in badge-data [:criteria :id]))
                                     (if (url? (:criteria badge-data)) (fetch-url (:criteria badge-data)) {:status 800}))
                    badge-issuer (if (and (map? (:issuer badge-data)) (contains? (:issuer badge-data) :id))
                                   (fetch-url (get-in badge-data [:issuer :id]))
                                   (if (url? (:issuer badge-data)) (fetch-url (:issuer badge-data)) {:status 800}))
                    revoked? (or (= (:status asr-response) 410) (:revoked asr-data))
                    expired? (and (:expires asr-data) (< (iso8601-to-unix-time (:expires asr-data)) (unix-time)))]

                (assoc result :assertion-status 200
                  :assertion asr-data
                  :asr asr
                  :badge-image-status (:status badge-image)
                  :badge-criteria-status (:status badge-criteria)
                  :badge-issuer-status (:status badge-issuer)
                  :revoked? revoked?
                  :expired? expired?
                  ))))
      (let [jws-response (assertion-jws badge asr)
            badge-id (or (:id (json/read-str (:assertion_json jws-response) :key-fn keyword)) (:uid (json/read-str (:assertion_json jws-response) :key-fn keyword)))
            revocation-list-url (:revocation_list_url (first (get-in jws-response [:badge :issuer])))
            revocation-list (if revocation-list-url (:revokedAssertions (fetch-json-data revocation-list-url)))
            revoked (revoked? badge-id revocation-list)
            revocation-reason (:revocationReason (first revoked))]
        (if (= 500 (:status jws-response))
          (assoc result :assertion-status 500
            :asr asr
            :badge-status (:message jws-response))
          (assoc result :assertion-status 200
            :assertion (json/read-str (:assertion_json jws-response) :key-fn keyword )
            :asr asr
            :badge-image-status 200
            :badge-criteria-status 200
            :badge-issuer-status 200
            :revoked? (not (empty? revoked))
            :revocation_reason revocation-reason
            :expired? (if (:expires_on jws-response) (< (:expires_on jws-response) (unix-time)))
            )
          )
        )
      )
    )
  )
