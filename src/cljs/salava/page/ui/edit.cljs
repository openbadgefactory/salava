(ns salava.page.ui.edit
  (:require [reagent.core :refer [atom cursor create-class]]
            [salava.core.ui.ajax-utils :as ajax]
            [cljs-uuid-utils.core :refer [make-random-uuid uuid-string]]
            [salava.core.ui.layout :as layout]
            [salava.core.ui.helper :refer [navigate-to]]
            [salava.core.i18n :as i18n :refer [t]]
            [salava.core.helper :refer [dump]]
            [salava.page.ui.helper :as ph]
            [salava.file.icons :refer [file-icon]]))

(defn random-key []
  (-> (make-random-uuid)
      (uuid-string)))

(def simplemde-toolbar
  (array "bold" "italic" "strikethrough" "|"
         "heading-1" "heading-2" "heading-3" "|"
         "quote" "unordered-list" "ordered-list" "|"
         "link" "image" "horizontal-rule" "|"
         "preview" "side-by-side" "fullscreen"))

(defn block-specific-values [{:keys [type content badge tag format sort files]}]
  (case type
    "heading" {:type "heading" :size "h1" :content content}
    "sub-heading" {:type "heading" :size "h2":content content}
    "badge" {:format (or format "short") :badge_id (:id badge 0)}
    "html" {:content content}
    "file" {:files (map :id files)}
    "tag" {:tag tag :format (or format "short") :sort (or sort "name")}
    nil))

(defn prepare-blocks-to-save [blocks]
  (for [block blocks]
    (-> block
        (select-keys [:id :type])
        (merge (block-specific-values block)))))

(defn save-page [{:keys [id name description blocks]} next-url]
  (ajax/POST
    (str "/obpv1/page/save_content/" id)
    {:params {:name name
              :description description
              :blocks (prepare-blocks-to-save blocks)}
     :handler (fn [] (navigate-to next-url))}))

(defn update-block-value [block-atom key value]
  (swap! block-atom assoc key value))

