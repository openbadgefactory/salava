(ns salava.file.db
  (:require [slingshot.slingshot :refer :all]
            [clojure.java.jdbc :as jdbc]
            [clojure.java.io :as io]
            [clojure.string :refer [split blank?]]
            [yesql.core :refer [defqueries]]
            [salava.core.i18n :refer [t]]
            [salava.core.helper :refer [dump]]
            [salava.core.util :refer [get-db get-datasource map-sha256]]))

(defqueries "sql/file/queries.sql")

(defn user-files-all [ctx user-id]
  (let [files (select-user-files {:user_id user-id} (get-db ctx))]
    (map #(assoc % :tags (if (:tags %) (split (get % :tags "") #",") [])) files)))

(defn file-owner? [ctx file-id user-id]
  (let [owner (select-file-owner {:id file-id} (into {:result-set-fn first :row-fn :user_id} (get-db ctx)))]
    (= owner user-id)))

(defn save-file-tags!
  "Save tags associated to file. Delete existing tags."
  [ctx file-id user-id tags]
  (if (file-owner? ctx file-id user-id)
    (let [valid-tags (filter #(not (blank? %)) (distinct tags))]
      (delete-file-tags! {:file_id file-id} (get-db ctx))
      (doall (for [tag valid-tags]
               (replace-file-tag! {:file_id file-id :tag tag}
                                  (get-db ctx)))))))

(defn save-file!
  "Save file info to database"
  [ctx user-id data]
  (:generated_key (insert-file<! (assoc data :user_id user-id) (get-db ctx))))

(defn file-usage [ctx path]
  (let [usage (select-file-usage {:path path} (into {:result-set-fn first} (get-db ctx)))]
    (reduce + (vals usage))))

(defn remove-file! [ctx path]
  (let [usage (file-usage ctx path)
        data-dir (get-in ctx [:config :core :data-dir])
        full-path (str data-dir "/" path)]
    (if (empty? data-dir)
      (throw+ "Data directory does not exist"))
    (if (and (= usage 0) (.exists (io/as-file full-path)))
      (io/delete-file full-path))))

(defn remove-file-with-db! [db file-id]
  (delete-file! {:id file-id} db)
  (delete-file-tags! {:file_id file-id} db)
  (delete-files-block-file! {:file_id file-id} db))

(defn remove-user-file! [ctx file-id user-id]
  (try+
    (let [{:keys [owner path]} (select-file-owner-and-path {:id file-id} (into {:result-set-fn first} (get-db ctx)))]
      (if-not (= owner user-id)
        (throw+ "Current user is not owner of the file"))
      (jdbc/with-db-transaction
        [tr-cn (get-datasource ctx)]
        (remove-file-with-db! {:connection tr-cn} file-id))
      (remove-file! ctx path)
      {:status "success" :message (t :file/Filedeleted) :reason (t :file/Filedeleted)})
    (catch Object _
      {:status "error" :message (t :file/Errorwhiledeleting) :reason (t :file/Errorwhiledeleting)})))

(defn user-image-files
  "Get all image files by user"
  [ctx user-id]
  (select-user-image-files {:user_id user-id} (get-db ctx)))