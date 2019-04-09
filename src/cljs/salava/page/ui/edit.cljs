(ns salava.page.ui.edit
  (:require [reagent.core :refer [atom cursor create-class]]
            [reagent.session :as session]
            [reagent-modals.modals :as m]
            [salava.core.ui.ajax-utils :as ajax]
            [cljs-uuid-utils.core :refer [make-random-uuid uuid-string]]
            [salava.core.ui.layout :as layout]
            [salava.core.ui.helper :refer [navigate-to path-for]]
            [salava.core.ui.field :as f]
            [salava.core.i18n :as i18n :refer [t]]
            [salava.core.helper :refer [dump]]
            [salava.page.ui.helper :as ph]
            [salava.file.ui.my :as file]
            [salava.file.icons :refer [file-icon]]))

(defn random-key []
  (-> (make-random-uuid)
      (uuid-string)))

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
    (path-for (str "/obpv1/page/save_content/" id))
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

(defn send-file [files-atom block-atom]
  (let [file (-> (.querySelector js/document (str "#upload-file-" (:key @block-atom)))
                 .-files
                 (.item 0))
        form-data (doto
                    (js/FormData.)
                    (.append "file" file (.-name file)))]
    (m/modal! (file/upload-modal nil (t :file/Uploadingfile) (t :file/Uploadinprogress)))
    (ajax/POST
      (path-for "/obpv1/file/upload")
      {:body    form-data
       :handler (fn [{:keys [status message reason data]} response]
                  (when (= status "success")
                    (reset! files-atom (conj @files-atom data))
                    (select-file block-atom @files-atom (:id data)))
                  (m/modal! (file/upload-modal status message reason)))})))

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
                 :aria-label "select badge group"
                 :value badge-id
                 :on-change #(select-badge block-atom @badges (js/parseInt (.-target.value %)))}
        [:option {:value 0} (t "-" :page/none "-")]
        (for [badge @badges]
          [:option {:value (:id badge)
                    :key (:id badge)}
           (:name badge)])]]
      [:div.badge-select
       [:select {:class "form-control"
                 :aria-label "select blocktype"
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
                 :aria-label "select badge"
                 :value tag
                 :on-change #(select-tag block-atom @tags (.-target.value %))}
        [:option {:value ""} (t "-" :page/none "-")]
        (for [tag @tags]
          [:option {:value tag :key tag} tag])]]
      [:div.badge-select
       [:select {:class "form-control"
                 :aria-label "select badge format"
                 :value format
                 :on-change #(update-block-value block-atom :format (.-target.value %))}
        [:option {:value "short"} (t :page/Short)]
        [:option {:value "long"} (t :page/Long)]]]
      [:div.badge-select
       [:select {:class "form-control"
                 :aria-label "select sorting"
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
   (into
     [:div.edit-block-files]
     (for [file (:files @block-atom)]
       [:div.row.flip
        [:div.col-xs-7
         [:i {:class (str "page-file-icon fa " (file-icon (:mime_type file)))}]
         [:a {:href (str "/" (:path file))
              :target "_blank"}
          (:name file)]]
        [:div.col-xs-1.remove
         [:span {:class "remove-file-icon"
                 :on-click #(remove-file (cursor block-atom [:files]) file)}
          [:i {:class "fa fa-close"}]]]]))
   [:div.form-group
    [:div.col-xs-8
     [:div.file-select
      [:select {:class "form-control"
                :value ""
                :on-change #(select-file block-atom @files (js/parseInt (.-target.value %)))}
       [:option {:value ""} "- " (t :page/choosefile) " -"]
       (for [file @files]
         [:option {:value (:id file) :key (:id file)} (:name file)])]]]]
   [:div.form-group
    [:div.col-xs-12
     [:button {:class "btn btn-primary upload"
               :on-change #(.preventDefault %)}
      (t :page/oruploadnewfile)]
     [:input {:id        (str "upload-file-" (:key @block-atom))
              :class     "page-file-upload"
              :type      "file"
              :name      "file"
              :on-change #(send-file files block-atom)}]]]])

(defn edit-block-text [block-atom]
  (let [content (:content @block-atom)]
    [:div.form-group
     [:div.col-md-12
      [:input {:class     "form-control"
               :type      "text"
               :value     content
               :on-change #(update-block-value block-atom :content (.-target.value %))}]]]))

(defn save-editor-content [block-atom]
  (if-let [editor (aget js/CKEDITOR "instances" "ckeditor")]
    (swap! block-atom assoc :content (.getData editor)))
  (m/close-modal!))

(defn editor-modal-content [block-atom]
  [:div
   [:div.modal-header
    [:button {:class "close" :type "button" :data-dismiss "modal" :aria-label "OK"}
     [:span {:aria-hidden "true" :dangerouslySetInnerHTML {:__html "&times;"}}]]
    [:h4.modal-title (t :page/Editblockcontent)]]
   [:div.modal-body
    [:textarea {:name "ckeditor"}]]
   [:div.modal-footer
    [:button {:class "btn btn-primary" :type "button" :on-click #(save-editor-content block-atom)}
     (t :core/Save)]]])

(defn editor-modal [block-atom]
  (create-class {:component-did-mount (fn []
                                        (.getScript (js* "$") "/js/ckeditor/modal-fix.js")
                                        (js/CKEDITOR.replace "ckeditor"
                                                             (js-obj "language" (name (session/get-in [:user :language] :en))
                                                                     "filebrowserBrowseUrl" (path-for "/file/browser")))
                                        (.setData (aget js/CKEDITOR "instances" "ckeditor") (:content @block-atom)))
                 :reagent-render      (fn [] (editor-modal-content block-atom))}))

(defn edit-block-html [block-atom]
  [:div.form-group
   [:div.col-md-12
    [:div {:class         (str "html-block-content" (if (:hover @block-atom) " html-block-content-hover"))
           :on-click      #(m/modal! [editor-modal block-atom] {:size :lg})
           :on-mouse-over #(swap! block-atom assoc :hover true)
           :on-mouse-out  #(swap! block-atom assoc :hover false)}
     (if (:hover @block-atom)
       [:i {:class "edit-icon fa fa-pencil-square-o fa-2x"}])
     (if (empty? (:content @block-atom))
       [:div.default-content (t :page/Clickheretoaddsomecontent)]
       (if (re-find #"iframe" (str (:content @block-atom)))
         [:div.embed-responsive.embed-responsive-16by9
          {:dangerouslySetInnerHTML {:__html (:content @block-atom)}}]
         [:div
          {:dangerouslySetInnerHTML {:__html (:content @block-atom)}}]
         ))]]])

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

(def block-type-map
  [{:icon "fa-header" :text (t :page/Heading) :value "heading"}
   {:icon "fa-header" :text (t :page/Subheading) :value "sub-heading"}
   {:icon "fa-file-code-o" :text (t :page/Html) :value "html"}
   {:icon "fa-file" :text (t :page/Files) :value "file"}])

(defn content-type [block-atom index]
  (let [type (:type @block-atom)]
    (fn []
      [:div#block-modal
       [:div.modal-body
        [:p.block-title (t :page/Addblock)]
        [:p "Select a block to add to your page"]
        (reduce-kv
          (fn [r k v]
            (conj r
                  [:a {:on-click #(do
                                    (.preventDefault %)
                                    (if index
                                      (f/add-field block-atom {:type (:value v)} index)
                                      (f/add-field block-atom {:type (:value v)})))
                       :data-dismiss "modal"}
                   [:div.row

                    [:i {:class (str "fa icon " (:icon v))}]
                    [:span (:text v)]]]
                  ))
          [:div.block-types]
          block-type-map)]
       [:div.modal-footer
        [:button.btn.btn-warning {:on-click #(do
                                               (.preventDefault %)
                                               (m/close-modal!)
                                               )}
         (t :core/Cancel)]]])))

(defn open-block-modal [block-atom index]
  (create-class {:reagent-render (fn [] (content-type block-atom index))
                 :component-will-unmount (fn []  (m/close-modal!))}))

(defn block [block-atom index blocks badges tags files]
  (let [{:keys [type]} @block-atom
        first? (= 0 index)
        last? (= (dec (count @blocks)) index)]
    [:div {:key index}
     [:div.add-field-after
      [:button {:class    "btn btn-success"
                :on-click #(do
                             (.preventDefault %)
                             (m/modal! [open-block-modal blocks index] {:size :md})
                             #_(f/add-field blocks {:type "heading"} index))}
       (t :page/Addblock)]]
     [:div.field.thumbnail
      [:div.field-move
       [:div.move-arrows
        (if-not first?
          [:div.move-up {:on-click #(f/move-field :up blocks index)}
           [:i {:class "fa fa-chevron-up"}]])
        (if-not last?
          [:div.move-down {:on-click #(f/move-field :down blocks index)}
           [:i {:class "fa fa-chevron-down"}]])]]
      [:div.field-content
       [:div.form-group
        [:div.col-xs-8
         [block-type block-atom]]
        [:div {:class "col-xs-4 field-remove"
               :on-click #(f/remove-field blocks index)}
         [:span {:class "remove-button" :title (t :page/Delete)}
          [:i {:class "fa fa-trash"}]]]]
       (case type
         ("heading" "sub-heading") [edit-block-text block-atom]
         ("badge") [edit-block-badges block-atom badges]
         ("tag") [edit-block-badge-groups block-atom tags badges]
         ("file") [edit-block-files block-atom files]
         ("html") [edit-block-html block-atom]
         nil)]]
     ]))



(defn page-blocks [blocks badges tags files]
  [:div {:id "field-editor"}
   (into [:div {:id "page-blocks"}]
         (for [index (range (count @blocks))]
           (block (cursor blocks [index]) index blocks badges tags files)))
   [:div.add-field-after
    [:button {:class    "btn btn-success"
              :on-click #(do
                           (.preventDefault %)
                           (m/modal! [open-block-modal blocks nil] {:size :md}))}
     (t :page/Addblock)]]])

#_(defn page-description [description]
    [:div.form-group
     [:label {:class "col-md-2"
              :for "page-description"}
      (t :page/Description)]
     [:div.col-md-10
      [:textarea {:id "page-description"
                  :class "form-control"
                  :value @description
                  :on-change #(reset! description (.-target.value %))}]]])

(defn page-description [description]
  [:div.col-md-12
   [:div.form-group
    [:label {;:class "col-md-2"
              :for "page-description"}
     (t :page/Description)]
    [:div;.col-md-10
     [:textarea {:id "page-description"
                 :class "form-control"
                 :value @description
                 :on-change #(reset! description (.-target.value %))}]]]])

#_(defn page-title [name]
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

(defn page-title [name]
  [:div.col-md-12
   [:div.form-group
    [:label {;:class "col-md-2"
              :for "page-name"}
     (t :page/Title)]
    [:div;.col-md-10
     [:input {:id "page-name"
              :class "form-control"
              :type "text"
              :value @name
              :on-change #(reset! name (.-target.value %))}]]]])

#_(defn page-form [state]
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
                              (save-page (:page @state) (str "/profile/page/edit_theme/" (get-in @state [:page :id]))))}
        (t :page/Save)]]]])

(defn page-form [state]
  [:div
   [:div.panel.thumbnail
    [:div.panel-heading [:p.block-title "Page Information"]]
    [:div.panel-body
     [:form.form-horizontal
      [:div {:id "title-and-description"}

       [page-title (cursor state [:page :name])]
       [page-description (cursor state [:page :description])]]]]]
   [:div.form-horizontal
    [page-blocks (cursor state [:page :blocks]) (cursor state [:badges]) (cursor state [:tags]) (cursor state [:files])]
    [:div.row
     [:div.col-md-12
      [:button {:class    "btn btn-primary"
                :on-click #(do
                             (.preventDefault %)
                             (save-page (:page @state) (str "/profile/page/edit_theme/" (get-in @state [:page :id]))))}
       (t :page/Save)]
      [:button.btn.btn-warning {:on-click #(do
                                             (.preventDefault %)
                                             (navigate-to  "/profile/page"))}
       (t :core/Cancel)]

      [:button.btn.btn-danger {:on-click #(do
                                            (.preventDefault %)
                                            (ph/delete-page (get-in @state [:page :id])))}
       (t :core/Delete)]
      [:div.pull-right {:id "step-button"}
         [:a  "Next"]
         ]]]
    ]])

(defn content [state]
  (let [{:keys [id name]} (:page @state)]
    [:div {:id "page-edit"}
     [m/modal-window]
     [ph/edit-page-header (t :page/Editpage ": " name)]
     [ph/edit-page-buttons id :content (fn [next-url] (save-page (:page @state) next-url))]
     [page-form state]]))

(defn init-data [state id]
  (ajax/GET
    (path-for (str "/obpv1/page/edit/" id) true)
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

