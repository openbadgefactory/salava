(ns salava.user.email-notifications
  (:require [yesql.core :refer [defqueries]]
            [clojure.set :refer [rename-keys]]
            [clojure.java.jdbc :as jdbc]
            [salava.core.helper :refer [dump]]
            [slingshot.slingshot :refer :all]
            [salava.social.db :refer [email-new-messages-block]]
            [salava.user.db :refer [get-user-and-primary-email get-user-ids-from-event-owners]] ))

(def ctx {:db (hikari-cp.core/make-datasource {:adapter "mysql",
                                               :username "root",
                                               :password "isokala",
                                               :database-name "salava2",
                                               :server-name "localhost"})})

(defn email-reminder-body [ctx user-id]
                                        ;lisää sleeppiä
  ;(Thread/sleep 50)
  (let [events (email-new-messages-block ctx user-id)
        user   (if (not-empty events) (get-user-and-primary-email ctx user-id))]
    (try+
     (if (and (not (empty? events)) (first events) user)
       (do
         (println "\n")
         (println "-----------------------")
         (println "email:" (:email user))
         (println "hello " (:first_name user) (:last_name user))
         (println events)
         ))
   "success"
   (catch Object _
     "error"))))


(defn email-sender [ctx]
  (let [event-owners (get-user-ids-from-event-owners ctx)]
    (map (fn [id] (email-reminder-body ctx id)) event-owners)))

(time (email-sender ctx))
