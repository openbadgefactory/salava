(ns salava.admin.report
  (:require
   [salava.profile.db :refer [profile-metrics]]
   [salava.admin.helper :refer [make-csv]]
   [salava.admin.db :as db]
   [salava.core.i18n :refer [t]]
   [salava.core.time :refer [date-from-unix-time]]
   [yesql.core :refer [defqueries]]
   [salava.core.util :as u]
   [salava.extra.customField.db :refer [custom-field-value]]
   [clojure.tools.logging :as log]))

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

(defn- process-filters [ctx filters]
  (let [{:keys [users badges]} filters
        badges+ (if (seq badges) badges (all-gallery-badges {} (u/get-db-col ctx :id)))
        users+  (if (seq users) users (select-all-users-for-report {} (u/get-db-col ctx :id)))]
    (assoc filters :badges badges :users users+)))

(defn- select-users [ctx user-ids page_count]
  (let [limit 50
        offset (* limit page_count)]
    (select-users-for-report-limit {:ids user-ids :limit limit :offset offset} (u/get-db ctx))))

(defn- user-count [remaining page_count]
 (let [limit 50
       users-left (- remaining (* limit (inc page_count)))]
    (if (pos? users-left)
      users-left
      0)))

(defn report!
  [ctx filters admin-id]
  (let [enabled-custom-fields (mapv :name (get-in ctx [:config :extra/customField :fields] nil))
        filters (process-filters ctx filters)
        user-ids (user-ids ctx filters)
        users (when (seq user-ids) (some->> (select-users ctx user-ids (:page_count filters)) #_(select-users-for-report {:ids user-ids} (u/get-db ctx))
                                            (pmap #(assoc % :completionPercentage (:completion_percentage (profile-metrics ctx (:id %)))))))
        users-with-badges (reduce
                            (fn [r user]
                              (conj r
                               (assoc user :badges (filter #(= (:user_id %) (:id user)) (some->> (badge-ids ctx (assoc filters :users [(:id user)]))
                                                                                                 (get-badges ctx))))))
                            []
                            users)
        users-with-customfields (when (seq enabled-custom-fields)(some->> users-with-badges (mapv #(merge % (reduce
                                                                                                             (fn [r field]
                                                                                                               (assoc r (keyword field) (or (custom-field-value ctx field (:id %)) (t :admin/notset))))
                                                                                                             {}
                                                                                                             enabled-custom-fields)))))]
    (if (empty? enabled-custom-fields)
        {:users users-with-badges :user_count (user-count (count user-ids) (:page_count filters)) :total (count user-ids)}
        {:users users-with-customfields :total (count user-ids) :user_count (user-count (count user-ids) (:page_count filters))})))

(defn report-for-export!
  [ctx filters admin-id]
  (let [enabled-custom-fields (mapv :name (get-in ctx [:config :extra/customField :fields] nil))
        filters (process-filters ctx filters)
        user-ids (user-ids ctx filters)
        users (when (seq user-ids) (some->> (select-users-for-report {:ids user-ids} (u/get-db ctx))
                                            (pmap #(assoc % :completionPercentage (:completion_percentage (profile-metrics ctx (:id %)))))))
        users-with-badges (reduce
                            (fn [r user]
                              (conj r
                               (assoc user :badges (filter #(= (:user_id %) (:id user)) (some->> (badge-ids ctx (assoc filters :users [(:id user)]))
                                                                                                 (get-badges ctx))))))
                            []
                            users)
        users-with-customfields (when (seq enabled-custom-fields)(some->> users-with-badges (pmap #(merge % (reduce
                                                                                                             (fn [r field]
                                                                                                               (assoc r (keyword field) (or (custom-field-value ctx field (:id %)) (t :admin/notset))))
                                                                                                             {}
                                                                                                             enabled-custom-fields)))))]

    (if (empty? enabled-custom-fields)
        {:users users-with-badges}
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
