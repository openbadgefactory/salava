(ns salava.core.mail
  (:require [clojure.java.io :as io]
            [hiccup.core :refer :all]
            [postal.core :refer [send-message]]
            [slingshot.slingshot :refer :all]
            [salava.core.i18n :refer [t]]))

(defn send-mail [ctx subject message recipients]
  (try+
    (let [mail-host-config (get-in ctx [:config :core :mail-host-config])
          data {:from    (get-in ctx [:config :core :mail-sender])
                :bcc      recipients
                :subject subject
                :body    [{:type    "text/plain; charset=utf-8"
                           :content message}]}]
      (if (nil? mail-host-config)
        (send-message data)
        (send-message mail-host-config data)))
    (catch Object _
      ;TODO log an error
      )))

(defn send-activation-message [ctx site-url activation-link login-link fullname email-address lng]
  (let [site-name (get-in ctx [:config :core :site-name])
        subject (str (t :core/Welcometo lng) " " site-name (t :core/Service lng))
        message (str fullname
                     ",\n\n" (t :core/Emailactivation2 lng) " " site-url  ".\n" (t :core/Emailactivation4 lng) ":\n\n"
                     activation-link
                     "\n\n" (t :core/Emailactivation5 lng) "\n" (t :core/Emailactivation6 lng) ".\n\n" (t :core/Emailactivation7 lng) "\n"
                     login-link
                     " " (t :core/Emailactivation8 lng) ".\n\n--  "site-name " -"(t :core/Team lng))]
    (send-mail ctx subject message [email-address])))

(defn send-password-reset-message [ctx site-url activation-link fullname email-address lng]
  (let [site-name (get-in ctx [:config :core :site-name])
        subject (str  site-name " " (t :core/Emailresetheader lng))
        message (str fullname ",\n\n" (t :core/Emailresetmessage1 lng) " " site-url
                     ".\n\n" (t :core/Emailactivation4 lng)":\n\n"
                     activation-link
                     "\n\n" (t :core/Emailactivation5 lng) "\n" (t :core/Emailactivation6 lng) ".\n\n" (t :core/Emailresetmessage2 lng) ".\n\n--  "
                      site-name " -"(t :core/Team lng))]
    (send-mail ctx subject message [email-address])))

(defn send-verification [ctx site-url email-verification-link fullname email lng]
  (let [subject (str (t :core/Emailverification1 lng))
        message (str fullname "\n\n" (t :core/Emailverification2 lng) " '" email "' " (t :core/Emailverification3 lng) " " site-url".\n" (t :core/Emailverification4 lng) ":\n\n"
                     email-verification-link
                     "\n\n" (t :core/Emailverification6 lng)".\n")]
    (send-mail ctx subject message [email])))
