(ns salava.badgeIssuer.db
 (:require
  [clojure.data.json :as json]
  [clojure.string :refer [blank?]]
  [clojure.tools.logging :as log]
  [salava.core.util :refer [get-db get-db-1 file-from-url-fix get-full-path md->html]]
  [slingshot.slingshot :refer :all]
  [yesql.core :refer [defqueries]]))

(defqueries "sql/badgeIssuer/main.sql")

(defn user-selfie-badges [ctx user-id]
  (let [selfies (get-user-selfie-badges {:creator_id user-id} (get-db ctx))]
   (->> selfies (reduce (fn [r s]
                         (conj r (-> s (assoc :tags (if (blank? (:tags s)) nil (json/read-str (:tags s)))
                                              :criteria_html (md->html (:criteria s))))))
                  []))))

(defn selfie-badge [ctx id]
 (let [selfie (get-selfie-badge {:id id} (get-db-1 ctx))
       tags (if-not (blank? (:tags selfie)) (json/read-str (:tags selfie)) [])]
  (some-> selfie (assoc :tags tags))))

(defn user-selfie-badge [ctx user-id id]
  (selfie-badge ctx id))

(defn delete-selfie-badge [ctx user-id id]
  (try+
    (hard-delete-selfie-badge! {:id id :creator_id user-id} (get-db ctx))
    {:status "success"}
    (catch Object _
      {:status "error"})))

(defn finalise-user-badge! [ctx data]
  (finalise-issued-user-badge! data (get-db ctx)))

(defn delete-user-selfie-badges! [ctx user-id]
  (try+
    (delete-selfie-badges-all! {:user_id user-id} (get-db ctx))
    (catch Object _
      (log/error _))))

(defn map-badges-issuable [ctx gallery_ids badges]
  (let [_ (select-issuable-gallery-badges {:gallery_ids gallery_ids} (get-db ctx))]
    (->> badges
         (map #(assoc % :selfie_id (some (fn [b] (when (= (:gallery_id %) (:gallery_id b))
                                                   (:selfie_id b))) _))))))
   
(defn insert-create-event! [ctx data]
  (insert-selfie-event<! data (get-db ctx)))

(defn insert-issue-event! [ctx data]
  (insert-selfie-event<! data (get-db ctx)))

(defn insert-issue-event-owner! [ctx data]
  (let [owner-id (select-selfie-badge-receiver {:id (:object data)} (into {:result-set-fn first :row-fn :user_id} (get-db ctx)))
        data {:event_id (:event_id data) :owner owner-id}]
    (insert-selfie-event-owner! data (get-db ctx))))

(defn get-selfie-events [ctx user-id]
  (some->> (select-issue-selfie-events {:user_id user-id} (get-db ctx))
           (take 25)))
