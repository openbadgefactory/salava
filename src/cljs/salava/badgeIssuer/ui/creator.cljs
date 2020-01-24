(ns salava.badgeIssuer.ui.creator
  (:require
   [clojure.string :refer [blank?]]
   [reagent.core :refer [atom create-class cursor]]
   [reagent-modals.modals :as m]
   [reagent.session :as session]
   [salava.core.ui.ajax-utils :as ajax]
   [salava.core.ui.helper :refer [path-for navigate-to]]
   [salava.core.ui.input :refer [text-field markdown-editor]]
   [salava.core.i18n :refer [t translate-text]]
   [salava.core.ui.layout :as layout]
   [salava.core.ui.modal :as mo]
   [salava.core.ui.tag :as tag]))


(defn init-data [state]
  (ajax/GET
    (path-for "/obpv1/issuer")
    {:handler (fn [data]
                (swap! state assoc :badge data
                                   :generating-image false))}))


(defn generate-image [state]
  (reset! (cursor state [:generating-image]) true)
  (ajax/GET
    (path-for "/obpv1/issuer/generate_image")
    {:handler (fn [{:keys [status url message]}]
                (when (= "success" status)
                  (reset! (cursor state [:badge :image]) url)
                  (reset! (cursor state [:generating-image]) false)))}))

(defn upload-image [])
(defn save-badge [])

(defn progress-wizard [state]
  (let [step (cursor state [:step])]
    [:div.container
     [:div.stepwizard
      [:div.stepwizard-row.setup-panel
       [:div.stepwizard-step.col-xs-4
        [:a#step1.btn.btn-success.btn-circle
         {:type "button"
          :class (if (= 0 @step) "active" "")
          :on-click #(reset! step 0)}
          ;:disabled (not= 0 step)}
         1]
        [:p [:small (t :badgeIssuer/Image)]]]

       [:div.stepwizard-step.col-xs-4
        [:a#step2.btn.btn-success.btn-circle
         {:type "button"
          :class (if (= 1 @step) "active" "")
          :on-click #(reset! step 1)}
          ;:disabled (not= 1 @step)}
         2]

        [:p [:small (t :badgeIssuer/Content)]]]

       [:div.stepwizard-step.col-xs-4
        [:a#step3.btn.btn-success.btn-circle
         {:type "button"
          :class (if (= 2 @step) "active" "")
          :on-click #(reset! step 2)}
          ;:disabled (not (= 2 @step))}
         3]

        [:p [:small (t :badgeIssuer/Issue)]]]]]]))

(defn bottom-navigation [state]
  (let [step (cursor state [:step])
        previous? (pos? @step)
        next? (< @step 2)]
    [:div.bottom-navigation
     [:hr.border]
     [:div.row
      [:div.col-md-12
       (when previous? [:a {:href "#"
                            :on-click #(do
                                         (.preventDefault %)
                                         (reset! step (dec @step)))}
                        [:div {:id "step-button-previous"}
                         (t :core/Previous)]])
       (when (= 1 @step)
         [:a.btn.btn-danger
          {:href "#"
           :on-click #(do
                        (.preventDefault %)
                        (mo/open-modal [:badgeIssuer :preview] {:badge (:badge @state)}))}
          (t :badgeIssuer/Preview)])

       (when next?
         [:a {:href "#"
              :on-click #(do
                           (.preventDefault %)
                           (reset! step (inc @step)))}
            [:div.pull-right {:id "step-button"}
             (t :core/Next)]])]]]))

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
     (path-for "/obpv1/issuer/upload_image")
     {:body    form-data
      :handler (fn [data]
                 (if (= (:status data) "success")
                   (do
                     (reset! (cursor state [:badge :image]) (:url data))
                     (reset! (cursor state [:generating-image]) false))
                  (m/modal! (upload-modal data) {:hidden #(reset! (cursor state [:generating-image]) false)})))})))

(defn badge-content [state]
  (fn []
    [:div.panel.panel-success
     [:div.panel-heading
      [:div.uppercase-header.text-center (t :badgeIssuer/Addbadgecontent)]]
     [:div.panel-body
      [:div.col-md-12
       [:div]

       [:form.form-horizontal
        [:div.form-group
         [:label {:for "input-name"} (t :badge/Name) [:span.form-required " *"]]
         [text-field
          {:name "name"
           :atom (cursor state [:badge :name])
           :placeholder (t :badgeIssuer/Inputbadgename)}]]
        [:div.form-group
         [:label {:for "input-description"} (t :page/Description) [:span.form-required " *"]]
         [text-field
          {:name "description"
           :atom (cursor state [:badge :description])
           :placeholder (t :badgeIssuer/Inputbadgedescription)}]]
        [:div.form-group
         [:label {:for "newtags"} (t :badge/Tags)]
         [:div {:class "row"}
          [:div {:class "col-md-12"}
           [tag/tags (cursor state [:badge :tags])]]]
         [:div {:class "form-group"}
          [:div {:class "col-md-12"}
           [tag/new-tag-input (cursor state [:badge :tags]) (cursor state [:badge :new-tag])]]]]]

       [:div.row
        [:span._label  (t :badge/Criteria) [:span.form-required " *"]]
        [markdown-editor (cursor state [:badge :criteria])]]]]]))

(defn add-image [state]
  (let [{:keys [badge generating-image]} @state
        {:keys [image name]} badge]
    [:div.panel.panel-success
     [:div.panel-heading
      [:div.uppercase-header.text-center  (t :badgeIssuer/Addbadgeimage)]]
     [:div.panel-body
      ;[:p (t :badgeIssuer/Generateimageorupload)]
      [:div.col-md-12 {:style {:margin "20px 0"}}

       [:div.col-md-6.text-center
        ;[:div (t :badgeIssuer/Generateimageorupload)]
        [:div.buttons {:style {:margin-bottom "20px"}}
         [:button.btn.btn-primary
          {:on-click #(do
                       (.preventDefault %)
                       (generate-image state))
           :aria-label (t :badgeIssuer/Generaterandomimage)}
          (t :badgeIssuer/Generaterandomimage)]

         [:div.or (t :user/or)]
         [:span {:class "btn btn-primary btn-file"}
               [:input {:type       "file"
                        :name       "file"
                        :on-change  #(send-file state)
                        :accept     "image/png"
                        :aria-label (t :badgeIssuer/Uploadbadgeimage)}]
          (t :badgeIssuer/Uploadbadgeimage)]]]



       [:div.col-md-6.text-center
        [:div.image-container
         (if-not @(cursor state [:generating-image])
           (when-not (blank? image)
            [:img {:src image :alt "image"}])
            ;[:img {:src (str "/" (:path uploaded-image)) :alt (or name "File upload")}])
           [:span.fa.fa-spin.fa-cog.fa-2x])]]]]]))



(defn content [state]
  (let [{:keys [badge generating-image]} @state
        {:keys [uploaded-image data-url name]} badge
        step (cursor state [:step])]
    [:div#badge-creator
     [m/modal-window]
     [:h1.sr-only (t :badgeIssuer/Badgecreator)]
     [:div.panel
      [progress-wizard state]
      [:div.panel-body
       (case @step
         0 [add-image state]
         1 [badge-content state]
         [add-image state])

       [:div.badge-name]
       [bottom-navigation state]]]]))


(defn handler [site-navi]
  (let [state (atom {:badge {:image ""
                             :description ""
                             :name ""
                             :criteria ""}
                     :generating-image true
                     :step 0})]

    (init-data state)
    (fn []
      (layout/default site-navi (content state)))))
