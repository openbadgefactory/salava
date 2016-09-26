(ns salava.social.db
 (:require [yesql.core :refer [defqueries]]
            [clojure.set :refer [rename-keys]]
            [clojure.java.jdbc :as jdbc]
            [salava.core.helper :refer [dump]]
            [slingshot.slingshot :refer :all]
            [salava.core.util :refer [get-db]]
            [salava.admin.helper :as ah]
            [salava.core.time :refer [unix-time get-date-from-today]]))

(defqueries "sql/social/queries.sql")

(defn message! [ctx badge_content_id user_id message]
  (try+
   (insert-badge-message<! {:badge_content_id badge_content_id :user_id user_id :message message} (get-db ctx))
   "success"
   (catch Object _
     "error"
     )))

(defn get-badge-messages [ctx badge_content_id]
  (let [badge-messages (select-badge-messages {:badge_content_id badge_content_id} (get-db ctx))]
    badge-messages))

(defn get-badge-message-count [ctx badge_content_id]
  (let [badge-messages-count (select-badge-messages-count {:badge_content_id badge_content_id} (into {:result-set-fn first :row-fn :count} (get-db ctx)) )]
    badge-messages-count))

(defn get-badge-messages-limit [ctx badge_content_id page_count]
  (let [limit 10
        offset (* limit page_count)
        badge-messages (select-badge-messages-limit {:badge_content_id badge_content_id :limit limit :offset offset} (get-db ctx))
        messages-left (- (get-badge-message-count ctx badge_content_id) (* limit (+ page_count 1)))]
    {:messages badge-messages
     :messages_left (if (pos? messages-left) messages-left 0)}))


(defn message-owner? [ctx message_id user_id]
(let [message_owner (select-badge-message-owner {:message_id message_id} (into {:result-set-fn first :row-fn :user_id} (get-db ctx)))]
 (= user_id message_owner)))

(defn delete-message! [ctx message_id user_id]
  (try+
   (let [message-owner (message-owner? ctx message_id user_id)
         admin (ah/user-admin? ctx user_id) ]
     (if (or message-owner admin)
       (update-badge-message-deleted! {:message_id message_id} (get-db ctx))))
   "success"
   (catch Object _
     "error"
     )))
