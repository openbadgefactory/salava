(ns salava.page.ui.edit
  (:require [reagent.core :refer [atom cursor create-class]]
            [reagent.session :as session]
            [reagent-modals.modals :as m]
            [salava.core.ui.ajax-utils :as ajax]
            [cljs-uuid-utils.core :refer [make-random-uuid uuid-string]]
            [salava.core.ui.layout :as layout]
            [salava.core.ui.helper :refer [navigate-to path-for plugin-fun]]
            [salava.core.ui.field :as f]
            [salava.core.i18n :as i18n :refer [t]]
            [salava.core.helper :refer [dump]]
            [salava.page.ui.helper :as ph]
            [salava.file.ui.my :as file]
            [salava.file.icons :refer [file-icon]]
            [clojure.string :refer [capitalize]]
            [salava.core.ui.modal :refer [open-modal]]
            [salava.core.ui.popover :refer [info]]
            [salava.core.ui.badge-grid :refer [badge-grid-element]]))

(defn random-key []
  (-> (make-random-uuid)
      (uuid-string)))

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
  (let [badge-id (get-in @block-atom [:badge :id] (session/get-in! [:badge-block :badge :id] 0))
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

(defn badge-block [block-atom]
  (let [badge-id (get-in @block-atom [:badge :id] (session/get-in! [:badge-block :badge :id] 0))
        image (get-in @block-atom [:badge :image_file])
        name (get-in @block-atom [:badge :name] "")
        format (:format @block-atom)
        description (get-in @block-atom [:badge :description])]
    [:div.badge-select
     [:div.row.flip
      [:div.col-md-3.badge-image
       [:img.badge-image {:src (str "/" image)}]]

      [:div.col-md-9
       [:h4.media-heading name]

       [:div description]]]]))




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
        [:option {:value "short"} (t :page/ImageandName)]
        [:option {:value "long"} (t :page/Content)]]]
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
          {:dangerouslySetInnerHTML {:__html (:content @block-atom)}}]))]]])


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
   {:icon "fa-file-code-o" :text (t :page/Texteditor) :value "html"}
   {:icon "fa-file" :text (t :page/Files) :value "file"}
   {:icon "fa-certificate" :text (t :page/Badge) :value "badge"}
   {:icon "fa-tags" :text (t :page/Badgegroup) :value "tag"}
   {:icon "fa-certificate" :icon-2 "fa-th-large" :text (t :page/Badgeshowcase) :value "showcase"}
   {:icon "fa-user" :text "Profile information" :value "profile"}])

