(ns salava.user.cron
  (:require 
   [salava.user.email-notifications :as en]))



(defn every-minute [ctx]
  (do
    (en/email-sender ctx)))
