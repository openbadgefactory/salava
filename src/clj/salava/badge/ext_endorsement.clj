(ns salava.badge.ext-endorsement
 (:require
  [clojure.tools.logging :as log]
  [hiccup.core :refer [html]]
  [postal.core :refer [send-message]]
  [slingshot.slingshot :refer :all]))
  ;[salava.mail.mail :refer [send-mail-to]]

(defn- request-template [id]
  (html
   [:div {:class "logo-image-url"}]))

(defn send-request [ctx subject message to]
  (let [mail-host-config (get-in ctx [:config :core :mail-host-config])
        data {:from    (get-in ctx [:config :core :mail-sender])
              :subject subject
              :body    [{:type    "text/html"
                         :content (request-template nil)#_(html message)}]}]
    (try+
      (log/info "sending to" to)
      (-> (if (nil? mail-host-config)
            (send-message (assoc data :to to))
            (send-message mail-host-config (assoc data :to to)))
          log/info)
      (catch Object ex
        (log/error ex)))))
