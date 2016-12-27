(ns salava.user.email-notifications
  (:require [yesql.core :refer [defqueries]]
            [clojure.set :refer [rename-keys]]
            [clojure.java.jdbc :as jdbc]
            [salava.core.helper :refer [dump]]
            [slingshot.slingshot :refer :all]
            [salava.core.mail :refer [send-mail]]
            [salava.core.i18n :refer [t]]
            [salava.core.util :refer [get-db get-datasource get-site-url get-base-path get-site-name]]
            [salava.social.db :refer [email-new-messages-block]]
            [salava.user.db :refer [get-user-and-primary-email get-user-ids-from-event-owners]] ))


(defn email-reminder-body [ctx user]
  (try
    (Thread/sleep 50)
    (catch InterruptedException _));lisää sleeppiä
  
  (let [site-url (get-site-url ctx)
        base-path (get-base-path ctx)
        url (str site-url base-path "/social")
        lng (:language user)
        events (email-new-messages-block ctx user lng)
        site-name (get-in ctx [:config :core :site-name] "Open Badge Passport")
        subject (str site-name ": " (t :user/Emailnotificationsubject lng))
        message (str (t :user/Emailnotificationtext1 lng) " " (:first_name user) " " (:last_name user) ",\n\n"(t :user/Emailnotificationtext2 lng)  ": " "\n" events "\n"(t :user/Emailnotificationtext3 lng) " " url "\n\n"(t :user/Emailnotificationtext4 lng) ",\n-- " site-name " - "(t :core/Team lng))
        ]
    (try+
     (if (and (not (empty? events)) (first events) user)
       (do
         (println "-----------------------")
         (println "\n")
         
         (println "email:" (:email user))
         (println subject)
         (println message)
         ;(send-mail ctx subject message [(:email user)])
         ))
     "success"
   (catch Object _
     "error"))))


(defn email-sender [ctx]
  (let [event-owners (get-user-ids-from-event-owners ctx)]
    (doseq [user event-owners]
      (email-reminder-body ctx user))))
