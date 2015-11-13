(ns salava.page.ui.helper
  (:require [reagent-modals.modals :as m]
            [markdown.core :refer [md->html]]
            [ajax.core :as ajax]
            [salava.core.i18n :refer [t]]
            [salava.badge.ui.helper :as bh]
            [salava.core.time :refer [date-from-unix-time]]
            [salava.core.helper :refer [dump]]
            [salava.file.icons :refer [file-icon]]))

(defn delete-page [id]
  (ajax/DELETE
    (str "/obpv1/page/" id)
    {:handler (fn []
                (.replace js/window.location "/page"))}))

(defn delete-page-modal [page-id]
  [:div
   [:div.modal-header
    [:button {:type "button"
              :class "close"
              :data-dismiss "modal"
              :aria-label "OK"}
     [:span {:aria-hidden "true"
             :dangerouslySetInnerHTML {:__html "&times;"}}]]]
   [:div.modal-body
    [:div {:class (str "alert alert-warning")}
    (t :page/Deleteconfirm)]]
   [:div.modal-footer
    [:button {:type "button"
              :class "btn btn-primary"
              :data-dismiss "modal"}
     (t :page/Cancel)]
    [:button {:type "button"
              :class "btn btn-warning"
              :on-click #(delete-page page-id)}
     (t :page/Delete)]]])

(defn badge-block [{:keys [format image_file name description issued_on criteria_url criteria_markdown issuer_name issuer_url issuer_email]} ]
  [:div {:class "row badge-block"}
   [:div {:class "col-md-4 badge-image"}
    [:img {:src (str "/" image_file)}]]
   [:div {:class "col-md-8"}
    [:div.row
     [:div.col-md-12
      [:h3 name]]]
    [:div.row
     [:div.col-md-12
      (bh/issued-on issued_on)]]
    [:div.row
     [:div.col-md-12
      (bh/issuer-label-and-link issuer_name issuer_url issuer_email)]]
    [:div.row
     [:div.col-md-12 description]]
    [:div.row
     [:div.col-md-12
      [:h3 (t :badge/Criteria)]]]
    [:div.row
     [:div.col-md-12
      [:a {:href criteria_url} (t :badge/Opencriteriapage)]]]
    (if (= format "long")
      [:div.row
       [:div {:class "col-md-12"
              :dangerouslySetInnerHTML {:__html (md->html criteria_markdown)}}]])]])

(defn html-block [{:keys [content]}]
  [:div.html-block
   {:dangerouslySetInnerHTML {:__html content}}])

(defn file-block [{:keys [files]}]
  [:div.file-block
   [:div.row
    [:div.col-md-12
     [:label.files-label
      (t :page/Attachments) ": "]
     (for [file files]
       [:span.attachment
        [:i {:class (str "page-file-icon fa " (file-icon (:mime_type file)))}]
        [:a.file-link {:href (str "/" (:path file))
                       :target "_blank"}
         (:name file)]])]]])

(defn heading-block [{:keys [size content]}]
  [:div.heading-block
   (case size
     "h1" [:h1 content]
     "h2" [:h2 content]
     nil)])

(defn tag-block [{:keys [tag badges format sort]}]
  [:div.tag-block
   [:div
    [:label (str (t :page/Tag) ":")] (str " " tag)]
   (let [sorted-badges (case sort
                         "name" (sort-by :name < badges)
                         "modified" (sort-by :mtime > badges)
                         badges)]
     (for [badge sorted-badges]
       (if (= format "short")
         [:a.small-badge-image {:href (str "/badge/info/" (:id badge))
                                :key (:id badge)}
          [:img {:src (str "/" (:image_file badge))
                 :title (:name badge)}]]
         (badge-block (assoc badge :format "long")))))])

(defn view-page [page]
  (let [{:keys [id name description mtime first_name last_name blocks theme]} page]
    [:div {:id    (str "theme-" (or theme 0))
           :class "page-content"}
     [:div.panel
      [:div.panel-left
       [:div.panel-right
        [:div.panel-content
         [:div.row
          [:div {:class "col-md-12 page-mtime"}
           (date-from-unix-time (* 1000 mtime))]]
         [:div.row
          [:div {:class "col-md-12 page-title"}
           [:h1 name]]]
         [:div.row
          [:div {:class "col-md-12 page-summary"}
           description]]
         [:div.row
          [:div {:class "col-md-12 page-author"}
           [:a {:href "#"}
            (str first_name " " last_name)]]]
         (into [:div.page-blocks]
               (for [block blocks]
                 (case (:type block)
                   "badge" (badge-block block)
                   "html" (html-block block)
                   "file" (file-block block)
                   "heading" (heading-block block)
                   "tag" (tag-block block)
                   nil)))]]]]]))

(defn edit-page-header [header]
  [:div.row
   [:div.col-sm-12
    [:h1 header]]])

(defn edit-page-buttons [id target]
  [:div {:class "row"
         :id "buttons"}
   [:div.col-xs-8
    [:a {:class (str "btn" (if (= target :content) " btn-active"))
         :href (str "/page/edit/" id)}
     (str "1." (t :page/Content))]
    [:a {:class (str "btn" (if (= target :theme) " btn-active"))
         :href (str "/page/edit_theme/" id)}
     (str "2." (t :page/Theme))]
    [:a {:class (str "btn" (if (= target :settings) " btn-active"))
         :href (str "/page/settings/" id)}
     (str "3." (t :page/Settings))]
    [:a {:class (str "btn" (if (= target :preview) " btn-active"))
         :href (str "/page/preview/" id)}
     (str "4." (t :page/Preview))]]
   [:div {:class "col-xs-4"
          :id "buttons-right"}
    [:a {:class "btn btn-primary"
         :href (str "/page/view/" id)}
     (t :page/View)]
    [:a {:class "btn btn-warning"
         :on-click #(m/modal! (delete-page-modal id))}
     (t :page/Delete)]]
   [m/modal-window]])