(defn select-badge [block-atom badges id]
  (let [badge (some #(if (= (:id %) id) %) badges)]
    (update-block-value block-atom :badge badge)))

(defn select-tag [block-atom tags value]
  (let [tag (some #(if (= % value) %) tags)]
    (update-block-value block-atom :tag tag)))

(defn select-file [block-atom files id]
  (let [file (some #(if (= (:id %) id) %) files)]
    (if file
      (update-block-value block-atom :files (conj (vec (:files @block-atom)) (assoc file :key (random-key)))))))

(defn remove-file [files-atom file]
  (reset! files-atom (vec (remove #(= % file) @files-atom))))

(defn edit-block-badges [block-atom badges]
  (let [badge-id (get-in @block-atom [:badge :id] 0)
        image (get-in @block-atom [:badge :image_file])
        format (:format @block-atom)]
    [:div.form-group
     [:div.col-xs-8
      [:div.badge-select
       [:select {:class "form-control"
                 :value badge-id
                 :on-change #(select-badge block-atom @badges (js/parseInt (.-target.value %)))}
        [:option {:value 0} (t "-" :page/none "-")]
        (for [badge @badges]
          [:option {:value (:id badge)
                    :key (:id badge)}
           (:name badge)])]]
      [:div.badge-select
       [:select {:class "form-control"
                 :value format
                 :on-change #(update-block-value block-atom :format (.-target.value %))}
        [:option {:value "short"} (t :page/Short)]
        [:option {:value "long"} (t :page/Long)]]]]
     [:div {:class "col-xs-4 badge-image"}
      (if image
        [:img {:src (str "/" image)}])]]))

(defn edit-block-badge-groups [block-atom tags badges]
  (let [tag (get-in @block-atom [:tag] "")
        format (get-in @block-atom [:format] "short")
        sort-by (get-in @block-atom [:sort] "name")
        tagged-badges (->> @badges
                         (filter #(some (fn [t] (= t tag)) (:tags %))))]
    [:div.form-group
     [:div.col-xs-8
      [:div.badge-select
       [:select {:class "form-control"
                 :value tag
                 :on-change #(select-tag block-atom @tags (.-target.value %))}
        [:option {:value ""} (t "-" :page/none "-")]
        (for [tag @tags]
          [:option {:value tag :key tag} tag])]]
      [:div.badge-select
       [:select {:class "form-control"
                 :value format
                 :on-change #(update-block-value block-atom :format (.-target.value %))}
        [:option {:value "short"} (t :page/Short)]
        [:option {:value "long"} (t :page/Long)]]]
      [:div.badge-select
       [:select {:class "form-control"
                 :value sort-by
                 :on-change #(update-block-value block-atom :sort (.-target.value %))}
        [:option {:value "name"} (t :page/Byname)]
        [:option {:value "modified"} (t :page/Bydatemodified)]]]]
     [:div {:class "col-xs-4 badge-image"}
      (if tagged-badges
        (for [badge tagged-badges]
          [:img {:src (str "/" (:image_file badge))
                 :key (:name badge)}]))]]))

(defn edit-block-files [block-atom files]
  [:div
   [:div.edit-block-files
    (for [file (:files @block-atom)]
      [:div.row
       [:div.col-xs-6
        [:i {:class (str "page-file-icon fa " (file-icon (:mime_type file)))}]
        [:a {:href (str "/" (:path file))
             :target "_blank"}
         (:name file)]]
       [:div.col-xs-6
        [:span {:class "remove-file-icon"
                :on-click #(remove-file (cursor block-atom [:files]) file)}
         [:i {:class "fa fa-close"}]]]])]
   [:div.form-group
    [:div.col-xs-12
     [:div.file-select
      [:select {:class "form-control"
                :value ""
                :on-change #(select-file block-atom @files (js/parseInt (.-target.value %)))}
       [:option {:value ""} (t "-" :page/none "-")]
       (for [file @files]
         [:option {:value (:id file) :key (:id file)} (:name file)])]]]]])

(defn edit-block-text [block-atom]
  (let [content (:content @block-atom)]
    [:div.form-group
     [:div.col-md-12
      [:input {:class     "form-control"
              :type      "text"
              :value     content
              :on-change #(update-block-value block-atom :content (.-target.value %))}]]]))

(defn init-editor [block-atom element-id]
  (let [editor (js/SimpleMDE. (js-obj "element" (.getElementById js/document element-id)
                                      "toolbar" simplemde-toolbar))]
    (.value editor (get @block-atom :content ""))
    (.codemirror.on editor "change" (fn []
                                      (update-block-value block-atom :content (.value editor))))))

(defn edit-block-html [block-atom]
  (let [element-id (str "editor_" (:key @block-atom))]
    (create-class {:component-did-mount #(init-editor block-atom element-id)
                   :reagent-render       (fn []
                                          [:div.form-group
                                           [:div.col-md-12
                                            [:textarea {:class "form-control"
                                                        :id    element-id}]]])})))

(defn remove-block [blocks block-atom]
  (reset! blocks (vec (remove #(= % @block-atom) @blocks))))

(defn block-type [block-atom]
  (let [type (:type @block-atom)]
    [:div
     [:select {:class "form-control"
               :value type
               :on-change #(update-block-value block-atom :type (.-target.value %))}
      [:option {:value "heading"} (t :page/Heading)]
      [:option {:value "sub-heading"} (t :page/Subheading)]
      [:option {:value "badge"} (t :page/Badge)]
      [:option {:value "tag"} (t :page/Badgegroup)]
      [:option {:value "html"} (t :page/Html)]
      [:option {:value "file"} (t :page/Files)]]]))

(defn create-new-block
  ([blocks] (create-new-block blocks (count @blocks)))
  ([blocks index]
   (let [new-block {:type "heading" :key (random-key)}
         [before-blocks after-blocks] (split-at index @blocks)]
     (reset! blocks (vec (concat before-blocks [new-block] after-blocks))))))

(defn move-block [direction blocks block]
  (let [old-position (.indexOf (to-array @blocks) @block)
        new-position (cond
                       (= :down direction) (if-not (= (inc old-position) (count @blocks))
                                           (inc old-position))
                       (= :up direction) (if-not (= (dec old-position) -1)
                                             (dec old-position)))]
    (if new-position
      (swap! blocks assoc old-position (nth @blocks new-position)
             new-position (nth @blocks old-position)))))

(defn block [block-atom index blocks badges tags files]
  (let [{:keys [type]} @block-atom
        first? (= 0 index)
        last? (= (dec (count @blocks)) index)]
    [:div {:key index}
     [:div.add-block-after
      [:button {:class    "btn btn-success"
                :on-click #(do
                            (.preventDefault %)
                            (create-new-block blocks index))}
       (t :page/Addblock)]]
     [:div.block
      [:div.block-move
       [:div.move-arrows
        (if-not first?
          [:div.move-up {:on-click #(move-block :up blocks block-atom)}
           [:i {:class "fa fa-chevron-up"}]])
        (if-not last?
          [:div.move-down {:on-click #(move-block :down blocks block-atom)}
           [:i {:class "fa fa-chevron-down"}]])]]
      [:div.block-content
       [:div.form-group
        [:div.col-xs-8
         [block-type block-atom]]
        [:div {:class "col-xs-4 block-remove"
               :on-click #(remove-block blocks block-atom)}
         [:span {:class "remove-button"}
          [:i {:class "fa fa-close"}]]]]
       (case type
         ("heading" "sub-heading") [edit-block-text block-atom]
         ("badge") [edit-block-badges block-atom badges]
         ("tag") [edit-block-badge-groups block-atom tags badges]
         ("file") [edit-block-files block-atom files]
         ("html") [edit-block-html block-atom]
         nil)]]
     ]))

(defn page-blocks [blocks badges tags files]
  [:div
   (into [:div {:id "page-blocks"}]
         (for [index (range (count @blocks))]
           (block (cursor blocks [index]) index blocks badges tags files)))
   [:div.add-block-after
    [:button {:class    "btn btn-success"
              :on-click #(do
                          (.preventDefault %)
                          (create-new-block blocks))}
     (t :page/Addblock)]]])

(defn page-description [description]
  [:div.form-group
   [:label {:class "col-md-2"
            :for "page-description"}
    (t :page/Description)]
   [:div.col-md-10
    [:textarea {:id "page-description"
                :class "form-control"
                :value @description
                :on-change #(reset! description (.-target.value %))}]]])

(defn page-title [name]
  [:div.form-group
   [:label {:class "col-md-2"
            :for "page-name"}
     (t :page/Title)]
   [:div.col-md-10
    [:input {:id "page-name"
             :class "form-control"
             :type "text"
             :value @name
             :on-change #(reset! name (.-target.value %))}]]])

(defn page-form [state]
  [:form.form-horizontal
   [:div {:id "title-and-description"}
    [page-title (cursor state [:page :name])]
    [page-description (cursor state [:page :description])]]
   [page-blocks (cursor state [:page :blocks]) (cursor state [:badges]) (cursor state [:tags]) (cursor state [:files])]
   [:div.row
    [:div.col-md-12
     [:button {:class    "btn btn-primary"
               :on-click #(do
                           (.preventDefault %)
                           (save-page (:page @state) (str "/page/edit_theme/" (get-in @state [:page :id]))))}
      (t :page/Save)]]]])

(defn content [state]
  (let [{:keys [id name]} (:page @state)]
    [:div {:id "page-edit"}
     [ph/edit-page-header (t :page/Editpage ": " name)]
     [ph/edit-page-buttons id :content (fn [next-url] (save-page (:page @state) next-url))]
     [page-form state]]))

(defn init-data [state id]
  (ajax/GET
    (str "/obpv1/page/edit/" id "?_=" (.now js/Date))
    {:handler (fn [data]
                (let [data-with-uuids (assoc-in data [:page :blocks] (vec (map #(assoc % :key (random-key))
                                                                               (get-in data [:page :blocks]))))]
                  (reset! state data-with-uuids)))}))

(defn handler [site-navi params]
  (let [id (:page-id params)
        state (atom {:page {:blocks []
                            :name ""
                            :description ""
                            :id id}
                     :badges []
                     :tags []})]

    (init-data state id)
    (fn []
      (layout/default site-navi (content state)))))
