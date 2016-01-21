(ns salava.user.ui.activate
  (:require [reagent.core :refer [atom cursor]]
            [ajax.core :as ajax]
            [clojure.walk :as walk :refer [keywordize-keys]]
            [salava.core.ui.helper :refer [input-valid?]]
            [salava.core.ui.layout :as layout]
            [salava.core.i18n :refer [t]]
            [salava.user.schemas :as schemas]))

(defn password-valid? [password]
  (input-valid? (:password schemas/User) password))

(defn verify-password-valid? [password verify-password]
  (= password verify-password))

(defn activate-account [state]
  (let [{:keys [user-id code password password-verify]} @state]
    (ajax/POST
      (str "/obpv1/user/activate/")
      {:params  {:user_id         (js/parseInt user-id)
                 :code            code
                 :password        password
                 :password_verify password-verify}
       :handler (fn [data]
                  (let [data-with-kws (keywordize-keys data)]
                    (if (= (:status data-with-kws) "error")
                      (swap! state assoc :error-message (:message data-with-kws))
                      (swap! state assoc :account-activated true))))})))

(defn activation-form [state]
  (let [password-atom (cursor state [:password])
        password-verify-atom (cursor state [:password-verify])]
    [:div {:class "form-horizontal"}
     (if (:error-message @state)
       [:div {:class "alert alert-danger" :role "alert"}
        (:error-message @state)])
     [:div.form-group
      [:label {:class "col-xs-4"
               :for "input-password"}
       (t :user/Password)
       [:span.form-required " *"]]
      [:div.col-xs-8
       [:input {:class "form-control"
                :id "input-password"
                :type "password"
                :name "password"
                :on-change #(reset! password-atom (.-target.value %))
                :value @password-atom}]]]
     [:div.form-group
      [:label {:class "col-xs-4"
               :for "input-password-verify"}
       (t :user/Verifypassword)
       [:span.form-required " *"]]
      [:div.col-xs-8
       [:input {:class "form-control"
                :id "input-password-verify"
                :type "password"
                :name "password-verify"
                :on-change #(reset! password-verify-atom (.-target.value %))
                :value @password-verify-atom}]]]
     [:button {:class    "btn btn-warning"
               :disabled (if-not (and (password-valid? @password-atom)
                                      (verify-password-valid? @password-atom @password-verify-atom))
                           "disabled")
               :on-click #(activate-account state)}
      (t :user/Setpassword)]]))

(defn content [state]
  [:div {:id "login-panel"
         :class "panel"}
   [:div.panel-body
    (if (:account-activated @state)
      [:div {:class "alert alert-success"
             :role "alert"}
       (t :user/Accountactivatedsuccessfully)
       [:a {:href "/user/login"} (t :user/Clickheretologin)]]
      (activation-form state))]])

(defn handler [site-navi params]
  (let [state (atom {:password ""
                     :password-verify ""
                     :user-id (:user-id params)
                     :code (:code params)
                     :error-message nil
                     :account-activated false})]
    (fn []
      (layout/landing-page (content state)))))
