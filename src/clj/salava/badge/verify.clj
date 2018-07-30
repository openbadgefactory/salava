(ns salava.badge.verify
  (:require [salava.badge.main :as b]
            [salava.core.http :as http]
            [salava.core.time :refer [iso8601-to-unix-time unix-time]]))

(defn fetch-json-data [url]
  #_(log/info "fetch-json-data: GET" url)
  (http/http-get url {:as :json :accept :json :throw-entire-message? true}))

(defn- assertion [assertion-url]
  (http/http-req {:socket-timeout 10000
                  :conn-timeout   10000
                  :method :get
                  :url assertion-url
                  :throw-exceptions false
                  :as :json
                  :accept :json}))

(defn verify-badge [ctx badge-id user-id]
  (let [badge (b/get-badge ctx badge-id user-id)
        assertion-url (:assertion_url badge)
        asr-response (assertion assertion-url)
        asr-data (:body asr-response)
        assertion-badge-url (:badge asr-data)
        badge-data (http/http-get assertion-badge-url {:as :json :accept :json :throw-entire-message? true})
        ;criteria (if (map? (:criteria badge-data)) (fetch-json-data (get-in badge-data [:criteria :id])) (fetch-json-data (:criteria badge-data)))
        revoked? (or (= (:status asr-response) 410) (:revoked asr-data))
        expired? (and (:expires asr-data) (< (iso8601-to-unix-time (:expires asr-data)) (unix-time)))]
   (prn (:criteria badge-data))
  {:revoked revoked?
   :expired expired?
   :badge {
            }
   }

    )
)
