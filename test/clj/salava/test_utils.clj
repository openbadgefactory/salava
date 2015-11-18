(ns salava.test-utils
  (:require [clj-http.client :as client]
            [salava.core.components.config :as c]))

(def api-host
  (let [config (c/load-config :core)
        host (get-in config [:http :host])
        port (get-in config [:http :port])]
    (str "http://" host ":"port "/obpv1" )))

(defn test-api-request
  ([method url] (test-api-request method url {}))
  ([method url post-params]
   (client/request
     {:method       method
      :url          (str api-host url)
      :content-type :json
      :as :json
      :form-params  post-params
      :throw-exceptions false})))

(defn test-upload [url upload-data]
  (client/request
    {:method           :post
     :url              (str api-host url)
     :multipart        upload-data
     :as               :json
     :throw-exceptions false}))


