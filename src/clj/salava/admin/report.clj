(ns salava.admin.report
  (:require
   [salava.profile.db :refer [profile-metrics]]
   [salava.admin.helper :refer [make-csv]]
   [salava.admin.db :as db]
   [salava.core.i18n :refer [t]]
   [salava.core.time :refer [date-from-unix-time]]
   [yesql.core :refer [defqueries]]
   [salava.core.util :as u]))

(defqueries "sql/admin/report.sql")

(defn user-ids [ctx filters]
 (let [{:keys [badges users to from]} filters
       filters-col
       (cond-> []
        (seq badges)
        (conj (set (select-user-ids-badge {:badge_ids badges :to to :from from :expected_count (count badges)} (u/get-db-col ctx :user_id))))
        (seq users)
        (conj (set users)))]

   (when (seq filters-col)
     (into [] (reduce clojure.set/intersection (first filters-col) (rest filters-col))))))

(defn badge-ids [ctx filters]
  (let [{:keys [badges users to from]} filters
        filters-col
        (cond-> []
         (seq badges)
         (conj (set badges))
         (seq users)
         (conj (set (select-badge-ids-report {:ids users :to to :from from} (u/get-db-col ctx :gallery_id)))))]
      (when (seq filters-col)
        (into [] (reduce clojure.set/intersection (first filters-col) (rest filters-col))))))

(defn get-badges [ctx badge-ids]
  (when (seq badge-ids)
   (select-user-badges-report {:ids badge-ids} (u/get-db ctx))))

(defn report!
  [ctx filters admin-id]
  (let [user-ids (user-ids ctx filters)
        users (when (seq user-ids) (some->> (select-users-for-report {:ids user-ids} (u/get-db ctx))
                                            (mapv #(assoc % :completionPercentage (:completion_percentage (profile-metrics ctx (:id %)))))))
        users-with-badges (reduce
                            (fn [r user]
                              (conj r
                               (assoc user :badges (filter #(= (:user_id %) (:id user)) (some->> (badge-ids ctx (assoc filters :users [(:id user)]))
                                                                                                 (get-badges ctx))))))
                            []
                            users)]
    {:users users-with-badges}))

(defn export-report [ctx users badges to from id current-user]
  (let [filters {:users (clojure.edn/read-string users)
                 :badges (clojure.edn/read-string badges)
                 :to (clojure.edn/read-string to)
                 :from (clojure.edn/read-string from)}
        ul (db/select-user-language {:id (:id current-user)} (into {:result-set-fn first :row-fn :language} (u/get-db ctx)))
        report (report! ctx filters (:id current-user))
        columns [:id :name :activated :completionPercentage :badgecount :sharedbadges :joined]
        headers (map #(t (keyword (str "admin/" (name %))) ul) columns)
        result_users (->> (:users report) (mapv #(assoc % :joined (date-from-unix-time (long (* 1000 (:ctime %)))))))
        rows (mapv #(mapv % columns) result_users)
        report->csvformat  (cons headers rows)]
    (make-csv ctx report->csvformat \,)))
