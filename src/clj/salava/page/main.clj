(ns salava.page.main
  (:require [clojure.string :refer [split blank? trim]]
            [yesql.core :refer [defqueries]]
            [slingshot.slingshot :refer :all]
            [salava.core.time :refer [unix-time]]
            [salava.core.i18n :refer [t]]
            [salava.core.helper :refer [dump]]
            [salava.core.util :refer [get-db]]
            [salava.badge.main :as b]
            [salava.page.themes :refer [valid-theme-id valid-border-id border-attributes]]
            [salava.file.db :as f]))

(defqueries "sql/page/main.sql")

(defn page-owner?
  "Check if user owns page"
  [ctx page-id user-id]
  (let [owner (select-page-owner {:id page-id} (into {:result-set-fn first :row-fn :user_id} (get-db ctx)))]
    (= owner user-id)))

(defn page-badges [ctx pages-coll]
  ""
  (let [badge-ids (->> pages-coll
                       (reduce #(concat %1 (split (or (:badges %2) "") #",")) [])
                       (filter not-empty)
                       distinct)
        badges-by-id (b/badges-images-names ctx badge-ids)]
    (map (fn [page]
           (assoc page :badges (if (:badges page)
                                 (map #(get badges-by-id %) (split (:badges page) #",")))))
         pages-coll)))

(defn user-pages-all [ctx user-id]
  "Get all user pages"
  (let [pages (select-user-pages {:user_id user-id} (get-db ctx))
        pages-with-badges (page-badges ctx pages)]
    (map #(assoc % :tags (if (:tags %) (split (get % :tags "") #",") []))
         pages-with-badges)))

(defn create-empty-page! [ctx user-id]
  (:generated_key (insert-empty-page<! {:user_id user-id
                                        :name    (t :page/Untitled)} (get-db ctx))))

(defn page-owner [ctx page-id]
  (select-page-owner {:id page-id} (into {:result-set-fn first :row-fn :user_id} (get-db ctx))))

(defn tag-blocks [ctx page-id]
  (let [blocks (select-pages-tag-blocks {:page_id page-id} (get-db ctx))
        owner-id (page-owner ctx page-id)]
    (map #(assoc % :badges (b/badges-by-tag-and-owner ctx (:tag %) owner-id)) blocks)))

(defn file-blocks [ctx page-id]
  (let [blocks (select-pages-files-blocks {:page_id page-id} (get-db ctx))]
    (map #(assoc % :files (select-files-block-content {:block_id (:id %)} (get-db ctx))) blocks)))

(defn page-blocks [ctx page-id]
  (let [badge-blocks (select-pages-badge-blocks {:page_id page-id} (get-db ctx))
        file-blocks (file-blocks ctx page-id)
        heading-blocks (select-pages-heading-blocks {:page_id page-id} (get-db ctx))
        html-blocks (select-pages-html-blocks {:page_id page-id} (get-db ctx))
        tag-blocks (tag-blocks ctx page-id)
        blocks (concat badge-blocks file-blocks heading-blocks html-blocks tag-blocks)]
    (sort-by :block_order blocks)))

(defn badge-blocks-for-edit [ctx page-id]
  (let [blocks (select-pages-badge-blocks {:page_id page-id} (get-db ctx))]
    (map #(hash-map :id (:id %)
                    :type (:type %)
                    :block_order (:block_order %)
                    :format (:format %)
                    :badge {:id (:badge_id %)
                            :name (:name %)
                            :image_file (:image_file %)}) blocks)))

(defn heading-blocks-for-edit [ctx page-id]
  (let [blocks (select-pages-heading-blocks {:page_id page-id} (get-db ctx))]
    (map #(hash-map :id (:id %)
                    :type (if (= (:size %) "h1")
                            "heading"
                            "sub-heading")
                    :content (:content %)
                    :block_order (:block_order %)) blocks)))

(defn page-blocks-for-edit [ctx page-id]
  (let [badge-blocks (badge-blocks-for-edit ctx page-id)
        heading-blocks (heading-blocks-for-edit ctx page-id)
        file-blocks (file-blocks ctx page-id)
        html-blocks (select-pages-html-blocks {:page_id page-id} (get-db ctx))
        tag-blocks (select-pages-tag-blocks {:page_id page-id} (get-db ctx))
        blocks (concat badge-blocks file-blocks heading-blocks html-blocks tag-blocks)]
    (sort-by :block_order blocks)))

(defn page-with-blocks [ctx page-id]
  (let [page (select-page {:id page-id} (into {:result-set-fn first} (get-db ctx)))
        blocks (page-blocks ctx page-id)]
    (assoc page :blocks blocks
                :border (border-attributes (:border page)))))

(defn page-with-blocks-for-owner [ctx page-id user-id]
  (if (page-owner? ctx page-id user-id)
    (page-with-blocks ctx page-id)))

(defn page-for-edit [ctx page-id user-id]
  (if (page-owner? ctx page-id user-id)
    (let [page (select-keys (select-page {:id page-id} (into {:result-set-fn first} (get-db ctx))) [:id :user_id :name :description])
          blocks (page-blocks-for-edit ctx page-id)
          owner (:user_id page)
          badges (map #(select-keys % [:id :name :image_file :tags]) (b/user-badges-all ctx owner))
          files (map #(select-keys % [:id :name :path :mime_type :size]) (f/user-files-all ctx owner))
          tags (distinct (flatten (map :tags badges)))]
      {:page (assoc page :blocks blocks) :badges badges :tags tags :files files})))

(defn delete-block! [ctx block]
  (case (:type block)
    "heading" (delete-heading-block! block (get-db ctx))
    "badge" (delete-badge-block! block (get-db ctx))
    "html" (delete-html-block! block (get-db ctx))
    "file" (do
             (delete-files-block! block (get-db ctx))
             (delete-files-block-files! {:block_id (:id block)} (get-db ctx)))
    "tag" (delete-tag-block! block (get-db ctx))))

(defn save-files-block-content [ctx block]
  (delete-files-block-files! {:block_id (:id block)} (get-db ctx))
  (doseq [[file-id index] (map list (:files block) (range (count (:files block))))]
    (insert-files-block-file! {:block_id (:id block) :file_id file-id :file_order index} (get-db ctx))))

(defn update-files-block-and-content! [ctx block]
  (update-files-block! block (get-db ctx))
  (save-files-block-content ctx block))

(defn create-files-block! [ctx block]
  (let [block-id (:generated_key (insert-files-block<! block (get-db ctx)))]
    (save-files-block-content ctx (assoc block :id block-id))))

(defn save-page-content! [ctx page-id page-content user-id]
  (try+
    (if-not (page-owner? ctx page-id user-id)
      (throw+ "Page is not owned by current user"))
    (let [{:keys [name description blocks]} page-content
          page-owner-id (page-owner ctx page-id)
          user-files (if (some #(= "file" (:type %)) blocks)
                       (f/user-files-all ctx page-owner-id))
          file-ids (map :id user-files)
          user-badges (if (some #(= "badge" (:type %)) blocks)
                        (b/user-badges-all ctx page-owner-id))
          badge-ids (map :id user-badges)
          page-blocks (page-blocks ctx page-id)]
      (update-page-name-description! {:id page-id :name name :description description} (get-db ctx))
      (doseq [block-index (range (count blocks))]
        (let [block (-> (nth blocks block-index)
                        (assoc :page_id page-id
                               :block_order block-index))
              id (and (:id block)
                      (some #(and (= (:type %) (:type block)) (= (:id %) (:id block))) page-blocks))]
          (case (:type block)
            "heading" (if id
                        (update-heading-block! block (get-db ctx))
                        (insert-heading-block! block (get-db ctx)))
            "badge" (when (some #(= % (:badge_id block)) badge-ids)
                      (if id
                        (update-badge-block! block (get-db ctx))
                        (insert-badge-block! block (get-db ctx))))
            "html" (if id
                     (update-html-block! block (get-db ctx))
                     (insert-html-block! block (get-db ctx)))
            "file" (when (= (->> (:files block)
                                 (filter (fn [x] (some #(= x %) file-ids)))
                                 count)
                            (count (:files block)))
                     (if id
                       (update-files-block-and-content! ctx block)
                       (create-files-block! ctx block)))
            "tag" (if id
                    (update-tag-block! block (get-db ctx))
                    (insert-tag-block! block (get-db ctx))))))
      (doseq [old-block page-blocks]
        (if-not (some #(and (= (:type old-block) (:type %)) (= (:id old-block) (:id %))) blocks)
          (delete-block! ctx old-block)))
      {:status "success" :message (t :page/Pagesavedsuccessfully)})
    (catch Object _
      {:status "error" :message (t :page/Errorwhilesavingpage)})))

(defn set-theme! [ctx page-id theme-id border-id padding user-id]
  (try+
    (if-not (page-owner? ctx page-id user-id)
      (throw+ "Page is not owned by current user"))
    (update-page-theme! {:id page-id :theme (valid-theme-id theme-id) :border (valid-border-id border-id) :padding padding} (get-db ctx))
    {:status "success" :message (t :page/Pagesavedsuccessfully)}
    (catch Object _
      {:status "error" :message (t :page/Errorwhilesavingpage)})))

(defn page-settings [ctx page-id user-id]
  (if (page-owner? ctx page-id user-id)
    (let [page (select-page {:id page-id} (into {:result-set-fn first} (get-db ctx)))]
      (when page
        (assoc page :tags (if (:tags page) (split (:tags page) #",") []))))))

(defn save-page-tags!
  "Save tags associated to page. Delete existing tags."
  [ctx page-id tags]
  (let [valid-tags (filter #(not (blank? %)) (distinct tags))]
    (delete-page-tags! {:page_id page-id} (get-db ctx))
    (doall (for [tag valid-tags]
             (replace-page-tag! {:page_id page-id :tag tag}
                                 (get-db ctx))))))

(defn save-page-settings! [ctx page-id tags visibility pword user-id]
  (try+
    (if-not (page-owner? ctx page-id user-id)
      (throw+ "Page is not owned by current user"))
    (let [password (if (= visibility "password") (trim pword) "")
          page-visibility (if (and (= visibility "password")
                                   (empty? password))
                            "private"
                            visibility)]
      (update-page-visibility-and-password! {:id page-id :visibility page-visibility :password password} (get-db ctx))
      (save-page-tags! ctx page-id tags)
      {:status "success" :message (t :page/Pagesavedsuccessfully)})
    (catch Object _
      {:status "error" :message (t :page/Errorwhilesavingpage)})))

(defn remove-files-blocks-and-content! [ctx page-id]
  (let [file-blocks (select-pages-files-blocks {:page_id page-id} (get-db ctx))]
    (doseq [file-block file-blocks]
      (delete-files-block-files! {:block_id (:id file-block)} (get-db ctx)))
    (delete-files-blocks! {:page_id page-id} (get-db ctx))))

(defn delete-blocks! [ctx page-id]
  (delete-heading-blocks! {:page_id page-id} (get-db ctx))
  (delete-badge-blocks! {:page_id page-id} (get-db ctx))
  (delete-html-blocks! {:page_id page-id} (get-db ctx))
  (remove-files-blocks-and-content! ctx page-id)
  (delete-tag-blocks! {:page_id page-id} (get-db ctx)))

(defn delete-page-by-id! [ctx page-id user-id]
  (when (page-owner? ctx page-id user-id)
    (delete-blocks! ctx page-id)
    (delete-page-tags! {:page_id page-id} (get-db ctx))
    (delete-page! {:id page-id} (get-db ctx))))

(defn toggle-visibility! [ctx page-id visibility user-id]
  (if (page-owner? ctx page-id user-id)
    (do
      (update-page-visibility! {:id page-id :visibility visibility} (get-db ctx))
      visibility)
    (if (= visibility "public") "private" "public")))