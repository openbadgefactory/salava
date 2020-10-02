(ns salava.badgeIssuer.db
 (:require
  [clojure.data.json :as json]
  [clojure.string :refer [blank?]]
  [clojure.tools.logging :as log]
  [salava.core.util :refer [get-db get-db-1 file-from-url-fix get-full-path md->html get-db-col]]
  [salava.core.helper :refer [string->number]]
  [slingshot.slingshot :refer :all]
  [yesql.core :refer [defqueries]]
  [salava.core.countries :refer [all-countries sort-countries]]))

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

(defn- selfie-count [remaining page_count]
  (let [limit 20
        selfies-left (- remaining (* limit (inc page_count)))]
    (if (pos? selfies-left)
      selfies-left
      0)))

(defn- select-selfies [ctx country selfie_ids order page_count]
  (let [limit 20
        offset (* limit page_count)]
    (if (nil? selfie_ids)
      (select-selfie-badges-all {:country country :limit limit :offset offset :order order} (get-db ctx))
      (if (empty? selfie_ids)
        []
        (select-selfie-badges-filtered {:country country :limit limit :offset offset :order order :selfies selfie_ids} (get-db ctx))))))

(defn get-selfie-ids [ctx params]
  (let [{:keys [name creator page_count]} params
        filters
        (cond-> []
          (not (blank? name))
          (conj (set (select-selfie-ids-badge {:badge (str "%" name "%")} (get-db-col ctx :id))))
          (not (blank? creator))
          (conj (set (select-selfie-ids-creator {:creator (str "%" creator "%")} (get-db-col ctx :id)))))]
       ;; Get final filtered gallery_id list
       (when (seq filters)
         (into [] (reduce clojure.set/intersection (first filters) (rest filters))))))

(defn all-created-selfies [ctx params]
  (let [{:keys [name creator order page_count country]} params
        offset page_count;(string->number page_count)
        selfie-ids (get-selfie-ids ctx params)
        selfies (select-selfies ctx country selfie-ids order offset)]
     {:selfies (some->> selfies (reduce (fn [r s]
                                         (conj r (-> s (assoc :tags (if (blank? (:tags s)) nil (json/read-str (:tags s)))
                                                              :criteria_html (md->html (:criteria s))))))
                                  []))
      :selfie_count (selfie-count (if (nil? selfie-ids)
                                    (get (select-total-selfie-count {:country country} (get-db-1 ctx)) :total 0)
                                    (count selfie-ids))
                                  offset)}))
(defn get-selfie-countries [ctx]
  (let [countries (select-selfie-countries {} (get-db-col ctx :country))]
    (hash-map :countries (-> all-countries
                             (select-keys countries)
                             (sort-countries)
                             (seq)))))
