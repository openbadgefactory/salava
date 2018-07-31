(ns salava.badge.verify
  (:require [salava.badge.main :as b]
            [salava.core.http :as http]
            [salava.core.time :refer [iso8601-to-unix-time unix-time]]
            [clojure.tools.logging :as log]
            #_[clj-http.client :as client]))

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

(defn verify-badge [ctx asr user-id]
  (log/info "Badge verification initiated:")
  (let [asr-response (assertion asr)
        result {}]
    (case (:status asr-response)
      410 (assoc result
            :assertion-status 410
            :revoked? true)
      500 (assoc result :assertion-status 500
            :badge-status "Broken url, badge can't be verified")
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
              :badge-image-status (:status badge-image)
              :badge-criteria-status (:status badge-criteria)
              :badge-issuer-status (:status badge-issuer)
              :revoked? revoked?
              :expired? expired?
              )))))
