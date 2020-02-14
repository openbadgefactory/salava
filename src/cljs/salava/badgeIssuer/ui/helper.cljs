(ns salava.badgeIssuer.ui.helper
  (:require
   [clojure.string :refer [blank?]]
   [reagent.core :refer [atom cursor create-class]]
   [reagent-modals.modals :as m]
   [reagent.session :as session]
   [salava.badgeIssuer.ui.util :refer [save-selfie-badge]]
   [salava.core.ui.ajax-utils :as ajax]
   [salava.core.ui.helper :refer [navigate-to path-for]]
   [salava.core.ui.input :refer [editor]]
   [salava.core.i18n :refer [t]]
   [salava.core.ui.modal :as mo]
   [salava.user.ui.helper :refer [profile-picture]]))

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
         1]
        [:p {:class (if (= 0 @step) "active" "")} [:small (t :badgeIssuer/Image)]]]

       [:div.stepwizard-step.col-xs-4
        [:a#step2.btn.btn-success.btn-circle
         {:type "button"
          :class (if (= 1 @step) "active" "")
          :on-click #(reset! step 1)}
         2]

        [:p {:class (if (= 1 @step) "active" "")} [:small (t :badgeIssuer/Content)]]]

       [:div.stepwizard-step.col-xs-4
        [:a#step3.btn.btn-success.btn-circle
         {:type "button"
          :class (if (= 2 @step) "active" "")
          :on-click #(reset! step 2)}
         3]

        [:p {:class (if (= 2 @step) "active" "") } [:small (t :badgeIssuer/Settings)]]]]]]))

(defn init-selfie-badge [id state]
  (ajax/GET
   (path-for (str "/obpv1/selfie/create/" id))
   {;:param {:id (:id @state)}
    :handler (fn [data]
               (swap! state assoc :badge data
                      :generating-image false))}))

(defn bottom-navigation [step state]
  (create-class
   {:reagent-render
    (fn [step]
      [:div.bottom-navigation
       [:hr.border]
       [:div.row
        [:div.col-md-12
             ;;previous
         (when (pos? @step) [:a {:href "#"
                                 :on-click #(do
                                              (.preventDefault %)
                                              (reset! step (dec @step)))}
                             [:div {:id "step-button-previous"}
                              (t :core/Previous)]])
             ;;preview
         (when (and  (not @(cursor state [:in-modal])) (< @step 2))
           [:a.btn.btn-success.btn-bulky
            {:href "#"
             :on-click #(do
                          (.preventDefault %)
                          (mo/open-modal [:selfie :preview] {:badge (assoc (:badge @state) :criteria (if @editor
                                                                                                       (.previewRender (.-options @editor) (get-in @state [:badge :criteria] ""))
                                                                                                       (get-in @state [:badge :criteria] "")))}))}
            [:span  (t :badgeIssuer/Preview)]])

                ;;Saveandexit
         (when (= 2 @step)
           [:a.btn.btn-primary.btn-bulky
            {:href "#"
             :on-click #(save-selfie-badge state (if @(cursor state [:in-modal])
                                                   (fn [] (do
                                                            (init-selfie-badge @(cursor state [:badge :id]) state)
                                                            (reset! (cursor state [:tab]) nil)
                                                            (reset! (cursor state [:tab-no]) 1)))

                                                   (fn [] (if (pos? @(cursor state [:badge :issue_to_self]))
                                                            (navigate-to "/badge")
                                                            (navigate-to "/badge/selfie")))))}

            (t :badgeIssuer/Saveandexit)])

              ;;cancel
         (when (not= @step 2)
           [:a.btn.btn-danger.btn-bulky
            {:href "#"
             :on-click #(do
                          (.preventDefault %)
                          (if @(cursor state [:in-modal])
                            (do
                              (init-selfie-badge @(cursor state [:badge :id]) state)
                              (reset! (cursor state [:tab]) nil)
                              (reset! (cursor state [:tab-no]) 1))
                            (navigate-to "/badge/selfie")))}
            [:span (t :core/Cancel)]])

               ;;save
         #_(when (= 2 @step)
             [:a.btn.btn-primary
              {:href "#"
               :on-click #(save-selfie-badge state nil)}
              (t :badgeIssuer/Save)])

                ;;next
         (when (< @step 2)
           [:a {:href "#"
                :on-click #(do
                             (.preventDefault %)
                             (if (pos? @step)
                               (save-selfie-badge state (fn [] (reset! step (inc @step))))
                               (reset! step (inc @step))))}
            [:div.pull-right {:id "step-button"}
             (t :core/Next)]])]]])
    :component-did-mount (fn [] (reset! step 0))}))

(defn badge-image [badge]
  (let [{:keys [name image]} badge]
    [:div {:class "col-md-3"}
     [:div.badge-image
      [:img {:src (if (re-find #"^data:image" image)
                    image
                    (str "/" image))
             :alt name}]]]))

(defn badge-content [badge]
 (let [{:keys [name description tags criteria_html image]} badge]
  [:div
   [:div.row
    [:div {:class "col-md-12"}
     [:h1.uppercase-header name]
     [:div.description description]]]

   (when-not (blank? criteria_html)
     [:div {:class "row criteria-html"}
      [:div.col-md-12
       [:h2.uppercase-header (t :badge/Criteria)]
       [:div {:dangerouslySetInnerHTML {:__html criteria_html}}]]])

   [:div.row
    (if (not (empty? tags))
      (into [:div.col-md-12 {:style {:margin "10px 0"}}]
            (for [tag tags]
              [:span {:id "tag"
                      :style {:font-weight "bold" :padding "0 2px"}}
               (str "#" tag)])))]]))



(defn profile-link-inline [id first_name last_name picture]
  [:div [:a {:href "#"
             :on-click #(mo/open-modal [:profile :view] {:user-id id})}
         [:img.badge-img {:src (profile-picture picture) :alt ""}]
         (str first_name " " last_name)]])
