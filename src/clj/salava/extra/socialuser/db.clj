(ns salava.extra.socialuser.db
  (:require [yesql.core :refer [defqueries]]
            [clojure.set :refer [rename-keys]]
            [clojure.java.jdbc :as jdbc]
            [salava.core.helper :refer [dump]]
            [slingshot.slingshot :refer :all]
            [salava.core.util :as u :refer [get-db]]
            [salava.admin.helper :as ah]
            [clojure.string :refer [blank? join]]
            [clojure.tools.logging :as log]
            [salava.core.time :refer [unix-time get-date-from-today]]))

(defqueries "sql/extra/socialuser/queries.sql")

(defn events-reduce [events]
  (let [helper (fn [current item]
                 (let [key [(:subject item) (:verb item) (:object item)]]
                    (-> current
                        (assoc  key item)
                        ;(assoc-in  [key :count] (inc (get-in current [key :count ] 0)))
                        )))
        reduced-events (vals (reduce helper {} (reverse events)))]
    (filter #(false? (:hidden %)) reduced-events)))

(defn get-user-events [ctx user_id]
  (let [user-badge-events (select-user-badge-events {:owner_id user_id} (get-db ctx))
        user-events (select-user-events {:owner_id user_id} (get-db ctx))
        events (distinct (concat user-badge-events user-events))]
    (events-reduce events)))

(defn get-owners
  "Set object to owner if event type is user"
  [ctx object user-event?]
  (let [owners (select-users-from-connections-user {:user_id object} (get-db ctx))]
    (if user-event?
      (conj owners {:owner object})
      owners)))

(defn get-user-accepted-connections-user
  "returns accepted connections: profile_picture, first_name, last_name and ordered by first_name "
  [ctx owner_id]
  (select-user-connections-user {:owner_id owner_id} (get-db ctx)))



(defn get-pending-requests
  "returns all users who try to connect with user "
  [ctx user_id]
  (select-user-connections-user-pending {:user_id user_id} (get-db ctx)))



(defn get-connections-user [ctx owner_id user_id]
  (let [user (select-connections-user {:owner_id owner_id :user_id user_id} (into {:result-set-fn first} (get-db ctx)) )]
    (or user
        {:owner_id owner_id, :user_id user_id, :status nil, :ctime nil}))
  )



(defn delete-connections-user [ctx owner_id user_id]
  (try+
   (delete-connections-user! {:owner_id owner_id :user_id user_id} (get-db ctx))
   {:status "success"}
   (catch Object _
     {:status "error" :message (:message &throw-context)}
     ))
  )

(defn- update-connections-user-status [ctx owner_id user_id status]
  (try+
   (update-connections-user-pending! {:user_id user_id :owner_id owner_id :status status} (get-db ctx))
   
   {:status "success"}
   (catch Object _
     {:status "error" :message (:message &throw-context)}
     ))
  )

(defn set-pending-request
  "if decline delete connection" 
  [ctx owner_id user_id status]
  (if (= "declined" status)
    (delete-connections-user ctx owner_id user_id)
    (do
      (u/event ctx owner_id "follow" user_id "user")
      (update-connections-user-status ctx owner_id user_id status))))

(defn set-user-connections-accepting
  "User can accept connectionss instantly or with pending requests or decline every"
  [ctx user_id status]
  (try+
   (update-user-connections-accepting! {:user_id user_id :value status} (get-db ctx))
   {:status "success"}
   (catch Object _
     {:status "error" :message (:message &throw-context) }
     )))

(defn get-user-connections-accepting [ctx user_id]
  (or (select-user-connections-accepting {:user_id user_id} (into {:result-set-fn first :row-fn :status} (get-db ctx))) "pending"))

(defn insert-connections-user! [ctx owner_id user_id status]
  (insert-connections-user<! {:owner_id owner_id :user_id user_id :status status} (get-db ctx)))

(defn create-connections-user! [ctx owner_id user_id]
  (try+
   (let [status (get-user-connections-accepting ctx user_id)]
     (insert-connections-user! ctx owner_id user_id status)
     (if (= status "accepted")
       (u/event ctx owner_id "follow" user_id "user")))
   {:status "success"}
   (catch Object _
     {:status "error" :message (:message &throw-context) }
     )))




