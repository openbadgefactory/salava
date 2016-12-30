(ns salava.social.db
  (:require [yesql.core :refer [defqueries]]
            [clojure.set :refer [rename-keys]]
            [clojure.java.jdbc :as jdbc]
            [salava.core.helper :refer [dump]]
            [slingshot.slingshot :refer :all]
            [salava.core.util :refer [get-db]]
            [salava.admin.helper :as ah]
            [salava.core.i18n :refer [t]]
            [clojure.string :refer [blank? join]]
            [salava.core.time :refer [unix-time get-date-from-today]]))

(defqueries "sql/social/queries.sql")

(defn messages-viewed
 "Save information about viewing messages."
 [ctx badge_content_id user_id]
  (try+
   (replace-badge-message-view! {:badge_content_id badge_content_id :user_id user_id} (get-db ctx))
   (catch Object _
     "error")))

(defn insert-event!
  "Creates event and adds event for every users who are connected with event"
  [ctx, subject, verb, object, type]
  (try+
   (let [event-id (insert-social-event<! {:subject subject :verb verb :object object :type type} (get-db ctx))
         connected-users  (cond
                            (= type "badge") (select-users-from-connections-badge {:badge_content_id object} (get-db ctx))
                            (= type "admin") (select-admin-users-id {} (get-db ctx)))
         query (vec (map #(assoc % :event_id (:generated_key event-id)) connected-users))]
     (jdbc/insert-multi! (:connection (get-db ctx)) :social_event_owners query)
     {:status "success"}) 
   (catch Object _
     {:status "error"})))


(defn is-connected? [ctx user_id badge_content_id]
  (let [id (select-connection-badge {:user_id user_id :badge_content_id badge_content_id} (into {:result-set-fn first :row-fn :badge_content_id} (get-db ctx)))]
    (= badge_content_id id)))

(defn insert-connection-badge! [ctx user_id badge_content_id]
  (insert-connect-badge<! {:user_id user_id :badge_content_id badge_content_id} (get-db ctx))
  (insert-event! ctx user_id "follow" badge_content_id "badge")
  (messages-viewed ctx badge_content_id user_id))

(defn create-connection-badge! [ctx user_id  badge_content_id]
  (try+
   (insert-connection-badge! ctx user_id badge_content_id)
   {:status "success" :connected? (is-connected? ctx user_id badge_content_id)}
   (catch Object _
     {:status "error" :connected? (is-connected? ctx user_id badge_content_id)}
     )))
;; STREAM ;;


(defn badge-events-reduce [events]
  (let [helper (fn [current item]
                  (let [key [(:verb item) (:object item)]]
                    (-> current
                        (assoc  key item)
                        ;(assoc-in  [key :count] (inc (get-in current [key :count ] 0)))
                        )))
        reduced-events (vals (reduce helper {} (reverse events)))]
    (filter #(false? (:hidden %)) reduced-events)))





(defn admin-events-reduce [events]
  (let [helper (fn [current item]
                  (let [key [(:verb item)]]
                    (-> current
                        (assoc  key item)
                        (assoc-in  [key :count] (inc (get-in current [key :count ] 0)))
                        )))
        reduced-events (vals (reduce helper {} (reverse events)))]
    (filter #(false? (:hidden %)) reduced-events)))





(defn badge-message-map
  "returns newest message and count new messages"
  [messages]
  (let [message-helper (fn [current item]
                         (let [key  (:badge_content_id item)
                               new-messages-count (get-in current [key :new_messages] 0)]
                           (-> current
                               (assoc key item)
                               (assoc-in [key :new_messages] (if (> (:ctime item) (:last_viewed item))
                                                              (inc new-messages-count)
                                                              new-messages-count)))))]
    (reduce message-helper {} (reverse messages))))


(defn filter-badge-message-events [events]
  (filter #(= "message" (:verb %)) events))

(defn filter-own-events [events user_id]
  (filter #(and (= user_id (:subject %)) (= "follow" (:verb %))) events) )


(defn update-last-viewed [ctx events user_id]
  (let [event_ids (map  #(:event_id %) events)]
    (update-last-checked-user-event-owner! {:user_id user_id :event_ids event_ids} (get-db ctx))
    ))
(defn get-user-admin-events [ctx user_id]
  (select-admin-events {:user_id user_id} (get-db ctx)))

(defn get-user-admin-events-sorted [ctx user_id]
  (let [events (get-user-admin-events ctx user_id)]
   (if (not-empty events)
     (update-last-viewed ctx events user_id))
   events))




(defn get-user-badge-events
  "get users badge  message and follow events"
  [ctx user_id]
  (let [events (select-user-events {:user_id user_id} (get-db ctx)) ;get all events where type = badge
        reduced-events (badge-events-reduce events) ;bundle events together with object and verb
        badge-content-ids (map #(:object %) reduced-events)
        messages (if (not (empty? badge-content-ids)) (select-messages-with-badge-content-id {:badge_content_ids badge-content-ids :user_id user_id} (get-db ctx)) ())
        messages-map (badge-message-map messages)
        message-events (map (fn [event] (assoc event :message (get messages-map (:object event)))) (filter-badge-message-events reduced-events)) ;add messages for nessage event
        follow-events (filter-own-events reduced-events user_id)
        badge-events (into follow-events message-events)]
    badge-events))

(defn get-user-badge-events-sorted-and-filtered [ctx user_id]
  (let [badge-events (get-user-badge-events ctx user_id)
        sorted (take 25 (sort-by :ctime #(> %1 %2) (vec badge-events)))]
    (if (not-empty sorted)
      (update-last-viewed ctx sorted user_id))
    sorted))

(defn hide-user-event! [ctx user_id event_id]
  (try+
   (update-hide-user-event! {:user_id user_id :event_id event_id} (get-db ctx))
   "success"
   (catch Object _
     "error")))

;; --- Email sender --- ;;


(defn admin-events-message [ctx user lng]
  (let [user-id (:id user)
        admin-events (get-user-admin-events ctx user-id)
        admin-events (filter #(nil? (:last_checked %)) admin-events)]
    (if (not (empty? admin-events))
      (str (t :social/Emailadmintickets lng) " " (count admin-events) "\n" ))
    ))

(defn email-new-messages-block [ctx user lng]
  (let [user-id (:id user)
        admin? (= "admin" (:role user))
        admin-events (if admin? (admin-events-message ctx user lng) nil)
        events (or (get-user-badge-events ctx user-id) nil)
        events (filter #(nil? (:last_checked %)) events)
        message-helper (fn [item]
                         (when (and (get-in item [:message :new_messages] )
                                    (< 0 (get-in item [:message :new_messages] ))
                                    (= "message" (:verb item)))
                           (let [new-messages (get-in item [:message :new_messages] ) ]
                             (str (:name item) "-"  (t :social/Emailnewmessage1 lng) " " new-messages " " (if (= 1 new-messages ) (t :social/Emailnewcomment lng)(t :social/Emailnewcomments lng)) "\n"))))]
    (str admin-events (join (map message-helper events)))))


;; MESSAGES ;;

(defn message! [ctx badge_content_id user_id message]
  (try+
   (if (not (blank? message))
     (let [badge-connection (if (not (is-connected? ctx user_id badge_content_id))
                              (:status (create-connection-badge! ctx user_id badge_content_id))
                              "connected")]
       (do
         (insert-badge-message<! {:badge_content_id badge_content_id :user_id user_id :message message} (get-db ctx))
         (insert-event! ctx user_id "message" badge_content_id "badge")
         {:status "success" :connected? badge-connection}))
     {:status "error" :connected? nil})
   (catch Object _
     {:status "error" :connected? nil})))

(defn get-badge-messages [ctx badge_content_id user_id]
  (let [badge-messages (select-badge-messages {:badge_content_id badge_content_id} (get-db ctx))]
    (do
      (messages-viewed ctx badge_content_id user_id)
      badge-messages
      )))

(defn get-badge-message-count [ctx badge_content_id user-id]
  (let [badge-messages-user-id-ctime (select-badge-messages-count {:badge_content_id badge_content_id} (get-db ctx))
        last-viewed (select-badge-message-last-view {:badge_content_id badge_content_id :user_id user-id} (into {:result-set-fn first :row-fn :mtime} (get-db ctx)))
        new-messages (if last-viewed (filter #(and (not= user-id (:user_id %)) (< last-viewed (:ctime %))) badge-messages-user-id-ctime) ())]
    {:new-messages (count new-messages)
     :all-messages (count badge-messages-user-id-ctime)}))


(defn get-badge-messages-limit [ctx badge_content_id page_count user_id]
  (let [limit 10
        offset (* limit page_count)
        badge-messages (select-badge-messages-limit {:badge_content_id badge_content_id :limit limit :offset offset} (get-db ctx))
        messages-left (- (:all-messages (get-badge-message-count ctx badge_content_id user_id)) (* limit (+ page_count 1)))]
    (if (= 0  page_count)
      (messages-viewed ctx badge_content_id user_id))
    {:messages badge-messages
     :messages_left (if (pos? messages-left) messages-left 0)}))


(defn message-owner? [ctx message_id user_id]
(let [message_owner (select-badge-message-owner {:message_id message_id} (into {:result-set-fn first :row-fn :user_id} (get-db ctx)))]
 (= user_id message_owner)))

(defn delete-message! [ctx message_id user_id]
  (try+
   (let [message-owner (message-owner? ctx message_id user_id)
         admin (ah/user-admin? ctx user_id)
         ;badge-content-id (select-badge-content-id-by-message-id {:message_id message_id} (into {:result-set-fn first :row-fn :badge_content_id} (get-db ctx)))
         ]
     (if (or message-owner admin)
       (do
         (update-badge-message-deleted! {:message_id message_id} (get-db ctx))
         )))
   "success"
   (catch Object _
     "error"
     )))





(defn create-connection-badge-by-badge-id! [ctx user_id badge_id]
  (let [badge_content_id (select-badge-content-id-by-badge-id {:badge_id badge_id} (into {:result-set-fn first :row-fn :badge_content_id} (get-db ctx)))]
    (try+
     (insert-connection-badge! ctx user_id badge_content_id)
     (catch Object _
       ))))


(defn delete-connection-badge-by-badge-id! [ctx user_id badge_id]
 (try+
   (delete-connect-badge-by-badge-id! {:user_id user_id :badge_id badge_id} (get-db ctx))
   (catch Object _
     )))

(defn delete-connection-badge! [ctx user_id  badge_content_id]
  (try+
   (delete-connect-badge! {:user_id user_id :badge_content_id badge_content_id} (get-db ctx))
   
   {:status "success" :connected? (is-connected? ctx user_id badge_content_id)}
   (catch Object _
     
     {:status "error" :connected? (is-connected? ctx user_id badge_content_id)}
     )))


(defn get-connections-badge [ctx user_id]
  (select-user-connections-badge {:user_id user_id} (get-db ctx))
  )

(defn get-users-not-verified-emails [ctx user_id]
  (select-user-not-verified-emails {:user_id user_id} (get-db ctx)))

(defn get-user-tips [ctx user_id]
  (let [welcome-tip (= 0 (select-user-badge-count {:user_id user_id} (into {:result-set-fn first :row-fn :count} (get-db ctx))))
        profile-picture-tip (if (not welcome-tip) (nil? (select-user-profile-picture {:user_id user_id} (into {:result-set-fn first :row-fn :profile_picture} (get-db ctx)))) false)]
    {:profile-picture-tip profile-picture-tip
     :welcome-tip welcome-tip
     :not-verified-emails (get-users-not-verified-emails ctx user_id)}
    ))

