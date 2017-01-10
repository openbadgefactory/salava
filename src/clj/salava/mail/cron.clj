(ns salava.mail.cron
  (:require 
   [salava.mail.email-notifications :as en]
   [clojure.tools.logging :as log]))


;;every-minute
(defn every-minute [ctx]
  (do
    (println "LOOL")
    (log/info "start")
    (en/email-sender ctx)
    (log/info "stop")))
