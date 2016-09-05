(ns salava.page.ui.helper
  (:require [reagent.core :refer [create-class]]
            [reagent-modals.modals :as m]
            [markdown.core :refer [md->html]]
            [salava.core.ui.ajax-utils :as ajax]
            [salava.core.i18n :refer [t]]
            [salava.core.ui.helper :refer [navigate-to path-for]]
            [salava.badge.ui.helper :as bh]
            [salava.core.time :refer [date-from-unix-time]]
            [salava.file.icons :refer [file-icon]]
            [salava.admin.ui.reporttool :refer [reporttool]]
            ))

(defn delete-page [id]
  (ajax/DELETE
    (path-for (str "/obpv1/page/" id))
    {:handler (fn [] (navigate-to "/page"))}))

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

(defn badge-block [{:keys [format image_file name description issuer_image issued_on issuer_contact criteria_url criteria_markdown issuer_content_name issuer_content_url issuer_email issuer_description html_content creator_name creator_url creator_email creator_image creator_description]}]
  [:div {:class "row badge-block"}
   [:div {:class "col-md-4 badge-image"}
    [:img {:src (str "/" image_file)}]]
   [:div {:class "col-md-8"}
    [:div.row
     [:div.col-md-12
      [:h3.badge-name name]]]
    [:div.row
     [:div
      (bh/issuer-image issuer_image)]]
     [:div.row
     [:div.col-md-12
      (bh/issued-on issued_on)]]
     [:div.row
      [:div.col-md-12
       (bh/issuer-label-image-link issuer_content_name issuer_content_url issuer_contact issuer_image)
       
       (bh/creator-label-image-link creator_name creator_url creator_email creator_image)
       ]]

    [:div.row
     [:div {:class "col-md-12 description"} description]]
    [:div.row
     [:div.col-md-12
      [:h3.criteria (t :badge/Criteria)]]]
    [:div.row
     [:div.col-md-12
      [:a {:href criteria_url :target "_blank"} (t :badge/Opencriteriapage)]]]
    [:div {:class "row criteria-html"}
     [:div.col-md-12
      {:dangerouslySetInnerHTML {:__html html_content}}]]
    (if (= format "long")
      [:div.row
       [:div {:class "col-md-12"
              :dangerouslySetInnerHTML {:__html (md->html criteria_markdown)}}]])]])

