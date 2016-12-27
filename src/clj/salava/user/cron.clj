(ns salava.user.cron
  (:require 
   [salava.user.email-notifications :as en]))


;;every-minute
(defn every-day [ctx]
  (do
    (en/email-sender ctx)))
