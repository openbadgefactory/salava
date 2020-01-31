(ns salava.badgeIssuer.ui.helper
  (:require
    [reagent.core :refer [atom cursor create-class]]
    [reagent-modals.modals :as m]
    [reagent.session :as session]
    [salava.badgeIssuer.ui.util :refer [save-selfie-badge]]
    [salava.core.ui.ajax-utils :as ajax]
    [salava.core.ui.helper :refer [navigate-to path-for]]
    [salava.core.i18n :refer [t]]
    [salava.core.ui.modal :as mo]))

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
        [:p [:small (t :badgeIssuer/Image)]]]

       [:div.stepwizard-step.col-xs-4
        [:a#step2.btn.btn-success.btn-circle
         {:type "button"
          :class (if (= 1 @step) "active" "")
          :on-click #(reset! step 1)}
         2]

        [:p [:small (t :badgeIssuer/Content)]]]

       [:div.stepwizard-step.col-xs-4
        [:a#step3.btn.btn-success.btn-circle
         {:type "button"
          :class (if (= 2 @step) "active" "")
          :on-click #(reset! step 2)}
         3]

        [:p [:small (t :badgeIssuer/Settings)]]]]]]))

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
             (when (pos? @step)[:a {:href "#"
                                    :on-click #(do
                                                 (.preventDefault %)
                                                 (reset! step (dec @step)))}
                                  [:div {:id "step-button-previous"}
                                   (t :core/Previous)]])
             ;;preview
             (when (and  (not @(cursor state [:in-modal]))(< @step 2))
               [:a.btn.btn-success.btn-bulky
                  {:href "#"
                   :on-click #(do
                                (.preventDefault %)
                                (mo/open-modal [:selfie :preview] {:badge (:badge @state)}))}
                [:span #_[:i.fa.fa-eye.fa-fw.fa-lg]  (t :badgeIssuer/Preview)]])

                ;;Saveandexit
             (when (= 2 @step)
               [:a.btn.btn-primary.btn-bulky
                {:href "#"
                 :on-click #(save-selfie-badge state (if @(cursor state [:in-modal])
                                                       (fn [] (do
                                                                (init-selfie-badge @(cursor state [:badge :id]) state)
                                                                (reset! (cursor state [:tab]) nil)
                                                                (reset! (cursor state [:tab-no]) 1)))

                                                       (fn [] (navigate-to "/badge/selfie"))))}

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
                                     (save-selfie-badge state (fn [](reset! step (inc @step))))
                                     (reset! step (inc @step))))}
                    [:div.pull-right {:id "step-button"}
                     (t :core/Next)]])]]])
     :component-did-mount (fn [] (reset! step 0))}))
