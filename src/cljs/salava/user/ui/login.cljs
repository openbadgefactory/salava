(ns salava.user.ui.login
  (:require [reagent.core :refer [atom cursor]]
            [reagent.session :as session]
            [clojure.string :as string]
            [salava.core.helper :as h]
            [salava.core.ui.ajax-utils :as ajax]
            [salava.user.ui.input :as input]
            [salava.oauth.ui.helper :refer [facebook-link linkedin-link]]
            [salava.core.ui.helper :refer [base-path js-navigate-to path-for private? plugin-fun input-valid?]]
            [salava.core.ui.layout :as layout]
            [salava.social.ui.helper :refer [social-plugin?]]
            [salava.core.helper :refer [dump]]
            [salava.user.schemas :as schemas]
            [salava.core.i18n :refer [t translate-text]]))

(defn verification-token [url]
  (if-let [match (re-find #"verification_key=([\w-]+)" url) ]
    (second match)))

(defn follow-up-url []

  (let [
        verification-key (verification-token js/window.location.search)
        manual-referrer (session/get :referrer)
        referrer js/document.referrer
        site-url (str (session/get :site-url) (base-path))
        path (if (and referrer site-url (string/starts-with? referrer site-url)) (string/replace referrer site-url "") )]

    (session/put! :referrer nil)
    (cond
      (not (empty? verification-key))  (str "/user/verify_email/" verification-key)
      (and (not (empty? manual-referrer)) (string/starts-with? manual-referrer "/")) manual-referrer
      (and (not (empty? path)) (not= "/user/login" path) (not= path (path-for "/user/login"))) path
      :else "/social/stream")))

(defn login [state]
  (let [{:keys [email password]} @state]
    (ajax/POST
      (path-for "/obpv1/user/login")
      {:params  {:email    email
                 :password password}
       :handler (fn [data]
                  (if (= (:status data) "success")
                    (js-navigate-to (follow-up-url))
                    (swap! state assoc :error-message (:message data))))}
      )))

(defn content [state]
  (let [email-atom (cursor state [:email])
        password-atom (cursor state [:password])
        error-message-atom (cursor state [:error-message])

        ]
    [:div {:id "login-page"}
     [:div {:id "narrow-panel"
            :class "panel"}
      [:div {:class "panel-body"}
       (if @error-message-atom
         [:div {:class "alert alert-warning"}
          (translate-text @error-message-atom)])
       ;(if login-info (login-info))
       [:form
        [:div.form-group {:aria-label "email"}
         [input/text-field {:name "email" :atom email-atom :error-message-atom error-message-atom :placeholder (t :user/Email) :aria-label (t :user/Email)}]]
        [:div.form-group
         [input/text-field {:name "password" :atom password-atom :error-message-atom error-message-atom :placeholder (t :user/Password) :aria-label (t :user/Password) :password? true}]]
        [:button {:class    "btn btn-primary login-button"
                  :on-click #(do (.preventDefault %)
                                 (if-not (input-valid? schemas/LoginUser {:email @email-atom :password @password-atom})
                                   (swap! state assoc :error-message "user/Loginfailed"))
                                 (login state))}
         (t :user/Login)]
        [:div {:class "row login-links"}
         [:div.management-links
          (if-not (private?)
            [:div {:class "col-sm-6 left-column"}
             [:a {:href (path-for "/user/register")} (t :user/Createnewaccount)]])
          [:div {:class (if (private?) "col-xs-12" "col-sm-6 right-column")}
           [:a {:href (path-for "/user/reset")} (t :user/Requestnewpassword)]]]
         [:div {:class "row oauth-buttons"}
          [:div {:class "col-sm-6 left-column"} (facebook-link false nil)]
          [:div.col-sm-6.right-column (linkedin-link nil nil)]]]
        #_[:div {:class "row login-links"}
         [:div.management-links
          (if-not (private?)
            [:div {:class "col-xs-6"}
             [:a {:href (path-for "/user/register")} (t :user/Createnewaccount)]])
          [:div {:class (if (private?) "col-xs-12" "col-sm-6")}
           [:a {:href (path-for "/user/reset")} (t :user/Requestnewpassword)]]]
         [:div {:class "row oauth-buttons"}
          [:div {:class "col-xs-6"} (facebook-link false nil)]
          [:div.col-sm-6 (linkedin-link nil nil)]]]]]]]))

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
