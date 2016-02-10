(ns salava.user.ui.login
  (:require [reagent.core :refer [atom cursor]]
            [salava.core.ui.ajax-utils :as ajax]
            [salava.user.ui.input :as input]
            [salava.core.ui.helper :refer [navigate-to]]
            [salava.core.ui.layout :as layout]
            [salava.core.i18n :refer [t]]))

(defn login [state]
  (let [{:keys [email password]} @state]
    (ajax/POST
      (str "/obpv1/user/login")
      {:params  {:email    email
                 :password password}
       :handler (fn [data]
                  (if (= (:status data) "success")
                    (navigate-to "/badge/mybadges")
                    (swap! state assoc :error-message (:message data))))})))

(defn content [state]
  (let [email-atom (cursor state [:email])
        password-atom (cursor state [:password])
        error-message-atom (cursor state [:error-message])]
    [:div {:id "login-panel"
           :class "panel"}
     [:div {:class "panel-body"}
      (if @error-message-atom
        [:div {:class "alert alert-warning"}
         @error-message-atom])
      [:form
       [:div.form-group
        [input/text-field {:name "email" :atom email-atom}]]
       [:div.form-group
        [input/text-field {:name "password" :atom password-atom :password? true}]]
       [:button {:class    "btn btn-warning"
                 :on-click #(do
                             (.preventDefault %)
                             (login state))
                 :disabled (not (and (input/email-valid? @email-atom) (input/password-valid? @password-atom)))}
        (t :user/Login)]
       [:div {:class "row login-links"}
        [:div {:class "col-xs-6 text-right"}
         [:a {:href "/user/register"} (t :user/Createnewaccount)]]
        [:div {:class "col-xs-6 text-left"}
         [:a {:href "/user/password"} (t :user/Requestnewpassword)]]]]]]))

(defn handler []
  (let [state (atom {:email ""
                     :password ""
                     :error-message nil})]
    (fn []
      (layout/landing-page (content state)))))