(defn badge-showcase [state block-atom]
  (let [badges (if (seq (:badges @block-atom)) (:badges @block-atom) [])
        new-field-atom (atom {:type "showcase" :badges badges})
        title (:title @block-atom)
        format (:format @block-atom)]
    [:div#badge-showcase
     [:div
      [:div;.form-group
       [:div;.col-md-12
        [:label (t :page/Title)]

        [:input {:class     "form-control"
                 :type      "text"
                 :value     title
                 :default-value (t :page/Untitled)
                 :on-change #(update-block-value block-atom :title (.-target.value %))
                 :placeholder (t :page/Untitled)}]]
       [:div;.col-md-12
        [:label (t :page/Displayinpageas)]
        [:div.badge-select
         [:select {:class "form-control"
                   :aria-label "select badge format"
                   :value (or format "short")
                   :on-change #(update-block-value block-atom :format (.-target.value %))}
          [:option {:value "short"} (t :page/ImageandName)]
          [:option {:value "long"} (t :page/Content)]]]]]
      [:div#grid {:class "row wrap-grid"}
       (reduce (fn [r b]
                 (conj r
                       (badge-grid-element b block-atom "showcase" {:delete! (fn [id badges] (update-block-value block-atom :badges (into [] (remove #(= id (:id %)) badges))))
                                                                    :swap! (fn [index data badges] (update-block-value block-atom :badges (into [] (assoc badges index data))))})))
               [:div]
               badges)
       [:div.addbadge
        [:a {:href "#" :on-click #(do
                                    (.preventDefault %)
                                    (open-modal [:badge :my] {:type "pickable" :block-atom block-atom :new-field-atom new-field-atom
                                                              :function (fn [f] (update-block-value block-atom :badges (conj badges f)))}))}
         [:i.fa.fa-plus.fa-5x.add-icon]]]]]]))

(defn profile-block [block-atom]
  (let [block (first (plugin-fun (session/get :plugins) "block" "editprofileinfo"))]
    (if block (block block-atom) [:div ""])))

(defn contenttype [{:keys [block-atom index]}]
  (let [block-type-map (if (some #(= "profile" (:type %)) @block-atom)
                         (into []  (remove #(= "profile" (:value %)) block-type-map)) block-type-map)]
    (fn []
      [:div#page-edit
       [:div#block-modal
        [:div.modal-body
         [:p.block-title (t :page/Addblock)]
         [:p (t :page/Choosecontent)]
         (reduce-kv
           (fn [r k v]
             (let [new-field-atom (atom {:type (:value v)})]
               (conj r
                     [:div.row
                      [:div.col-md-12
                       [:div.content-type {:style {:display "inline-table"}} [:a.link {:on-click #(do
                                                                                                    (.preventDefault %)
                                                                                                    (case (:value v)
                                                                                                      "badge" (open-modal [:badge :my] {:type "pickable" :new-field-atom new-field-atom  :block-atom block-atom  :index (or index nil)})
                                                                                                      (if index
                                                                                                        (f/add-field block-atom {:type (:value v)} index)
                                                                                                        (f/add-field block-atom {:type (:value v)}))))
                                                                                       :data-dismiss (case (:value v)
                                                                                                       ("badge") nil
                                                                                                       "modal")}

                                                                              [:div

                                                                               [:i.fa.fa-fw {:class (:icon v)}]
                                                                               (when (:icon-2 v) [:i.fa.fa-fw {:class (:icon-2 v)}])

                                                                               [:span (:text v)]]]]
                       [:span {:style {:display "inline"}}
                        [info {:placement "right" :content (case (:value v)
                                                             "badge" (t :page/Badgeinfo)
                                                             "tag" (t :page/Badgegroupinfo)
                                                             "heading" (t :page/Headinginfo)
                                                             "sub-heading" (t :page/Subheadinginfo)
                                                             "file" (t :page/Filesinfo)
                                                             "html" (t :page/Htmlinfo)
                                                             "showcase" (t :page/Badgeshowcaseinfo)
                                                             "profile" (t :profile/Addprofileinfo))

                               :style {:font-size "15px"}}]]]])))

           [:div.block-types] block-type-map)]
        [:div.modal-footer
         [:button.btn.btn-warning {:on-click #(do
                                                (.preventDefault %)
                                                (m/close-modal!))}

          (t :core/Cancel)]]]])))



(defn field-after [blocks state index initial?]
  (let [ first? (= 0 index)
         last? (= (dec (count @blocks)) index)
         block-count (count @blocks)]
    (fn []
      [:div.add-field-after

       (if (and (:toggle-move-mode @state) (not (= index (:toggled @state))))
         [:a {:href "#" :on-click #(do
                                     (f/move-field-drop blocks (:toggled @state) index)
                                     (swap! state assoc :toggle-move-mode false :toggled nil))}
          [:div.placeholder.html-block-content.html-block-content-hover
           (t :page/Clicktodrop)]]
         [:button {:class    "btn btn-success"
                   :on-click #(do
                                (.preventDefault %)
                                (if index (open-modal [:page :blocktype] {:block-atom blocks :index index} {:size :md}) (open-modal [:page :blocktype] {:block-atom blocks :index nil})))}
          (t :page/Addblock)])])))

(defn block [block-atom index blocks badges tags files state]
  (let [{:keys [type]} @block-atom
        first? (= 0 index)
        last? (= (dec (count @blocks)) index)
        block-toggled? (and (:toggle-move-mode @state) (= (:toggled @state) index))]
    [:div {:key index}
     [field-after blocks state index]
     [:div.field.thumbnail {:class (when block-toggled? " block-to-move")}
      [:div.field-content
       [:div.form-group
        [:div.col-xs-8
         [:span.block-title (some-> (filter #(= type (:value %)) block-type-map) first :text capitalize)]

         (when (= type "badge")
          [:div.row.form-group {:style {:padding-top "10px"}}
           [:div.col-xs-8 [:select {:class "form-control"
                                    :aria-label "select blocktype"
                                    :value (:format @block-atom)
                                    :on-change #(update-block-value block-atom :format (.-target.value %))}
                           [:option {:value "short"} (t :page/Short)]
                           [:option {:value "long"} (t :page/Long)]]]
           [:div.col-xs-4
            [info {:content (t :page/Badgeformatinfo) :placement "left"}]]])]

        [:div.move {:on-click #(do
                                 (.preventDefault %)
                                 (cond
                                   (and first? last?) (swap! state assoc :toggle-move-mode false :toggled nil)
                                   (:toggle-move-mode @state) (swap! state assoc :toggle-move-mode false :toggled nil)
                                   :else (swap! state assoc :toggle-move-mode true :toggled index)))}
         [:span.move-block {:class (when block-toggled? " block-to-move")}  [:i.fa.fa-arrows]]]
        [:div {:class "close-button"
               :on-click #(f/remove-field blocks index)}
         [:span {:class "remove-button" :title (t :page/Delete)}
          [:i {:class "fa fa-trash"}]]]]
       (case type
         ("heading" "sub-heading") [edit-block-text block-atom]
         ("badge") [badge-block block-atom]#_[edit-block-badges block-atom badges]
         ("tag") [edit-block-badge-groups block-atom tags badges]
         ("file") [edit-block-files block-atom files]
         ("html") [edit-block-html block-atom]
         ("showcase") [badge-showcase state block-atom]
         ("profile") [profile-block block-atom]
         nil)]]]))



(defn page-blocks [blocks badges tags files state]
  (let [block-count (count @blocks)
        position (if (pos? block-count) (dec block-count) nil)]

    [:div {:id "field-editor"}
     (into [:div {:id "page-blocks"}]
           (for [index (range (count @blocks))]
             (block (cursor blocks [index]) index blocks badges tags files state)))
     [field-after blocks state position]]))


(defn page-description [description]
  [:div;.col-md-12
   [:div;.form-group
    [:label {;:class "col-md-2"
              :for "page-description"}
     (t :page/Description)]
    [:div;.col-md-10
     [:textarea {:id "page-description"
                 :class "form-control"
                 :value @description
                 :on-change #(reset! description (.-target.value %))}]]]])

(defn page-title [name]
  [:div;.col-md-12
   [:div;.form-group
    [:label {;:class "col-md-2"
              :for "page-name"}
     (t :page/Title)]
    [:div;.col-md-10
     [:input {:id "page-name"
              :class "form-control"
              :type "text"
              :value @name
              :on-change #(reset! name (.-target.value %))}]]]])

(defn page-form [state]
  [:div
   [:div.panel.thumbnail
    [:div.panel-heading [:p.block-title "Page Information"]]
    [:div.panel-body
     [:div.form-horizontal
      [:div {:id "title-and-description"}

       [page-title (cursor state [:page :name])]
       [page-description (cursor state [:page :description])]]]]]
   [ph/manage-page-buttons :content (cursor state [:page :id]) state]
   [:div.form-horizontal
    [page-blocks (cursor state [:page :blocks]) (cursor state [:badges]) (cursor state [:tags]) (cursor state [:files]) state]]])


(defn content [state]
  (let [{:keys [id name]} (:page @state)]

    [:div {:id "page-edit"}
     [m/modal-window]
     [ph/edit-page-header (t :page/Editpage ": " name)]
     [ph/edit-page-buttons id :content state]
     [page-form state]]))

(defn init-data [state id]
  (ajax/GET
    (path-for (str "/obpv1/page/edit/" id) true)
    {:handler (fn [data]
               (let [data-with-uuids (assoc-in data [:page :blocks] (vec (map #(assoc % :key (random-key))
                                                                              (get-in data [:page :blocks]))))]
                 (reset! state (assoc data-with-uuids :toggle-move-mode false))))}))



(defn handler [site-navi params]
  (let [id (:page-id params)
        state (atom {:page {:blocks []
                            :name ""
                            :description ""
                            :id id}
                     :badges []
                     :tags []
                     :toggle-move-mode false
                     :profile-tab? nil})]
    (init-data state id)
    (fn []
      (layout/default site-navi (content state)))))
