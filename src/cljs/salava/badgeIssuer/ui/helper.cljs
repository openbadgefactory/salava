(ns salava.badgeIssuer.ui.helper
  (:require
    [reagent.core :refer [atom cursor create-class]]
    [reagent-modals.modals :as m]
    [reagent.session :as session]
    [salava.badgeIssuer.ui.util :refer [save-selfie-badge]]
    [salava.core.ui.helper :refer [navigate-to]]
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

        [:p [:small (t :badgeIssuer/Issue)]]]]]]))

(defn bottom-navigation [step state]
  (create-class
    {:reagent-render
     (fn [step]
        [:div.bottom-navigation
           [:hr.border]
           [:div.row
            [:div.col-md-12
             (when (pos? @step)[:a {:href "#"
                                    :on-click #(do
                                                 (.preventDefault %)
                                                 (reset! step (dec @step)))}
                                  [:div {:id "step-button-previous"}
                                   (t :core/Previous)]])
             [:a.btn.btn-danger
                {:href "#"
                 :on-click #(do
                              (.preventDefault %)
                              (mo/open-modal [:badgeIssuer :preview] {:badge (:badge @state)}))}
                (t :badgeIssuer/Preview)]

             (when (pos? @step)
               [:a.btn.btn-primary
                {:href "#"
                 :on-click #(save-selfie-badge state nil)}
                (t :badgeIssuer/Save)])

             (when (pos? @step)
               [:a.btn.btn-warning
                {:href "#"
                 :on-click #(save-selfie-badge state (fn [] (navigate-to "/badge/issuer")))}
                (t :badgeIssuer/Saveandexit)])

             (when (< @step 2)
                [:a {:href "#"
                     :on-click #(do
                                  (.preventDefault %)
                                  (prn @step)
                                  (reset! step (inc @step)))}
                   [:div.pull-right {:id "step-button"}
                    (t :core/Next)]])]]])
     :component-did-mount (fn [] (reset! step 0))}))
