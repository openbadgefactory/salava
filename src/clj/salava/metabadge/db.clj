(ns salava.metabadge.db
  (:require [yesql.core :refer [defqueries]]
            [salava.core.util :as u]
            [salava.core.http :as http]
            [clojure.tools.logging :as log]
            [clojure.string :refer [blank?]]
            [clojure.java.jdbc :as jdbc]))

(defqueries "sql/metabadge/queries.sql")

(defn- fetch-json-data [url]
  (try
    (http/http-get url {:as :json :accept :json})
    (catch Exception e
      (log/error (str "Could not fetch data: " url))
      {})))

(defn user-badge-by-assertion
  [ctx assertion_url]
  (some-> (select-user-badge-by-assertion-url {:assertion_url assertion_url} (u/get-db ctx))))

(defn metabadge?!
  "checks if badge is a metabadge, db is updated with information"
  [ctx factory-url user_badge]
  (let [url (str factory-url "/v1/assertion/is_metabadge/?url=" (u/url-encode (:assertion_url user_badge)))
        data (fetch-json-data url)]
    (jdbc/with-db-transaction  [db-conn (:connection (u/get-db ctx))]
      (delete-user-badge-metabadge! {:user_badge_id (:id user_badge)} {:connection db-conn})
      (when-not (every? nil? (-> data (dissoc :last_modified) vals))
        (if (nil? (:metabadge data))
          (insert-user-badge-metabadge! {:user_badge_id (:id user_badge) :meta_badge "NULL" :meta_badge_req (:required_badge data) :last_modified (:last_modified data)} {:connection db-conn})
          (doseq [metabadge-id (:metabadge data)]
            (insert-user-badge-metabadge! {:user_badge_id (:id user_badge) :meta_badge metabadge-id :meta_badge_req (:required_badge data) :last_modified (:last_modified data)} {:connection db-conn})))))
    (if (empty? data) false true)))

(defn badge-url [s]
  (if (clojure.string/blank? s) nil (->> (clojure.string/split s #"&event=") first)))

(defn get-metabadge! [ctx factory-url user_badge]
  (let [url (str factory-url "/v1/assertion/metabadge/?url=" (u/url-encode (:assertion_url user_badge)))
        metabadges (:metabadge (fetch-json-data url))]
    (try
      (when-not (empty? metabadges)
        (doseq [metabadge metabadges
                :let [metabadge-badge-content (if-not (-> metabadge :badge :received)
                                                (fetch-json-data (-> metabadge :badge :url))
                                                (some->> (:badge (fetch-json-data (-> metabadge :badge :url)))
                                                         (badge-url)
                                                         (fetch-json-data)))
                      {:keys [description criteria image]} metabadge-badge-content]]
          (when-not (empty? metabadge-badge-content)
            (jdbc/with-db-transaction  [db-conn (:connection (u/get-db ctx))]
              (delete-factory-metabadge! {:id (:id metabadge)} {:connection db-conn})
              (insert-factory-metabadge! {:id (:id metabadge) :name (:name metabadge) :description description :criteria (u/md->html criteria) :image_file (u/file-from-url ctx image) :min_required (:min_required metabadge)} {:connection db-conn}))
            (doseq [badge (:required_badges metabadge)
                    :let [required-badge-content (if-not (:received badge)
                                                   (fetch-json-data (:url badge))
                                                   (some->> (:badge (fetch-json-data (:url badge)))
                                                            (badge-url)
                                                            (fetch-json-data)))
                          {:keys [name description image criteria]} required-badge-content]]
              (when-not (empty? required-badge-content)
                (jdbc/with-db-transaction  [db-conn (:connection (u/get-db ctx))]
                  (delete-factory-metabadge-required-badge! {:metabadge_id (:id metabadge) :required_badge_id (:id badge)} {:connection db-conn})
                  (insert-factory-metabadge-required-badge! {:metabadge_id (:id metabadge) :required_badge_id (:id badge) :name name :description description :criteria (u/md->html criteria) :image_file (u/file-from-url ctx image)} {:connection db-conn})
                  ))))))
    (catch Exception e
      (log/error (.getMessage e))))))

(defn all-badges [ctx factory-url]
  (select-all-badges {:obf_url (str factory-url "%")} (u/get-db ctx)))

(defn all-metabadges [ctx]
  (select-all-metabadges {} (u/get-db ctx)))

