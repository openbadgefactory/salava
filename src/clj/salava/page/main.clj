(ns salava.page.main
  (:require [clojure.string :refer [split blank? trim]]
            [yesql.core :refer [defqueries]]
            [clojure.java.jdbc :as jdbc]
            [clojure.set :refer [rename-keys]]
            [slingshot.slingshot :refer :all]
            [autoclave.core :refer :all]
            [salava.core.time :refer [unix-time date-from-unix-time]]
            [salava.core.i18n :refer [t]]
            [salava.core.helper :refer [dump private?]]
            [salava.core.util :as u :refer [get-db get-datasource get-site-url get-base-path str->qr-base64 md->html plugin-fun get-plugins file-from-url md->html]]
            [salava.badge.main :as b]
            [clojure.tools.logging :as log]
            [salava.page.themes :refer [valid-theme-id valid-border-id border-attributes]]
            [salava.file.db :as f]
            [clj-pdf.core :as pdf]
            [clj-pdf-markdown.core :refer [markdown->clj-pdf]]
            [clojure.zip :as zip]
            [net.cgrand.enlive-html :as enlive]
            ))


(defqueries "sql/page/main.sql")

(defn page-url [ctx page-id]
  (str (get-site-url ctx) (get-base-path ctx) "/page/view/" page-id))

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
  (let [language (select-user-language {:id user-id} (into {:result-set-fn first :row-fn :language} (get-db ctx)))
        name (or (t :page/Untitled language) "Untitled")]
    (:generated_key (insert-empty-page<! {:user_id user-id
                                          :name    name} (get-db ctx)))))

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
  (let [badge-blocks (map #(update % :criteria_content md->html) (select-pages-badge-blocks {:page_id page-id} (get-db ctx)))
        file-blocks (file-blocks ctx page-id)
        heading-blocks (select-pages-heading-blocks {:page_id page-id} (get-db ctx))
        html-blocks (select-pages-html-blocks {:page_id page-id} (get-db ctx))
        tag-blocks (tag-blocks ctx page-id)
        blocks (concat badge-blocks file-blocks heading-blocks html-blocks tag-blocks)]
    (sort-by :block_order blocks)))

(defn badge-blocks-for-edit [ctx page-id]
  (let [blocks (map #(update % :criteria_content md->html) (select-pages-badge-blocks {:page_id page-id} (get-db ctx)))]
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
      :border (border-attributes (:border page))
      :qr_code (str->qr-base64 (page-url ctx page-id)))))


(defn generate-pdf [ctx page-id]
  (let [ blocks (page-blocks ctx page-id)
         badge-block-with-markdown (:criteria_content (select-pages-badge-blocks {:page_id page-id} (into {:result-set-fn first} (get-db ctx))))
         page (conj () (-> (select-page {:id page-id} (get-db ctx))
                           first
                           (assoc :blocks blocks)))
         data-dir (get-in ctx [:config :core :data-dir])
         page-template (pdf/template
                         (let [template #(cons [:paragraph] [#_[:heading {:size :15 :align :center} $name] [:spacer 0]
                                                             #_[:paragraph {:align :center}
                                                                [:chunk (str $first_name " " $last_name)]][:spacer 1]
                                                             [:line {:dotted true}]
                                                             [:spacer 2]
                                                             (if (= "heading"  (:type %))
                                                               (case (:size %)
                                                                 "h1" [:paragraph {:align :center}
                                                                       [:heading (:content %)]]
                                                                 "h2" [:paragraph {:align :center}
                                                                       [:heading {:style {:size 10 :align :center}}  (:content %)]] )" ")
                                                             (if (= "badge" (:type %))
                                                               [:pdf-table {:width-percent 100 :cell-border false }
                                                                [25 75]
                                                                [[:pdf-cell {:align :right}
                                                                  (if (contains? % :image_file)
                                                                    [:image {:align :center :width 100 :height 100} (str data-dir (:image_file %))] " ")
                                                                  [:spacer 2]]
                                                                 [:pdf-cell
                                                                  (if (contains? % :name )
                                                                    [:heading (:name %)] " ")
                                                                  [:spacer]
                                                                  #_(if (contains? % :type)
                                                                      [:paragraph
                                                                       [:heading (:content %)]] " ")
                                                                  (if (or
                                                                        (contains? % :issuer_content_name) (contains? % :issued_on) (contains? % :description) (contains? % :criteria_url))
                                                                    [:paragraph
                                                                     [:chunk (str (t :badge/Issuedby) ": ")] [:chunk (:issuer_content_name %)] "\n"
                                                                     [:chunk (str (t :badge/Issuedon)": ")] [:chunk (date-from-unix-time (long (* 1000 (:issued_on %))) "date")]
                                                                     [:spacer 1]
                                                                     (:description %) "\n"
                                                                     ;;                                                                  [:chunk (str (t :badge/CriteriaUrl)": " )] [:anchor {:target (:criteria_url %) :style{:family :times-roman :color [66 100 162]}} (:criteria_url %)]
                                                                     [:spacer 0]
                                                                     [:paragraph
                                                                      [:phrase (str (t :badge/Criteria)": ")] [:spacer 0]
                                                                      [:anchor {:target (:criteria_url %) :style{:family :times-roman :color [66 100 162]}} (:criteria_url %)]]] " ")]]
                                                                ] " ")
                                                             (if (= "html" (:type %))
                                                               [:paragraph {:align :center}
                                                                (:content %)] "")
                                                             (if (= "tag" (:type %))
                                                               [:paragraph
                                                                [:chunk ]
                                                                ]
                                                               )

                                                             [:spacer 0]])

                               content (map template $blocks)
                               ]
                           (reduce into [[:paragraph {:align :center} [:heading {:size :15 :align :center} $name][:spacer 0] [:paragraph {:align :center}
                                                                                                                              [:chunk (str $first_name " " $last_name)]]]] content)))]

    (pdf/pdf (into [{:right-margin 50 :left-margin 50 }] (page-template page)) "out")

    ))

