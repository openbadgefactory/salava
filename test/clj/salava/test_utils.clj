(ns salava.test-utils
  (:require [clj-http.client :as client]
            [slingshot.slingshot :refer :all]
            [salava.core.components.config :as c]))

(def api-root
  (let [config (c/load-config :core)
        host (get-in config [:http :host])
        port (get-in config [:http :port])]
    (str "http://" host ":"port "/obpv1" )))

(defn test-api-request
  ([method url] (test-api-request method url {}))
  ([method url post-params]
   (try+
     (client/request
       {:method       method
        :url          (str api-root url)
        :content-type :json
        :as :json
        :form-params  post-params
        :throw-exceptions false})
     (catch Exception _))))

(defn test-upload [url upload-data]
  (try+
    (client/request
      {:method           :post
       :url              (str api-root url)
       :multipart        upload-data
       :as               :json
       :throw-exceptions false})
    (catch Exception _)))


