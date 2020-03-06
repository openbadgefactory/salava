(ns salava.badgeIssuer.ui.creator
  (:require
   [cemerick.url :as url]
   [clojure.string :refer [blank?]]
   [clojure.walk :refer [keywordize-keys]]
   [reagent.core :refer [atom create-class cursor]]
   [reagent-modals.modals :as m]
   [reagent.session :as session]
   [salava.badgeIssuer.ui.helper :refer [#_bottom-navigation progress-wizard bottom-buttons]]
   [salava.badgeIssuer.ui.util :refer [generate-image toggle-setting issuing-history]]
   [salava.core.ui.ajax-utils :as ajax]
   [salava.core.ui.helper :refer [path-for navigate-to current-route-path]]
   [salava.core.ui.input :refer [text-field markdown-editor editor]]
   [salava.core.i18n :refer [t translate-text]]
   [salava.core.ui.layout :as layout]
   [salava.core.ui.modal :as mo]
   [salava.core.ui.popover :refer [info]]
   [salava.core.ui.tag :as tag]))

(defn init-data
  ([state]
   (ajax/POST
     (path-for "/obpv1/selfie/new")
     {:handler (fn [data]
                 (swap! state assoc :badge data
                                    :generating-image false))}))
  ([id state]
   (ajax/GET
    (path-for (str "/obpv1/selfie/create/" id))
    {:handler (fn [data]
               (swap! state assoc :badge data
                                  :generating-image false
                                  :error-message nil))})))

(defn upload-modal [{:keys [status message reason]}]
  [:div
   [:div.modal-header
    [:button {:type "button"
              :class "close"
              :data-dismiss "modal"
              :aria-label "OK"}
     [:span {:aria-hidden "true"
             :dangerouslySetInnerHTML {:__html "&times;"}}]]
    [:h4.modal-title (translate-text message)]]
   [:div.modal-body
    [:div {:class (str "alert " (if (= status "error")
                                  "alert-warning"
                                  "alert-success"))}
     (translate-text message)]]
   [:div.modal-footer
    [:button {:type "button"
              :class "btn btn-primary"
              :data-dismiss "modal"}
     "OK"]]])

(defn send-file [state]
  (let [file (-> (.querySelector js/document "input")
                 .-files
                 (.item 0))
        form-data (doto
                   (js/FormData.)
                   (.append "file" file (.-name file)))]
    (swap! state assoc :generating-image true)
    (ajax/POST
     (path-for "/obpv1/selfie/upload_image")
     {:body    form-data
      :handler (fn [data]
                 (if (= (:status data) "success")
                   (do
                     (reset! (cursor state [:badge :image]) (:url data))
                     (reset! (cursor state [:generating-image]) false))
                  (m/modal! (upload-modal data) {:hidden #(reset! (cursor state [:generating-image]) false)})))})))

#_(defn add-image [state]
      (let [{:keys [badge generating-image]} @state
            {:keys [image name]} badge]
        [:div.col-md-12.panel-section
         [:p (t :badgeIssuer/Aboutselfieimage)]
         [:div.col-md-6.text-center
          [:div.image-container
           (if-not @(cursor state [:generating-image])
             (when-not (blank? image)
              [:img {:src (if (re-find #"^data:image" image)
                            image
                            (str "/" image))
                     :alt "image"}])
             [:span.fa.fa-spin.fa-cog.fa-2x])]]
         [:div.col-md-6.text-center
          [:div.buttons {:style {:margin-bottom "20px"}}
           [:button.btn.btn-primary.btn-bulky
            {:on-click #(do
                         (.preventDefault %)
                         (generate-image state))
             :aria-label (t :badgeIssuer/Generaterandomimage)}
            [:span [:i.fa.fa-random.fa-lg]" "(t :badgeIssuer/Generaterandomimage)]]

           [:div.or (t :user/or)]
           [:span {:class "btn btn-primary btn-file btn-bulky"}
                 [:input {:type       "file"
                          :name       "file"
                          :on-change  #(send-file state)
                          :accept     "image/png"
                          :aria-label (t :badgeIssuer/Uploadbadgeimage)}]
            [:span [:i.fa.fa-upload.fa-lg.fa-fw](t :badgeIssuer/Uploadbadgeimage)]]]]]))

(defn add-image [state]
  (let [{:keys [badge generating-image]} @state
        {:keys [image name]} badge]
    [:div.col-md-12.panel-section

     [:div.row.text-center {:style {:margin "15px 0"}}
      ;[:div.col-md-9;.image-container
       [:div.image-container
        (if-not @(cursor state [:generating-image])
          (when-not (blank? image)
           [:img {:src (if (re-find #"^data:image" image)
                         image
                         (str "/" image))
                  :alt "image"}])
          [:span.fa.fa-spin.fa-cog.fa-2x])]
      [:div.row ;col-md-3;.text-center
       [:div.img-buttons {:style {:margin-bottom "20px"}}
        #_[:button.btn.btn-primary.btn-bulky
             {:on-click #(do
                          (.preventDefault %)
                          (generate-image state))
              :aria-label (t :badgeIssuer/Generaterandomimage)}
             [:span [:i.fa.fa-random.fa-lg]" "(t :badgeIssuer/Generaterandomimage)]]

        [:span {:class "btn btn-primary btn-file btn-bulky"}
              [:input {:type       "file"
                       :name       "file"
                       :on-change  #(send-file state)
                       :accept     "image/png"
                       :aria-label (t :badgeIssuer/Uploadbadgeimage)}]
         [:span [:i.fa.fa-upload.fa-lg.fa-fw](t :badgeIssuer/Uploadbadgeimage)]]
        [:button.btn.btn-light.btn-bulky
         {:on-click #(do
                      (.preventDefault %)
                      (generate-image state))
          :aria-label (t :badgeIssuer/Generaterandomimage)}
         [:span [:i.fa.fa-random.fa-lg]" "(t :badgeIssuer/Generaterandomimage)]]]]]
     [:p (t :badgeIssuer/Aboutselfieimage)]]))

(defn badge-content [state]
  (fn []
    [:div.col-md-12.panel-section
     [:form.form-horizontal
      [:div.form-group
       [:label {:for "input-name"} (t :badge/Name) [:span.form-required " *"] [info {:content (t :badgeIssuer/Badgenameinfo) :placement "right"}]]
       [text-field
        {:name "name"
         :atom (cursor state [:badge :name])
         :placeholder (t :badgeIssuer/Inputbadgename)}]]


      [:div.form-group
       [:label {:for "input-description"} (t :page/Description) [:span.form-required " *"] [info {:content (t :badgeIssuer/Badgedescriptioninfo) :placement "right"}]]
       [text-field
        {:name "description"
         :atom (cursor state [:badge :description])
         :placeholder (t :badgeIssuer/Inputbadgedescription)}]]
      [:div.form-group
       [:span._label {:style {:margin-bottom "5px"}}  (t :badge/Criteria) [:span.form-required " *"] [info {:content (t :badgeIssuer/Badgecriteriainfo) :placement "right"}]]
       [:div.col-md-12 [markdown-editor (cursor state [:badge :criteria])]]]

      [:div.form-group
       [:div [:label {:for "newtags"} (t :badge/Tags)] [:em [:span.text-muted (str " - " (t :badgeIssuer/Optional)) [info {:content (t :badgeIssuer/Badgetagsinfo) :placement "right"}]]]]
       [:div {:class "row"}
        [:div {:class "col-md-12"}
         [tag/tags (cursor state [:badge :tags])]]]
       [:div {:class "form-group"}
        [:div {:class "col-md-12"}
         [tag/new-tag-input (cursor state [:badge :tags]) (cursor state [:badge :new-tag])]]]]]]))



(defn set-badge-content [state]
  [:div
   [badge-content state]])

#_(defn settings-content [state]
      (let [{:keys [badge]} @state
            ifg (cursor state [:badge :issuable_from_gallery])
            its (cursor state [:badge :issue_to_self])]
        [:div.panel.panel-success
         [:div.panel-heading
          [:div.uppercase-header.text-center  (t :badgeIssuer/Settings)]]
         [:div.panel-body
          [:div.col-md-12
           [:div.checkbox
            [:label {:for "ifg"}
             [:input
               {:type "checkbox"
                :on-change #(toggle-setting ifg)
                :checked  (pos? @ifg)
                :id "ifg"}]]
            [:span " " (t :badgeIssuer/Issuablefromgallery)]]]

          (when-not @(cursor state [:in-modal])
            [:div.form-group
             [:fieldset {:class "col-md-9 checkbox"}
              [:legend.col-md-9 ""]
              [:div.col-md-12 [:label {:for "its"}
                               [:input {;:name "issuetoself"
                                        :type      "checkbox"
                                        :id        "its"
                                        :on-change #(toggle-setting its)
                                        :checked   @its}]
                               (str (t :badgeIssuer/Issuetoself))]]]])]]))

#_(defn modal-content [state]
     (let [{:keys [badge generating-image]} @state
           step (cursor state [:step])]
       (init-data (:id badge) state)
       (fn []
         [:div#badge-creator;.col-md-12
          [:h1.sr-only (t :badgeIssuer/Editbadge)]
          [:div.panel
           [progress-wizard state]
           (when (and (:error-message @state) (not (blank? (:error-message @state))))
             [:div
               {:class "alert alert-danger" :role "alert"} (translate-text (:error-message @state))])
           [:div.panel-body {:style {:padding "15px"}}
            [set-badge-content state]
            (case @step
             0 [add-image state]
             1 [badge-content state]
             2 [settings-content state]
             [add-image state])
            [bottom-navigation step state]]]])))

(defn modal-content [state]
 (let [{:keys [badge generating-image]} @state
       step (cursor state [:step])]
   (init-data (:id badge) state)
   (fn []
     [:div#badge-creator {:style {:margin "20px"}}
      [:div.panel.panel-default
       [:div.panel-heading
        [:div.panel-title
         [:img {:src (str "/" (:image badge)) }][:h4.inline " " (t :badgeIssuer/Editbadge) "/" (:name badge)]]]
       #_(when (and (:error-message @state) (not (blank? (:error-message @state))))
             [:div
               {:class "alert alert-danger" :role "alert"} (translate-text (:error-message @state))])
       [:div.panel-body
         [:div
          [add-image state]
          [:hr.border]
          [badge-content state]]]
       [bottom-buttons state]]])))

#_(defn content [state]
      (let [{:keys [badge generating-image]} @state
            step (cursor state [:step])]
        [:div#badge-creator
         [m/modal-window]
         [:h1.uppercase-header (t :badgeIssuer/Createselfiebadge)]
         [:p (t :badgeIssuer/Aboutselfiebadges2)]
         [:div.panel
          [progress-wizard state]
          [:div.panel-body
           [set-badge-content state]
           (case @step
            0 [add-image state]
            1 [badge-content state]
            2 [settings-content state]
            [add-image state])
           [bottom-navigation step state]]]]))


(defn content [state]
  (let [{:keys [badge generating-image]} @state
        step (cursor state [:step])]
    [:div#badge-creator
     [m/modal-window]
     [:h1.uppercase-header (t :badgeIssuer/Createselfiebadge)]
     [:p (t :badgeIssuer/Createnewbadgeinfo)]
     [:div.panel.panel-default
      [:div.panel-heading]
      [:div.panel-body
       [:div
        [add-image state]
        [:hr.border]
        [badge-content state]]]

      [bottom-buttons state]]]))

(defn handler [site-navi params]
  (let [id (:id params)
        state (atom {:badge {:image ""
                             :description ""
                             :name ""
                             :criteria ""
                             :issuable_from_gallery 0
                             :id ""
                             :issue_to_self 0}
                     :generating-image true
                     :step 0
                     :id id
                     :in-modal false
                     :error-message nil})]
    (if-not (blank? id)
      (init-data id state)
      (init-data state))
    (fn []
      (layout/default site-navi (content state)))))
