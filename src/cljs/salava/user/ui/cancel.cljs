(ns salava.user.ui.cancel
  (:require [reagent.core :refer [atom cursor]]
            [salava.core.ui.ajax-utils :as ajax]
            [salava.core.ui.helper :refer [input-valid? navigate-to path-for]]
            [salava.core.ui.layout :as layout]
            [salava.core.i18n :refer [t]]
            [salava.user.schemas :as schemas]))

(defn password-valid? [password]
  (input-valid? (:password schemas/User) password))

(defn cancel-account [state]
  (ajax/POST
    (path-for "/obpv1/user/delete")
    {:params  {:password (:password @state)}
     :handler (fn [data]
                (if (= (:status data) "error")
                  (swap! state assoc :error-message (t :user/Erroroccuredduringaccountcancellation))
                  (navigate-to "/user/login")))}))

(defn cancel-form [state]
  (let [password-atom (cursor state [:password])]
    [:div {:class "form-horizontal"}
     (if (:error-message @state)
       [:div {:class "alert alert-warning" :role "alert"}
        (:error-message @state)])
     [:div.form-group
      [:label {:class "col-xs-3"
               :for "input-password"}
       (t :user/Password)
       [:span.form-required " *"]]
      [:div.col-xs-9
       [:input {:class       "form-control"
                :id          "input-password"
                :type        "password"
                :name        "password"
                :read-only   true
                :on-change   #(reset! password-atom (.-target.value %))
                :on-focus    #(.removeAttribute (.-target %) "readonly")
                :placeholder (t :user/Tocancelaccountenterpassword)
                :value       @password-atom}]]]
     [:button {:class    "btn btn-warning"
               :disabled (if-not (password-valid? @password-atom)
                           "disabled")
               :on-click #(cancel-account state)}
      (t :user/Cancelaccount)]]))

(defn content [state]
  [:div {:id "cancel-account"}
   [:h1.uppercase-header (t :user/Cancelaccount)]
   [:div {:id "cancel-info"}
    [:p "You are about to cancel your user account"]
    [:p "All badges and pages in your account will be removed from the site. Remember\nto a) upload your badges to your computer or b) save them to Mozilla\nBackpack. Here's how:"]
    [:p "a) upload your badges to your computer:"]
    [:ul
     [:li "Go to " [:a {:href (path-for "/badge/export")} "badge export"] " page"]
     [:li "Click the icon on the right lower corner of the box where the badge is, \"download badge\""]]
    [:p "b) export badges to Mozilla Backpack:"]
    [:ul
     [:li "Go to " [:a {:href (path-for "/badge/export")} "badge export"] " page"]
     [:li "Click \"Select All\" button in order to select all of your badges or click one-by-one \"Export to backpack\" "]
     [:li "Click \"Export selected badges to backpack\""]
     [:li "Follow the Backpack instructions after this "]]
    [:p [:b "NOTE: The cancellation of your account is not reversible. "]]]
   [:div {:class "panel"}
    [:div.panel-body
     (cancel-form state)]]])

(defn handler [site-navi]
  (let [state (atom {:password "" :error-message nil})]
    (fn []
      (layout/default site-navi (content state)))))
