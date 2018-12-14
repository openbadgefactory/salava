(ns salava.metabadge.db
  (:require [yesql.core :refer [defqueries]]
            [salava.core.util :as u]
            [clojure.string :refer [blank?]]))

(defqueries "sql/metabadge/queries.sql")

(defn user-badge-by-assertion
  "in case of multiple hits return the most recent"
  [ctx assertion_url]
  (some-> (select-user-badge-by-assertion-url {:assertion_url assertion_url} (u/get-db ctx))))

(defn %completed [req gotten]
  (let [calc (Math/round (double (* (/ gotten req) 100)))]
    (if (> calc 100) 100 calc)))

(defn get-required-badges
  ([ctx user_id metabadge_id]
   (let [received-required-badges (select-received-required-badges {:metabadge_id metabadge_id :user_id user_id} (u/get-db ctx))
         not-received-required-badges (select-not-received-required-badges {:metabadge_id metabadge_id} (u/get-db ctx))]
     {:received received-required-badges :not-received not-received-required-badges}))
  ([ctx user_id metabadge_id option]
   (case option
     :in_progress  (let [required-badges-map (get-required-badges ctx user_id metabadge_id)]
                     (assoc {} :required_badges (->> required-badges-map  vals concat flatten)
                       :received-badge-count (->> required-badges-map :received count)))
     :completed    (let [required-badges-map (get-required-badges ctx user_id metabadge_id)]
                     (assoc {} :required_badges (remove (fn [m] (some #(identical? (:name m) (:name %)) (:not-received required-badges-map))) (->> required-badges-map vals concat flatten))
                       :received-badge-count (->> required-badges-map :received count)
                       )))))

(defn expand-required-badges [ctx user_id option coll]
  (reduce (fn [r m]
            (let [required-badges-map (get-required-badges ctx user_id (or (:metabadge_id m) (:id m) ) option)]
              (conj r (assoc m :required_badges (->> required-badges-map :required_badges)
                        :completion_status (%completed (:min_required m) (->> required-badges-map :received-badge-count)))))) [] coll))

;;test
(declare get-metabadge)

(defn completed-metabadges [ctx user_id]
  (prn (get-metabadge ctx 514 12))
  (let [metabadges (select-completed-metabadges {:user_id user_id} (u/get-db ctx))]
    (get-metabadge ctx 454 209)
    (->> metabadges (expand-required-badges ctx user_id :completed))))

(defn metabadges-in-progress [ctx user_id]
  (let [metabadges (->> (select-all-user-metabadges {:user_id user_id} (u/get-db ctx)) (expand-required-badges ctx user_id :in_progress))
        completed-metabadges (completed-metabadges ctx user_id)]
    (remove (fn [m] (some #(= (:metabadge_id %) (:id m)) completed-metabadges)) metabadges)))

(defn all-metabadges [ctx user_id]
  {:in_progress (metabadges-in-progress ctx user_id)
   :completed (completed-metabadges ctx user_id)})

(defn is-metabadge? [ctx user_badge_id]
  (let [x (some-> (select-metabadge-info-from-user-badge {:id user_badge_id} (u/get-db ctx)) first)]
    (reduce-kv (fn [r k v]
                 (assoc r k (if (blank? v) false true))) {} x)))

(defn is-received-metabadge? [ctx user_id metabadge_id]
  (let [check (select-user-received-metabadge-by-metabadge-id {:metabadge_id metabadge_id :user_id user_id} (u/get-db ctx))]
    (if (empty? check) false true)))

(defn metabadge-helper [ctx user_badge_id user_id metabadge_ids]
  (reduce (fn [r id]
            (conj r
                  (if-let [received-metabadge? (is-received-metabadge? ctx user_id id)]
                    (->> (select-completed-metabadge-by-metabadge-id {:user_id user_id :metabadge_id id} (u/get-db ctx)) (expand-required-badges ctx user_id :completed))
                    (->> (select-factory-metabadge {:id id} (u/get-db ctx)) (expand-required-badges ctx user_id :in_progress))))) [] metabadge_ids))

(defn get-metabadge [ctx user_badge_id user_id]
  (if-let [check (empty? (is-metabadge? ctx user_badge_id))]
    {}
    (let [milestones (->> (select-completed-metabadge-by-badge-id {:user_id user_id :user_badge_id user_badge_id} (u/get-db ctx)) (expand-required-badges ctx user_id :completed))
          required-in (->> (select-required-metatabadge-by-badge-id {:id user_badge_id} (u/get-db ctx))
                           (map :metabadge_id)
                           (metabadge-helper ctx user_badge_id user_id)
                           flatten)]
      {:milestones milestones :required-in required-in})))
