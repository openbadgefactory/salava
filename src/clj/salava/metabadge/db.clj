(ns salava.metabadge.db
  (:require [yesql.core :refer [defqueries]]
            [salava.core.util :as u]
            [clojure.string :refer [blank?]]))

(defqueries "sql/metabadge/queries.sql")

(defn user-badge-by-assertion
  "in case of multiple hits return the most recent"
  [ctx assertion_url]
  (some-> (select-user-badge-by-assertion-url {:assertion_url assertion_url} (u/get-db ctx)) first))

(defn %completed [req gotten]
  (let [calc (Math/round (double (* (/ gotten req) 100)))]
    (if (> calc 100) 100 calc)))

(defn get-completed-metabadges [ctx user_id]
  (let [metabadges (select-completed-metabadges {:user_id user_id} (u/get-db ctx))]
    (reduce (fn [r m]
              (let [not-received-required-badges (select-not-received-required-badges {:metabadge_id (:metabadge_id m)} (u/get-db ctx))
                    received-required-badges (select-received-required-badges {:metabadge_id (:metabadge_id m) :user_id user_id} (u/get-db ctx))
                    required-badges (remove (fn [m] (some #(identical? (:name m) (:name %)) not-received-required-badges))
                                            (concat received-required-badges not-received-required-badges))]
                (conj r (assoc m :required_badges required-badges
                          :completion_status (%completed (:min_required m) (count received-required-badges)))) )) [] metabadges)))
;;TODO refactor

(defn get-metabadges-in-progress [ctx user_id]
  (let [all-metabadges (reduce (fn [r m]
                                 (let [not-received-required-badges (select-not-received-required-badges {:metabadge_id (:id m)} (u/get-db ctx))
                                       received-required-badges (select-received-required-badges {:metabadge_id (:id m) :user_id user_id} (u/get-db ctx))
                                       required-badges (concat received-required-badges not-received-required-badges) #_(remove (fn [m] (some #(identical? (:name m) (:name %)) not-received-required-badges))
                                                                                                                                (concat received-required-badges not-received-required-badges))]
                                   (conj r (assoc m :required_badges required-badges
                                             :completion_status (%completed (:min_required m) (count received-required-badges)))) )) [] (select-all-user-metabadges {:user_id user_id} (u/get-db ctx)))
        completed-metabadges (get-completed-metabadges ctx user_id)]
    (remove (fn [m] (some #(= (:metabadge_id %) (:id m)) completed-metabadges)) all-metabadges)))

(defn all-metabadges [ctx user_id]
  {:in_progress (get-metabadges-in-progress ctx user_id)
   :completed (get-completed-metabadges ctx user_id)})

(defn is-metabadge? [ctx user_badge_id]
  (let [resp (some-> (select-metabadge-info-from-user-badge {:id user_badge_id} (u/get-db ctx)) first)]
    (reduce-kv (fn [r k v]
          (assoc r k (if (blank? v) false true))) {} resp)))

;;TODO
#_(defn metabadge-helper [ctx user_badge_id user_id ids]
  (reduce (fn [r id]
            (let [received-metabadge? ])
            ) [] ids)
  )
;;TODO
#_(defn get-metabadge [ctx user_badge_id user_id]
  (if-let [check (empty? (is-metabadge? ctx user_badge_id))]
    {}
    (let [milestones (select-completed-metabadge-by-badge-id {:user_id user_id :user_badge_id user_badge_id} (u/get-db ctx))
          required (select-required-metatabadge-by-badge-id {:id user_badge_id} (u/get-db ctx))])

    )
  )
