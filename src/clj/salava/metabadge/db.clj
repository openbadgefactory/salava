(ns salava.metabadge.db
  (:require [salava.core.http :as http]
            [salava.core.util :as u]
            [clojure.tools.logging :as log]))

(defn fetch-json-data [url]
  (log/info "fetch-json-data: GET" url)
  (http/http-get url {:as :json :accept :json :throw-entire-message? true}))

(defn check-metabadge [ctx assertion-url]
  (log/info "check if badge is a metabadge")
  (let [meta-data-url (str (get-in ctx [:config :factory :url]) "/v1/assertion/metabadge/?url=" assertion-url #_(u/url-encode assertion-url))
        ;meta-data (http/json-get meta-data-url)
        meta-data (fetch-json-data meta-data-url)
        ]
    (prn meta-data)
      meta-data))
