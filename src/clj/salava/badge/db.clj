(ns salava.badge.db
  (:require [clojure.tools.logging :as log]
            [yesql.core :refer [defqueries]]
            [clojure.java.jdbc :as jdbc]
            [clojure.string :as string]
            [clojure.data.json :as json]
            [schema.core :as s]
            [salava.core.helper :refer [dump]]
            [salava.badge.schemas :as schemas] ;cljc
            [salava.core.util :as u]))

(defqueries "sql/badge/main.sql")

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

(defn get-user-badge-events
  "get users badge  message and follow events"
  [ctx user_id]
  (let [events (select-user-events {:user_id user_id} (u/get-db ctx)) ;get all events where type = badge
        reduced-events (badge-events-reduce events) ;bundle events together with object and verb
        badge-content-ids (map #(:object %) reduced-events)
        messages (if (not (empty? badge-content-ids)) (select-messages-with-badge-content-id {:badge_content_ids badge-content-ids :user_id user_id} (u/get-db ctx)) ())
        messages-map (badge-message-map messages)
        message-events (map (fn [event] (assoc event :message (get messages-map (:object event)))) (filter-badge-message-events reduced-events)) ;add messages for nessage event
        follow-events (filter-own-events reduced-events user_id)
        badge-events (into follow-events message-events)]
    badge-events))

(defn get-badge-events [ctx user_id]
  (let [badge-events (get-user-badge-events ctx user_id)
        sorted (take 25 (sort-by :ctime #(> %1 %2) (vec badge-events)))]
    sorted))
;Owners

(defn str->number? [str]
  (try
    (if (number? str)
        true
        (let [n (read-string str)]
          (number? n)))
    (catch Exception e
      false)))

(defn get-owners [ctx object]
  (if (str->number? object) ;if object is badge-id set owner be badges owner
    (select-badge-owner-as-owner {:id object} (u/get-db ctx))
    (select-users-from-connections-badge {:badge_content_id object} (u/get-db ctx))))

(defn- content-id [data]
  (u/map-sha256 (assoc data :id "")))

(defn- save-image [ctx item]
  (if (string/blank? (:image_file item))
    item
    (assoc item :image_file (u/file-from-url ctx (:image_file item)))))

(defn save-images [ctx badge]
  (-> badge
      (update :content  (fn [content]  (mapv #(save-image ctx %) content)))
      (update :criteria (fn [criteria] (mapv #(save-image ctx %) criteria)))
      (update :issuer   (fn [issuer]   (mapv #(save-image ctx %) issuer)))
      (update :creator  (fn [creator]  (mapv #(save-image ctx %) creator)))))



(defn save-criteria-content! [ctx input]
  (s/validate schemas/CriteriaContent input)
  (let [id (content-id input)]
    (insert-criteria-content! (assoc input :id id) (u/get-db ctx))
    id))

(defn save-issuer-content! [ctx input]
  (s/validate schemas/IssuerContent input)
  (let [id (content-id input)]
    (insert-issuer-content! (assoc input :id id) (u/get-db ctx))
    id))

(defn save-creator-content! [ctx input]
  (when input
    (s/validate schemas/CreatorContent input)
    (let [id (content-id input)]
      (insert-creator-content! (assoc input :id id) (u/get-db ctx))
      id)))

(defn save-badge-content! [ctx input]
  (s/validate schemas/BadgeContent input)
  (let [id (content-id input)]
    (jdbc/with-db-transaction  [t-con (:connection (u/get-db ctx))]
      (insert-badge-content! (assoc input :id id) {:connection t-con})
      (doseq [tag (:tags input)]
        (insert-badge-content-tag! {:badge_content_id id :tag tag} {:connection t-con}))
      (doseq [a (:alignment input)]
        (insert-badge-content-alignment! (assoc a :badge_content_id id) {:connection t-con})))
    id))

;;

#_(defn save-badge! [ctx badge]
  (try
    (let [badge_content_id    (->> (:badge_content badge) (save-image ctx) (save-badge-content! ctx))
          issuer_content_id   (->> (:issuer_content badge) (save-image ctx) (save-issuer-content! ctx))
          criteria_content_id (save-criteria-content! ctx (:criteria_content badge))
          creator_content_id  (->> (:creator_content badge) (save-image ctx) (save-creator-content! ctx))]
      (-> badge
          (dissoc :badge_content :issuer_content :criteria_content :creator_content)
          (assoc    :badge_content_id badge_content_id
                   :issuer_content_id issuer_content_id
                 :criteria_content_id criteria_content_id
                  :creator_content_id creator_content_id)
          (insert-badge<! (u/get-db ctx))))
    (catch Exception ex
      (log/error "save-badge!: failed to save badge data")
      (log/error (.toString ex)))))

(defn save-badge! [ctx badge]
  (let [badge-content-id (sort (map #(save-badge-content! ctx %) (:content badge)))
        criteria-content-id (sort (map #(save-criteria-content! ctx %) (:criteria badge)))
        issuer-content-id (sort (map #(save-issuer-content! ctx %) (:issuer badge)))
        creator-content-id (sort (map #(save-creator-content! ctx %) (:creator badge)))
        badge-id (u/hex-digest "sha256" (apply str (concat badge-content-id
                                                           criteria-content-id
                                                           issuer-content-id
                                                           creator-content-id)))]
    (jdbc/with-db-transaction  [tx (:connection (u/get-db ctx))]
      (doseq [content-id badge-content-id]
        (jdbc/execute! tx ["INSERT IGNORE INTO badge_badge_content (badge_id, badge_content_id) VALUES (?,?)"
                           badge-id content-id]))
      (doseq [criteria-id criteria-content-id]
        (jdbc/execute! tx ["INSERT IGNORE INTO badge_criteria_content (badge_id, criteria_content_id) VALUES (?,?)"
                           badge-id criteria-id]))
      (doseq [issuer-id issuer-content-id]
        (jdbc/execute! tx ["INSERT IGNORE INTO badge_issuer_content (badge_id, issuer_content_id) VALUES (?,?)"
                           badge-id issuer-id]))
      (doseq [creator-id creator-content-id]
        (jdbc/execute! tx ["INSERT IGNORE INTO badge_creator_content (badge_id, creator_content_id) VALUES (?,?)"
                           badge-id creator-id]))
      (-> badge
          (dissoc :content :criteria :issuer :creator)
          (assoc :id badge-id)
          (insert-badge! tx)))
    badge-id))


(defn save-user-badge! [ctx user-badge]
  (try
    (let [badge-id (->> (:badge user-badge) (save-images ctx) (save-badge! ctx))]
      (-> user-badge
          (dissoc :badge)
          (assoc :badge_id badge-id)
          (insert-user-badge<! (u/get-db ctx))))
    (catch Exception ex
      (log/error "save-user-badge!: failed to save badge data")
      (log/error (.toString ex)))))
