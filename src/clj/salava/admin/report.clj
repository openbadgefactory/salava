(ns salava.admin.report
  (:require
   [salava.profile.db :refer [profile-metrics]]
   [salava.admin.helper :refer [make-csv]]
   [salava.admin.db :as db]
   [salava.core.i18n :refer [t]]
   [salava.core.time :refer [date-from-unix-time]]
   [yesql.core :refer [defqueries]]
   [salava.core.util :as u :refer [plugin-fun get-plugins]]
   ;[salava.extra.customField.db :refer [custom-field-value]]
   [clojure.tools.logging :as log]
   [clojure.core.reducers :as r]))

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

(defn get-badges [ctx user-id badge-ids]
  (when (seq badge-ids)
   (select-user-badges-report {:ids badge-ids :user_id user-id} (u/get-db ctx))))

(defn- process-filters [ctx filters]
  (let [{:keys [users badges]} filters
        ;badges+ (if (seq badges) badges (all-gallery-badges {} (u/get-db-col ctx :id)))
        users+  (if (seq users) users (select-all-users-for-report {} (u/get-db-col ctx :id)))]
    (assoc filters :badges badges :users users+)))

(defn- select-users [ctx user-ids page_count]
  (let [limit 50
        offset (* limit page_count)]
    (select-users-for-report-limit-fix {:ids user-ids :limit limit :offset offset} (u/get-db ctx))))

(defn- user-count [remaining page_count]
 (let [limit 50
       users-left (- remaining (* limit (inc page_count)))]
    (if (pos? users-left)
      users-left
      0)))
 
(defn- map-users-badges-count [ctx users]
  (let [ids (mapv :id users)
        shared_badgecount_col (count-shared-badges {:ids ids} (u/get-db ctx))
        total_badgecount_col  (count-all-user-badges {:ids ids} (u/get-db ctx))]
    (->> users
         (r/map #(assoc % :sharedbadges (or (some (fn [u] (when (= (:id %) (:user_id u)) (:count u) )) shared_badgecount_col) 0)))
         (r/map #(assoc % :badgecount (or (some (fn [u] (when (= (:id %) (:user_id u)) (:count u) )) total_badgecount_col) 0)))
         (r/foldcat))))

(defn- map-users-completion% [ctx users]
  (let [ids (mapv :id users)
        coll (->> ids (r/map #(hash-map :user_id % :c (:completion_percentage (profile-metrics ctx %)))) (r/foldcat))]
    (->> users
         (r/map #(assoc % :completionPercentage (some (fn [u] (when (= (:id %) (:user_id u)) (:c u))) coll)))
         (r/foldcat))))

(defn badges-for-report [ctx filters]
  (let [user-ids (:users filters)]
    (->> user-ids
        (r/reduce (fn [r u] (conj r (hash-map :user_id u
                                              :badges (some->> (badge-ids ctx (assoc filters :users [u]))
                                                               (get-badges ctx u))))) []))))

(defn- custom-field-value [ctx field user-id]
   (as-> (first (plugin-fun (get-plugins ctx) "main" "custom-field-value")) $
          (if (ifn? $) ($ field user-id) nil)))


(defn report!
  [ctx filters admin-id]
  (let [enabled-custom-fields (mapv :name (get-in ctx [:config :extra/customField :fields] nil))
        filters (process-filters ctx filters)
        user-ids (user-ids ctx filters)
        users (when (seq user-ids) (select-users ctx user-ids (:page_count filters)))
        users-with-badge-counts (when (seq users) (map-users-badges-count ctx users))
        users-with-completion% (when (seq users-with-badge-counts) (map-users-completion% ctx users-with-badge-counts))
        users-with-customfields (when (seq enabled-custom-fields)
                                     (some->> users-with-completion%
                                              (r/map #(merge % (r/reduce
                                                                  (fn [r field]
                                                                   (assoc r (keyword field) (or (custom-field-value ctx field (:id %)) (t :admin/notset))))
                                                                  {}
                                                                  enabled-custom-fields)))
                                              (r/foldcat)))]

    (if (empty? enabled-custom-fields)
        {:users users-with-completion% :user_count (user-count (count user-ids) (:page_count filters)) :total (if (seq user-ids) (count user-ids) 0)}
        {:users users-with-customfields :total (if (seq user-ids) (count user-ids) 0) :user_count (user-count (count user-ids) (:page_count filters))})))

(defn report-for-export!
  [ctx filters admin-id]
  (let [enabled-custom-fields (mapv :name (get-in ctx [:config :extra/customField :fields] nil))
        filters (process-filters ctx filters)
        user-ids (user-ids ctx filters)
        users (when (seq user-ids) (select-users-for-report-fix {:ids user-ids} (u/get-db ctx)))
        users-with-badge-counts (when (seq users) (map-users-badges-count ctx users))
        users-with-completion% (when (seq users-with-badge-counts) (map-users-completion% ctx users-with-badge-counts))
        users-with-customfields (when (seq enabled-custom-fields)
                                     (some->> users-with-completion%
                                              (r/map #(merge % (r/reduce
                                                                  (fn [r field]
                                                                   (assoc r (keyword field) (or (custom-field-value ctx field (:id %)) (t :admin/notset))))
                                                                  {}
                                                                  enabled-custom-fields)))
                                              (r/foldcat)))]

    (if (empty? enabled-custom-fields)
        {:users users-with-completion%}
        {:users users-with-customfields})))

(defn export-report [ctx users badges to from id current-user]
  (let [filters {:users (clojure.edn/read-string users)
                 :badges (clojure.edn/read-string badges)
                 :to (clojure.edn/read-string to)
                 :from (clojure.edn/read-string from)}
        ul (db/select-user-language {:id (:id current-user)} (into {:result-set-fn first :row-fn :language} (u/get-db ctx)))
        report (report-for-export! ctx filters (:id current-user))
        enabled-custom-fields (mapv :name (get-in ctx [:config :extra/customField :fields] nil))
        columns (if (seq enabled-custom-fields) (concat [:id :name] (mapv #(keyword %) enabled-custom-fields) [:activated :completionPercentage :badgecount :sharedbadges :joined :emailaddresses]) [:id :name :activated :completionPercentage :badgecount :sharedbadges :joined :emailaddresses])
        headers (map #(t (keyword (str "admin/" (name %))) ul) columns)
        result_users (->> (:users report) (mapv #(assoc % :joined (date-from-unix-time (long (* 1000 (:ctime %)))))))
        rows (mapv #(mapv % columns) result_users)
        report->csvformat  (cons headers rows)]
    (make-csv ctx report->csvformat \,)))
