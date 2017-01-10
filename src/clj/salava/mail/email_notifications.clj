(ns salava.mail.email-notifications
  (:require [yesql.core :refer [defqueries]]
            [clojure.tools.logging :as log]
            [clojure.set :refer [rename-keys]]
            [clojure.java.io :as io]
            [clojure.java.jdbc :as jdbc]
            [salava.core.helper :refer [dump]]
            [slingshot.slingshot :refer :all]
            [salava.mail.mail :refer [send-mail]]
            [salava.core.i18n :refer [t]]
            [salava.core.util :refer [get-db get-datasource get-site-url get-base-path get-site-name get-email-notifications]]
            [salava.social.db :refer [email-new-messages-block]]
            [salava.core.time :refer [get-day-of-week]]
            [salava.user.db :refer [get-user-and-primary-email get-user-ids-from-event-owners]] ))



(defn email-message [ctx full-name events lng site-name]
  (let [site-url (get-site-url ctx)
        base-path (get-base-path ctx)
        social-url (str site-url base-path "/social")
        user-url (str site-url base-path "/user/edit")]
    (str  (t :user/Emailnotificationtext1 lng) " " full-name ",\n\n"
          (t :user/Emailnotificationtext2 lng)  ": " "\n\n" events "\n"
          (t :user/Emailnotificationtext3 lng) " " social-url "\n\n"
          (t :user/Emailnotificationunsubscribetext  lng) ":\n" user-url "\n\n"
          (t :user/Emailnotificationtext4 lng) ",\n\n-- " site-name " - "(t :core/Team lng))
    ))




(defn email-reminder-body [ctx user]
  (try
    (Thread/sleep 50)
    (catch InterruptedException _));lisää sleeppiä
  (try+
   (let [lng (:language user)
         events (email-new-messages-block ctx user lng)
         site-name (get-in ctx [:config :core :site-name] "Open Badge Passport")
         full-name (str (:first_name user) " " (:last_name user))
         subject (str site-name ": " (t :user/Emailnotificationsubject lng))
         message (email-message ctx full-name events "en" site-name)
         ]
     (if (and (not (empty? events)) (first events) user)
       (do
         (println "-----------------------")
         (println "\n")
         (println "email:" (:email user))
         (println subject)
         (println message)
         ;(send-mail ctx subject message [(:email user)])
         )))
     (catch Object ex
       (log/error "failed to send email notification to user:")
       (log/error (.toString ex)))))


(defn email-sender [ctx]
  (if (get-email-notifications ctx)    
    (let [event-owners      (get-user-ids-from-event-owners ctx)
          day               (dec (get-day-of-week))
          current-day-users event-owners
                                        ;(filter #(= day (rem (:id %) 7)) event-owners)
          ]
      (doseq [user current-day-users]
        (email-reminder-body ctx user)))))



