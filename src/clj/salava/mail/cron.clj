(ns salava.mail.cron
  (:require
   [salava.mail.email-notifications :as en]
   [clojure.tools.logging :as log]))


(defn every-day [ctx]
  (log/info "Email notifications disabled")
  #_(do
      (log/info "start")
      ;(en/email-sender ctx)
      (log/info "stop")))
