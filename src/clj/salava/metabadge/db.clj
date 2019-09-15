(ns salava.metabadge.db
  (:require [clojure.pprint :refer [pprint]]
            [yesql.core :refer [defqueries]]
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

(defn clear-user-metabadge! [ctx user_badge_id]
  (delete-user-badge-metabadge! {:user_badge_id user_badge_id} (u/get-db ctx)))

(defn badge-url [s]
  (if (blank? s) nil (->> (clojure.string/split s #"&event=") first)))

(defn url? [s]
 (not (blank? (re-find #"^http" (str s)))))

(defn get-metabadge! [ctx factory-url user_badge]
  (let [url (str factory-url "/v1/assertion/metabadge/?url=" (:assertion_url user_badge) #_(u/url-encode (:assertion_url user_badge)))
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
              (replace-factory-metabadge! {:id (:id metabadge) :remote_issuer_id (:remote_issuer_id metabadge) :name (:name metabadge) :description (:description metabadge-badge-content) :criteria (u/md->html (:criteria metabadge-badge-content)) :image_file (u/file-from-url-fix ctx (:image metabadge-badge-content)) :min_required (:min_required metabadge) :ctime (u/now) :mtime (u/now)} {:connection db-conn})
              #_(insert-factory-metabadge! {:id (:id metabadge) :remote_issuer_id (:remote_issuer_id metabadge) :name (:name metabadge) :description description :criteria (u/md->html criteria) :image_file (u/file-from-url ctx image) :min_required (:min_required metabadge) :factory_url (:factory_url metabadge)} {:connection db-conn})
              (doseq [badge (:required_badges metabadge)
                      :let [required-badge-content (if-not (:received badge)
                                                     (fetch-json-data (:url badge))
                                                     (some->> (:badge (fetch-json-data (:url badge)))
                                                              (badge-url)
                                                              (fetch-json-data)))
                            {:keys [name description image criteria]} required-badge-content]]
                (when-not (empty? required-badge-content)
                  (insert-factory-metabadge-required-badge! {:metabadge_id (:id metabadge) :required_badge_id (:id badge) :name (:name required-badge-content) :description (:description required-badge-content) :criteria (u/md->html (:criteria required-badge-content)) :image_file (u/file-from-url-fix ctx (:image required-badge-content))} {:connection db-conn})
                  ))))))
      (catch Exception e
        (log/error "Could not get metabadge info from factory id:" (:id user_badge)", assertion: " (:assertion_url user_badge))
        (log/error (.getMessage e))))))

(defn metabadge?!
  "checks if badge is a metabadge, db is updated with information"
  [ctx factory-url user_badge]
  (let [url (str factory-url "/v1/assertion/is_metabadge/?url=" (:assertion_url user_badge) #_(u/url-encode (:assertion_url user_badge)))
        data (fetch-json-data url)]
    (jdbc/with-db-transaction  [db-conn (:connection (u/get-db ctx))]
      (delete-user-badge-metabadge! {:user_badge_id (:id user_badge)} {:connection db-conn})
      (when-not (every? nil? (-> data (dissoc :last_modified) vals))
        (if (nil? (:metabadge data))
         (let [saved-required-badge (select-factory-required-badge-by-required-badge-id {:id (:required_badge data)} {:connection db-conn})]
           (insert-user-badge-metabadge! {:user_badge_id (:id user_badge) :meta_badge "NULL" :meta_badge_req (:required_badge data) :last_modified (:last_modified data)} {:connection db-conn})
           (when (empty? saved-required-badge)
             (do (log/info "Required metabadge info not found in db, Getting info from factory")
                 (get-metabadge! ctx factory-url user_badge))))
         (doseq [metabadge-id (:metabadge data)]
           (let [saved-metabadge (select-factory-metabadge {:id metabadge-id} {:connection db-conn})]
            (insert-user-badge-metabadge! {:user_badge_id (:id user_badge) :meta_badge metabadge-id :meta_badge_req (:required_badge data) :last_modified (:last_modified data)} {:connection db-conn})
            (when (empty? saved-metabadge)
             (do (log/info "Metabadge info not found in db, Getting metabadge info from factory")
                 (get-metabadge! ctx factory-url user_badge))))))))
    (if (empty? data) false true)))


(defn all-badges [ctx factory-url]
  (select-all-badges {:obf_url (str factory-url "%")} (u/get-db ctx)))

(defn all-metabadges [ctx]
  (select-all-metabadges {} (u/get-db ctx)))


(defn metabadge-update [ctx data]
  (try
    (jdbc/with-db-transaction  [tx (:connection (u/get-db ctx))]
      (doseq [mb (:metabadges data)]
        (delete-factory-metabadge! {:id (:id mb)} {:connection tx})
        (-> mb
            (assoc :image_file (u/file-from-url ctx (:image mb)))
            (assoc :remote_issuer_id (:remote_issuer_id data))
            (replace-factory-metabadge! {:connection tx}))
        (doseq [rb (:required_badges mb)]
          (-> rb
              (assoc :image_file (u/file-from-url ctx (:image rb))
                     :application_url (get-in rb [:badge_application :application_url] nil)
                     :not_after (get-in rb [:badge_application :not_after] nil)
                     :not_before (get-in rb [:badge_application :not_before] nil))
              (dissoc :badge_application)
              (replace-factory-metabadge-required! {:connection tx}))))

      (when (pos? (count (:deleted_metabadges data)))
        (delete-factory-metabadge-multi! data {:connection tx}))

      (when (pos? (count (:deleted_badges data)))
        (delete-factory-metabadge-required-multi! data {:connection tx})))

    {:success true}

    (catch Exception e
      (log/error (.getMessage e))
      {:success false})))