(defn html-block [{:keys [content]}]
  [:div.html-block
   (if (re-find #"iframe" (str content))
     [:div.embed-responsive.embed-responsive-16by9
      {:dangerouslySetInnerHTML {:__html (md->html content)}}]
     [:div
      {:dangerouslySetInnerHTML {:__html (md->html content)}}]
     )])


(defn file-block [{:keys [files]}]
  [:div.file-block
   [:div.row
    [:div.col-md-12
     (if (every? #(re-find #"image/" (str (:mime_type %))) files)
       (into [:div.file-block-images]
             (for [file files]
               [:div.file-block-image
                [:img {:src (str "/" (:path file))}]]))
       [:div.file-block-attachments
        [:label.files-label
         (t :page/Attachments) ": "]
        (into [:div]
              (for [file files]
                [:span.attachment
                 [:i {:class (str "page-file-icon fa " (file-icon (:mime_type file)))}]
                 [:a.file-link {:href (str "/" (:path file))
                                :target "_blank"}
                  (:name file)]]))])]]])

(defn heading-block [{:keys [size content]}]
  [:div.heading-block
   (case size
     "h1" [:h1 content]
     "h2" [:h2 content]
     nil)])

(defn tag-block [{:keys [tag badges format sort]}]
  [:div.tag-block
   [:div
    [:label (t :page/Tag ":")] (str " " tag)]
   (let [sorted-badges (case sort
                         "name" (sort-by :name < badges)
                         "modified" (sort-by :mtime > badges)
                         badges)]
     (for [badge sorted-badges]
       (if (= format "short")
         [:a.small-badge-image {:href (path-for (str "/badge/info/" (:id badge)))
                                :key  (:id badge)}
          [:img {:src (str "/" (:image_file badge))
                 :title (:name badge)}]]
         (badge-block (assoc badge :format "long")))))])

(defn view-page [page]
  (let [{:keys [id name description mtime user_id first_name last_name blocks theme border padding visibility qr_code]} page]
    [:div {:id    (str "theme-" (or theme 0))
           :class "page-content"}
     (if id
       [:div.panel
        [:div.panel-left
         [:div.panel-right
          [:div.panel-content
           (if (and qr_code (= visibility "public"))
             [:div.row
              [:div {:class "col-xs-12 text-center"}
               [:img#print-qr-code {:src (str "data:image/png;base64," qr_code)}]]])
           (if mtime
             [:div.row
              [:div {:class "col-md-12 page-mtime"}
               (date-from-unix-time (* 1000 mtime))]])
           [:div.row
            [:div {:class "col-md-12 page-title"}
             [:h1 name]]]
           [:div.row
            [:div {:class "col-md-12 page-author"}
             [:a {:href (path-for (str "/user/profile/" user_id))} (str first_name " " last_name)]]]
           [:div.row
            [:div {:class "col-md-12 page-summary"}
             description]]
           (into [:div.page-blocks]
                 (for [block blocks]
                   [:div {:class "block-wrapper"
                          :style {:border-top-width (:width border)
                                  :border-top-style (:style border)
                                  :border-top-color (:color border)
                                  :padding-top (str padding "px")
                                  :margin-top (str padding "px")}}
                    (case (:type block)
                      "badge" (badge-block block)
                      "html" (html-block block)
                      "file" (file-block block)
                      "heading" (heading-block block)
                      "tag" (tag-block block)
                      nil)]))]]]])]))

(defn render-page-modal [page reporttool-atom]
  [:div {:id "badge-content"}
   [:div.modal-body
    [:div.row
     [:div.col-md-12
      [:button {:type         "button"
                :class        "close"
                :data-dismiss "modal"
                :aria-label   "OK"}
       [:span {:aria-hidden             "true"
               :dangerouslySetInnerHTML {:__html "&times;"}}]]]]]
   [view-page page]
   [:div {:class "modal-footer page-content"}
   (reporttool (:id page) (:name page) "page" reporttool-atom)
    ]
   ])

(defn view-page-modal [page reporttool-atom]
  (create-class {:reagent-render (fn [] (render-page-modal page reporttool-atom))
                 :component-will-unmount (fn [] (m/close-modal!))}))

(defn edit-page-header [header]
  [:div.row
   [:div.col-sm-12
    [:h1 header]]])

(defn edit-page-buttons [id target save-function]
  [:div {:class "row"
         :id "buttons"}
   [:div.col-xs-8
    [:a {:class (str "btn" (if (= target :content) " btn-active"))
         :href "#"
         :on-click #(do (.preventDefault %) (save-function (str "/page/edit/" id)))}
     (t "1." :page/Content)]
    [:a {:class (str "btn" (if (= target :theme) " btn-active"))
         :href "#"
         :on-click #(do (.preventDefault %) (save-function (str "/page/edit_theme/" id)))}
     (t "2." :page/Theme)]
    [:a {:class (str "btn" (if (= target :settings) " btn-active"))
         :href "#"
         :on-click #(do (.preventDefault %) (save-function (str "/page/settings/" id)))}
     (t "3." :page/Settings)]
    [:a {:class (str "btn" (if (= target :preview) " btn-active"))
         :href "#"
         :on-click #(do (.preventDefault %) (save-function (str "/page/preview/" id)))}
     (t "4." :page/Preview)]]
   [:div {:class "col-xs-4"
          :id "buttons-right"}
    [:a {:class "btn btn-primary"
         :on-click #(do (.preventDefault %) (save-function (str "/page/view/" id)))
         :href "#"}
     (t :page/View)]
    [:a {:class "btn btn-warning"
         :on-click #(m/modal! (delete-page-modal id))}
     (t :page/Delete)]]
   [m/modal-window]])
