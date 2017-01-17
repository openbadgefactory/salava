(ns salava.mail.email-notifications
  (:require [yesql.core :refer [defqueries]]
            [clojure.tools.logging :as log]
            [clojure.set :refer [rename-keys]]
            [clojure.java.jdbc :as jdbc]
            [salava.core.helper :refer [dump]]
            [slingshot.slingshot :refer :all]
            [salava.mail.mail :refer [send-html-mail html-mail-template]]
            [salava.core.i18n :refer [t]]
            [salava.core.util :refer [get-db get-datasource get-site-url get-base-path get-site-name get-email-notifications get-plugins plugin-fun]]
            [salava.core.time :refer [get-day-of-week]]
            [salava.user.db :refer [get-user-and-primary-email get-user-ids-from-event-owners]] ))


(defn email-reminder-body [ctx user]
  (try
    (Thread/sleep 50)
    (catch InterruptedException _))
  (try+
   (let [lng (:language user)
         site-name (get-in ctx [:config :core :site-name] "Open Badge Passport")
         full-name (str (:first_name user) " " (:last_name user))
         subject (str site-name ": " (t :user/Emailnotificationsubject lng))
         message (html-mail-template ctx user lng subject "email-notifications")
         ]
     (if (and message user)
       (send-html-mail ctx subject message [(:email user)])))
     (catch Object ex
       (log/error "failed to send email notification to user:")
       (log/error (.toString ex)))))


(defn email-sender [ctx]
  (if (get-email-notifications ctx)    
    (let [event-owners      (get-user-ids-from-event-owners ctx)
          day               (dec (get-day-of-week))
          current-day-users (filter #(= day (rem (:id %) 7)) event-owners)]
      (doseq [user current-day-users]
        (email-reminder-body ctx user)))))






