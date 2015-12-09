(ns salava.file.db
  (:require [slingshot.slingshot :refer :all]
            [clojure.java.io :refer [delete-file]]
            [clojure.string :refer [split blank?]]
            [yesql.core :refer [defqueries]]
            [salava.core.i18n :refer [t]]
            [salava.core.helper :refer [dump]]
            [salava.core.util :refer [get-db map-sha256 file-from-url]]))

(defqueries "sql/file/queries.sql")

(defn user-files-all [ctx user-id]
  (let [files (select-user-files {:user_id user-id} (get-db ctx))]
    (map #(assoc % :tags (if (:tags %) (split (get % :tags "") #",") [])) files)))

(defn save-file-tags!
  "Save tags associated to file. Delete existing tags."
  [ctx file-id tags]
  (let [valid-tags (filter #(not (blank? %)) (distinct tags))]
    (delete-file-tags! {:file_id file-id} (get-db ctx))
    (doall (for [tag valid-tags]
             (replace-file-tag! {:file_id file-id :tag tag}
                                (get-db ctx))))))

(defn save-file!
  "Save file info to database"
  [ctx user-id data]
  (:generated_key (insert-file<! (assoc data :user_id user-id) (get-db ctx))))

(defn remove-file! [ctx file-id]
  (try+
    (let [file (select-file-count-and-path {:id file-id} (into {:result-set-fn first} (get-db ctx)))]
      (if (= (:usage file) 1)
        (delete-file (str "resources/public/" (:path file))))
      (delete-file! {:id file-id} (get-db ctx))
      (delete-files-block-file! {:file_id file-id} (get-db ctx))
      {:status "success" :message (t :file/Filedeleted) :reason (t :file/Filedeleted)})
    (catch Object _
      {:status "error" :message (t :file/Errorwhiledeleting) :reason (t :file/Errorwhiledeleting)})))