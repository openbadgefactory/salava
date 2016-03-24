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
                :to      recipients
                :subject subject
                :body    [{:type    "text/plain; charset=utf-8"
                           :content message}]}]
      (if (nil? mail-host-config)
        (send-message data)
        (send-message mail-host-config data)))
    (catch Object _
      ;TODO log an error
      )))

(defn send-activation-message [ctx site-url activation-link login-link fullname email-address]
  (let [subject (str "Account details for " fullname " at " site-url)
        message (str fullname
                     ",\n\nThank you for registering at " site-url ". You may now log in by\nclicking this link or copying and pasting it to your browser:\n\n"
                     activation-link
                     "\n\nThis link can only be used once to log in and will lead you to a page where\nyou can set your password.\n\nAfter setting your password, you will be able to log in at\n"
                     login-link
                     " in the future using: your e-mail address and password.\n\n--  " site-url " team")]
    (send-mail ctx subject message [email-address])))

(defn send-password-reset-message [ctx site-url activation-link fullname email-address]
  (let [subject (str "Replacement login information for " fullname " at " site-url)
        message (str fullname ",\n\nA request to reset the password for your account has been made at\n" site-url
                     ".\n\nYou may now log in by clicking this link or copying and pasting it to your\nbrowser:\n\n"
                     activation-link
                     "\n\nThis link will lead you to a page where\nyou can set your password. It expires after one day and nothing will happen\nif it's not used.\n\n--  "
                     site-url" team")]
    (send-mail ctx subject message [email-address])))

(defn send-verification [ctx site-url email-verification-link fullname email]
  (let [subject (str "Confirm your e-mail address at  " site-url)
        message (str fullname "\n\nYou have added the e-mail address '" email "' to your\naccount at " site-url". In order to complete the registration of\nthis email, you must confirm it by clicking the link below.\n\n"
                     email-verification-link
                     "\n\nIf the web address does not appear as a link, you must copy the address out\nof this email, and paste it into the address bar of your web browser.\n\nIf you do not confirm this e-mail in 5 days, it will be unregistered from\nyour account.\n")]
    (send-mail ctx subject message [email])))
