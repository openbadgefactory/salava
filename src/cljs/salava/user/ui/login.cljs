(ns salava.user.ui.login
  (:require [reagent.core :refer [atom cursor]]
            [reagent.session :as session]
            [clojure.string :as string]
            [salava.core.ui.ajax-utils :as ajax]
            [salava.user.ui.input :as input]
            [salava.core.ui.helper :refer [base-url navigate-to]]
            [salava.core.ui.layout :as layout]
            [salava.core.i18n :refer [t]]))

(defn follow-up-url []
  (let [referrer js/document.referrer
        site-url (session/get :site-url)
        path (if (and referrer site-url) (string/replace referrer site-url ""))]
    (if (or (empty? path) (= referrer path) (= path "/user/login"))
      "/badge/mybadges"
      path)))

(defn login [state]
  (let [{:keys [email password]} @state]
    (ajax/POST
      (str "/obpv1/user/login")
      {:params  {:email    email
                 :password password}
       :handler (fn [data]
                  (if (= (:status data) "success")
                    (navigate-to (follow-up-url))
                    (swap! state assoc :error-message (:message data))))})))

(defn content [state]
  (let [email-atom (cursor state [:email])
        password-atom (cursor state [:password])
        error-message-atom (cursor state [:error-message])]
    [:div {:id "login-page"}
     [:div {:id "narrow-panel"
            :class "panel"}
      [:div {:class "panel-body"}
       (if @error-message-atom
         [:div {:class "alert alert-warning"}
          @error-message-atom])
       [:form
        [:div.form-group
         [input/text-field {:name "email" :atom email-atom :placeholder (t :user/Email)}]]
        [:div.form-group
         [input/text-field {:name "password" :atom password-atom :placeholder (t :user/Password) :password? true}]]
        [:button {:class    "btn btn-primary"
                  :on-click #(do (.preventDefault %) (login state))
                  :disabled (not (and (input/email-valid? @email-atom) (input/password-valid? @password-atom)))}
         (t :user/Login)]
        [:div {:class "row login-links"}
         [:div {:class "col-xs-6 text-right"}
          [:a {:href "/user/register"} (t :user/Createnewaccount)]]
         [:div {:class "col-xs-6 text-left"}
          [:a {:href "/user/reset"} (t :user/Requestnewpassword)]]]]]]]))

(defn handler []
  (let [state (atom {:email ""
                     :password ""
                     :error-message nil})]
    (fn []
      (layout/landing-page (content state)))))
