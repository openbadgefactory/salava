(ns salava.user.ui.login
  (:require [reagent.core :refer [atom cursor]]
            [reagent.session :as session]
            [clojure.string :as string]
            [salava.core.ui.ajax-utils :as ajax]
            [salava.user.ui.input :as input]
            [salava.oauth.ui.helper :refer [facebook-link linkedin-link]]
            [salava.core.ui.helper :refer [base-path navigate-to path-for]]
            [salava.core.ui.layout :as layout]
            [salava.social.ui.helper :refer [social-plugin?]]
            [salava.core.i18n :refer [t translate-text]]))

(defn follow-up-url []
  (let [referrer js/document.referrer
        site-url (str (session/get :site-url) (base-path))
        path (if (and referrer site-url) (string/replace referrer site-url ""))]
    (if (or (empty? path) (= referrer path) (= path (path-for "/user/login")))
      (if (social-plugin?) "/social/stream" "/badge/mybadges")
      
      path)))

(defn login [state]
  (let [{:keys [email password]} @state]
    (ajax/POST
      (path-for "/obpv1/user/login")
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
          (translate-text @error-message-atom)])
       [:form
        [:div.form-group {:aria-label "email"}
         [input/text-field {:name "email" :atom email-atom :placeholder (t :user/Email) :aria-label (t :user/Email)}]]
        [:div.form-group
         [input/text-field {:name "password" :atom password-atom :placeholder (t :user/Password) :aria-label (t :user/Password) :password? true}]]
        [:button {:class    "btn btn-primary"
                  :on-click #(do (.preventDefault %) (login state))
                  :disabled (not (and (input/email-valid? @email-atom) (input/password-valid? @password-atom)))}
         (t :user/Login)]
        [:div {:class "row login-links"}
         [:div {:class "col-xs-6"}
          [:a {:href (path-for "/user/register")} (t :user/Createnewaccount)]]
         [:div {:class "col-sm-6"}
          [:a {:href (path-for "/user/reset")} (t :user/Requestnewpassword)]]]
        [:div {:class "row oauth-buttons"}
         [:div {:class "col-xs-6"} (facebook-link false)]
         [:div.col-sm-6 (linkedin-link nil nil)]]]]]]))

(defn handler [site-navi params]
  (let [flash-message (t (keyword (session/get! :flash-message)))
        state (atom {:email         ""
                     :password      ""
                     :error-message (if (not-empty flash-message) flash-message)})
        lang (:lang params)]
    (if (and lang (some #(= lang %) (session/get :languages)))
      (session/assoc-in! [:user :language] lang))
    (fn []
      (layout/landing-page site-navi (content state)))))