(defn page-with-blocks-for-owner [ctx page-id user-id]
  (if (page-owner? ctx page-id user-id)
    (page-with-blocks ctx page-id)))

(defn page-for-edit [ctx page-id user-id]
  (if (page-owner? ctx page-id user-id)
    (let [page (select-keys (select-page {:id page-id} (into {:result-set-fn first} (get-db ctx))) [:id :user_id :name :description])
          blocks (page-blocks-for-edit ctx page-id)
          owner (:user_id page)
          badges (map #(select-keys % [:id :name :image_file :tags]) (b/user-badges-all ctx owner))
          files (map #(select-keys % [:id :name :path :mime_type :size]) (:files (f/user-files-all ctx owner)))
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

(defn url-checker [ctx]
  (fn [element-name attrs]
    (let [whitelist     (get-in ctx [:config :page :iframe-whitelist])
          attr-map (apply hash-map attrs)
          link     (get attr-map "src")]
      (if (and (some (fn [key] (re-find key link)) whitelist) (not-empty link))
        "iframe"))))

(defn sanitize-html [ctx]
  (fn [content]
    (let [policy (html-policy :allow-elements ["a" "big" "blockquote" "br" "caption" "cite" "code" "del" "div"
                                               "em" "h1" "h2" "h3" "hr" "img" "ins" "kbd" "li" "ol" "p" "pre"
                                               "q" "s" "samp" "small" "span" "strong" "table" "tbody" "td" "tfoot"
                                               "th" "thead" "tr" "tt" "ul" "var"]
                              :allow-elements [(url-checker ctx)
                                               "iframe"]
                              :allow-attributes ["align" :on-elements ["table"]]
                              :allow-attributes ["alt" :on-elements ["img"]]
                              :allow-attributes ["border" :on-elements ["table"]]
                              :allow-attributes ["bordercolor" :on-elements ["table"]]
                              :allow-attributes ["cellpadding" :on-elements ["table"]]
                              :allow-attributes ["cellspacing" :on-elements ["table"]]
                              :allow-attributes ["colspan" :on-elements ["td" "th"]]
                              :allow-attributes ["data-cke-realelement" :on-elements ["img"]]
                              :allow-attributes ["dir" :on-elements ["span"]]
                              :allow-attributes ["href" :on-elements ["a"]]
                              :allow-attributes ["id" :on-elements ["a"]]
                              :allow-attributes ["name" :on-elements ["a"]]
                              :allow-attributes ["onclick" :on-elements ["a"]]
                              :allow-attributes ["rowspan" :on-elements ["td" "th"]]
                              :allow-attributes ["scope" :on-elements ["th" "td" "tr"]]
                              :allow-attributes ["src" :on-elements ["img"]]
                              :allow-attributes ["summary" :on-elements ["table"]]
                              :allow-attributes ["target" :on-elements ["a"]]
                              :allow-attributes ["rowspan" :on-elements ["td" "th"]]
                              :allow-attributes ["title" :on-elements ["img"]]
                              :allow-attributes ["allowfullscreen" :on-elements ["iframe"]]
                              :allow-attributes ["src" :on-elements ["iframe"]]
                              :allow-standard-url-protocols
                              :require-rel-nofollow-on-links
                              :allow-styling)]
      (html-sanitize policy content))))


(defn save-page-content! [ctx page-id page-content user-id]

  (try+
    (if-not (page-owner? ctx page-id user-id)
      (throw+ "Page is not owned by current user"))
    (let [{:keys [name description blocks]} page-content
          page-owner-id (page-owner ctx page-id)
          user-files (if (some #(= "file" (:type %)) blocks)
                       (:files (f/user-files-all ctx page-owner-id)))
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
            "html" (let [sanitized-block (update block :content (sanitize-html ctx))]
                     (if id
                       (update-html-block! sanitized-block (get-db ctx))
                       (insert-html-block! sanitized-block (get-db ctx))))
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
      {:status "success" :message "page/Pagesavedsuccessfully"})
    (catch Object _
      {:status "error" :message "page/Errorwhilesavingpage"})))

(defn set-theme! [ctx page-id theme-id border-id padding user-id]
  (try+
    (if-not (page-owner? ctx page-id user-id)
      (throw+ "Page is not owned by current user"))
    (update-page-theme! {:id page-id :theme (valid-theme-id theme-id) :border (valid-border-id border-id) :padding padding} (get-db ctx))
    {:status "success" :message "page/Pagesavedsuccessfully"}
    (catch Object _
      {:status "error" :message "page/Errorwhilesavingpage"})))

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
    (if (and (not= "public" visibility) (b/is-evidence? ctx user-id {:id page-id :resource-type "page"}))
      {:status "error" :message "page/Evidenceerror"}
      (let [password (if (= visibility "password") (trim pword) "")
            page-visibility (if (and (= visibility "password")
                                     (empty? password))
                              "private"
                              visibility)
            evidence-check-fn (first (plugin-fun (get-plugins ctx) "main" "is-evidence?"))
            page-is-evidence? (evidence-check-fn ctx user-id {:id page-id :type ::page})]
        (if (and (private? ctx) (= "public" visibility))
          (throw+ {:status "error" :user-id user-id :message "trying save page visibilty as public in private mode"}) )
        (update-page-visibility-and-password! {:id page-id :visibility page-visibility :password password} (get-db ctx))
        (save-page-tags! ctx page-id tags)
        (if (or (= "internal" visibility) (= "public" visibility))
          (u/event ctx user-id "publish" page-id "page")
          (u/event ctx user-id "unpublish" page-id "page"))
        {:status "success" :message "page/Pagesavedsuccessfully"}))
    (catch Object ex
      (log/error "trying save badge visibilty as public in private mode: " ex)
      {:status "error" :message "page/Errorwhilesavingpage"})))

(defn remove-files-blocks-and-content! [db page-id]
  (let [file-blocks (select-pages-files-blocks {:page_id page-id} db)]
    (doseq [file-block file-blocks]
      (delete-files-block-files! {:block_id (:id file-block)} db))
    (delete-files-blocks! {:page_id page-id} db)))

(defn delete-blocks! [db page-id]
  (delete-heading-blocks! {:page_id page-id} db)
  (delete-badge-blocks! {:page_id page-id} db)
  (delete-html-blocks! {:page_id page-id} db)
  (remove-files-blocks-and-content! db page-id)
  (delete-tag-blocks! {:page_id page-id} db))

(defn delete-page-with-db! [db page-id]
  (delete-blocks! db page-id)
  (delete-page-tags! {:page_id page-id} db)
  (delete-page! {:id page-id} db))

(defn delete-page-by-id! [ctx page-id user-id]
  (when (page-owner? ctx page-id user-id)
    (jdbc/with-db-transaction
      [tr-cn (get-datasource ctx)]
      (delete-page-with-db! {:connection tr-cn} page-id))))

(defn toggle-visibility! [ctx page-id visibility user-id]
  (if (page-owner? ctx page-id user-id)
    (if (and (not= "public" visibility) (b/is-evidence? ctx user-id {:id page-id :resource-type "page"}))
      {:status "error" :message "page/Evidenceerror"}
      (do
        (update-page-visibility! {:id page-id :visibility visibility} (get-db ctx))
        (if (or (= "internal" visibility) (= "public" visibility))
          (u/event ctx user-id "publish" page-id "page")
          (u/event ctx user-id "unpublish" page-id "page"))
        visibility))

    (if (= visibility "public") "private" "public")))

(defn meta-tags [ctx id]
  (let [page (page-with-blocks ctx id)
        image (->> page
                   :blocks
                   (filter #(= "badge" (:type %)))
                   first
                   :image_file)]
    (if (= (:visibility page) "public")
      (-> page
          (select-keys [:name :description])
          (rename-keys {:name :title})
          (assoc :image image)))))
