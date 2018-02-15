(ns salava.badge.db
  (:require [clojure.tools.logging :as log]
            [yesql.core :refer [defqueries]]
            [clojure.java.jdbc :as jdbc]
            [clojure.string :as string]
            [clojure.pprint :refer [pprint]]
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


(defn badge-message-map
  "returns newest message and count new messages"
  [messages]
  (let [message-helper (fn [current item]
                         (let [key  (:badge_id item)
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
        badge-ids (map #(:object %) reduced-events)
        messages (if (not (empty? badge-ids)) (select-messages-with-badge-id {:badge_ids badge-ids :user_id user_id} (u/get-db ctx)) ())
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
    (select-users-from-connections-badge {:badge_id object} (u/get-db ctx))))

(defn- content-id [data]
  (u/map-sha256 (assoc data :id "")))

(defn- save-image [ctx item]
  (if (string/blank? (:image_file item))
    item
    (assoc item :image_file (u/file-from-url ctx (:image_file item)))))


(defn- save-endorsement-images [ctx endorsements]
  (mapv (fn [e]
          (let [issuer (save-image ctx (:issuer e))]
            (assoc e :issuer (update issuer :endorsement #(save-endorsement-images ctx %)))))
        endorsements))

;;;TODO endorsement images
(defn save-images [ctx badge]
  (-> badge
      (update :content  (fn [content]  (mapv #(save-image ctx %) content)))
      (update :criteria (fn [criteria] (mapv #(save-image ctx %) criteria)))
      (update :issuer   (fn [issuer]   (mapv #(save-image ctx %) issuer)))
      (update :creator  (fn [creator]  (mapv #(save-image ctx %) creator)))
      (update :endorsement (fn [e] (save-endorsement-images ctx e)))))


(defn save-criteria-content! [t-con input]
  (log/debug "save-criteria-content!")
  (s/validate schemas/CriteriaContent input)
  (let [id (content-id input)]
    (insert-criteria-content! (assoc input :id id) {:connection t-con})
    id))


#_(defn save-endorser-endorsements [t-con input]
    (let [ endorser-id (content-id (-> input
                                     (dissoc :endorsement)))
           endorsements (:endorsement input)]

    (insert-endorser-content! (assoc input :id endorser-id) {:connection t-con})

    (when-not (empty? endorsements)
      (doseq [endorsement endorsements
              :let [
                     parsed-endorsement (json/read-str endorsement :key-fn keyword)
                     endorser (content-id (-> (:endorser_info parsed-endorsement)))
                     endorsement-id (content-id (-> parsed-endorsement
                                                   (dissoc :endorser_info)
                                                   #_(assoc :endorser endorser-id)))]]


        (-> parsed-endorsement
            (update :endorser_info (fn [endorser-info] (map #(save-image t-con %) endorser-info))))


        (insert-endorser-content! (assoc (:endorser_info parsed-endorsement) :id endorser) {:connection t-con})

        (insert-endorsement-content! (assoc parsed-endorsement :id endorsement-id
                                                  :endorser endorser) {:connection t-con})

         (jdbc/execute! t-con ["INSERT IGNORE INTO client_endorsement_content (client_content_id, endorsement_content_id) VALUES (?,?)"
                         (str endorser-id endorser) endorsement-id])))
  endorser-id))

(declare save-endorsement-content!)

(defn save-issuer-content! [t-con input]
  (log/debug "save-issuer-content!")
  (s/validate schemas/IssuerContent input)
  (let [id (content-id (-> input (dissoc :endorsement)))]
    (insert-issuer-content! (assoc input :id id) {:connection t-con})

    (jdbc/execute! t-con ["DELETE FROM issuer_endorsement_content WHERE issuer_content_id = ?" id])
    (doseq [endorsement (:endorsement input)
            :let [endorsement-id (save-endorsement-content! t-con endorsement)]]
      (jdbc/execute! t-con ["INSERT IGNORE INTO issuer_endorsement_content (issuer_content_id, endorsement_content_id) VALUES (?,?)"
                            id endorsement-id]))
    id))

(defn save-endorsement-content! [t-con input]
  (when input
    (log/debug "save-endorsement-content!")
    (s/validate schemas/Endorsement input)
    (let [issuer-id (save-issuer-content! t-con (:issuer input))
          id (content-id (-> input
                             (assoc :issuer_content_id issuer-id)
                             (dissoc :issuer)))]
      (insert-endorsement-content! (-> input
                                       (assoc :id id)
                                       (assoc :issuer_content_id issuer-id))
                                   {:connection t-con})
      id)))

(defn save-creator-content! [t-con input]
  (when input
    (log/debug "save-creator-content!")
    (s/validate schemas/CreatorContent input)
    (let [id (content-id (-> input (dissoc :endorsement)))]
      (insert-creator-content! (assoc input :id id) {:connection t-con})

      (jdbc/execute! t-con ["DELETE FROM issuer_endorsement_content WHERE issuer_content_id = ?" id])
      (doseq [endorsement (:endorsement input)
              :let [endorsement-id (save-endorsement-content! t-con endorsement)]]
        (jdbc/execute! t-con ["INSERT IGNORE INTO issuer_endorsement_content (issuer_content_id, endorsement_content_id) VALUES (?,?)"
                              id endorsement-id]))
      id)))

(defn save-badge-content! [t-con input]
  (log/debug "save-badge-content!")
  (s/validate schemas/BadgeContent input)
  (let [id (content-id input)]
    (insert-badge-content! (assoc input :id id) {:connection t-con})
    (doseq [tag (:tags input)]
      (insert-badge-content-tag! {:badge_content_id id :tag tag} {:connection t-con}))
    (doseq [a (:alignment input)]
      (insert-badge-content-alignment! (assoc a :badge_content_id id) {:connection t-con}))
    id))

(defn save-badge! [tx badge]
  (let [badge-content-id (sort (map #(save-badge-content! tx %) (:content badge)))
        criteria-content-id (sort (map #(save-criteria-content! tx %) (:criteria badge)))
        issuer-content-id (sort (map #(save-issuer-content! tx %) (:issuer badge)))
        creator-content-id (sort (map #(save-creator-content! tx %) (:creator badge)))
        endorsement-content-id  (sort (map #(save-endorsement-content! tx %) (:endorsement badge)))

        ;;NB: endorsements are not included in badge id hash
        badge-id (u/hex-digest "sha256" (apply str (concat badge-content-id
                                                           criteria-content-id
                                                           issuer-content-id
                                                           creator-content-id)))]

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

    (jdbc/execute! tx ["DELETE FROM badge_endorsement_content WHERE badge_id = ?" badge-id])
    (doseq [endorsement-id endorsement-content-id]
      (jdbc/execute! tx ["INSERT IGNORE INTO badge_endorsement_content (badge_id, endorsement_content_id) VALUES (?,?)"
                         badge-id endorsement-id]))

    (-> badge
        (dissoc :content :criteria :issuer :creator :endorsement )
        (assoc :id badge-id)
        (insert-badge! {:connection tx}))
    badge-id))

(defn save-user-badge! [ctx user-badge]
  (jdbc/with-db-transaction  [tx (:connection (u/get-db ctx))]
    (let [now (u/now)
          badge-id (->> (:badge user-badge) (save-images ctx) (save-badge! tx))
          user-badge-id (-> user-badge
                            (dissoc :badge)
                            (assoc :badge_id badge-id)
                            (insert-user-badge<! {:connection tx})
                            :generated_key)]

      (when (seq (:evidence user-badge))
        (doseq [evidence (:evidence user-badge)]
          (when (map? evidence)
            (jdbc/insert! tx "user_badge_evidence" (-> evidence
                                                       (dissoc :id)
                                                       (assoc :user_badge_id user-badge-id
                                                              :ctime now
                                                              :mtime now)))))))))
