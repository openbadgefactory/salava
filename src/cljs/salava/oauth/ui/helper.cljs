(ns salava.oauth.ui.helper
  (:require [reagent.session :as session]
            [cljs-uuid-utils.core :refer [make-random-uuid uuid-string]]
            [salava.core.ui.ajax-utils :as ajax]
            [salava.core.ui.helper :refer [path-for navigate-to]]
            [salava.core.helper :refer [dump]]
            [salava.core.i18n :refer [t]]
            [reagent.core :refer [create-class]]))


(defn facebook-link [deauthorize? register?]
   (let [fb-app-id (session/get-in [:facebook-app-id])
         redirect-path (if deauthorize? "/oauth/facebook/deauthorize" "/oauth/facebook")
         redirect-uri (js/encodeURIComponent (str (session/get :site-url) (path-for redirect-path)))
         facebook-url (str "https://www.facebook.com/dialog/oauth?client_id=" fb-app-id "&redirect_uri=" redirect-uri "&scope=email")]
     (when fb-app-id
       [:a {:class "btn btn-oauth btn-facebook" :href facebook-url :rel "nofollow"}
        [:i {:class "fa fa-facebook"}]
        (if deauthorize?
          (t :oauth/Unlink)
          (if register?
            (t :oauth/RegisterwithFacebook)
            (t :oauth/LoginwithFacebook)))])))

(defn linkedin-login-link [linkedin-app-id]
 (let [redirect-uri (js/encodeURIComponent (str (session/get :site-url) (path-for "/oauth/linkedin")))
       random-state (-> (make-random-uuid) (uuid-string))
       linkedin-url (str "https://www.linkedin.com/oauth/v2/authorization?response_type=code&client_id=" linkedin-app-id "&redirect_uri=" redirect-uri "&state=" random-state "&scope=r_liteprofile%20r_emailaddress%20w_member_social") #_(str "https://www.linkedin.com/uas/oauth2/authorization?response_type=code&client_id=" linkedin-app-id "&redirect_uri=" redirect-uri "&state=" random-state "&scope=r_basicprofile%20r_emailaddress")]
   (when linkedin-app-id
    [:a {:class "btn btn-oauth btn-linkedin" :href linkedin-url :rel "nofollow"}
     [:i {:class "fa fa-linkedin"}]
     (t :oauth/LoginwithLinkedin)])))

(defn linkedin-register-link [linkedin-app-id]
  (let [redirect-uri (js/encodeURIComponent (str (session/get :site-url) (path-for "/oauth/linkedin")))
        random-state (-> (make-random-uuid) (uuid-string))
        linkedin-url (str "https://www.linkedin.com/oauth/v2/authorization?response_type=code&client_id=" linkedin-app-id "&redirect_uri=" redirect-uri "&state=" random-state "&scope=r_liteprofile%20r_emailaddress%20w_member_social") #_(str "https://www.linkedin.com/uas/oauth2/authorization?response_type=code&client_id=" linkedin-app-id "&redirect_uri=" redirect-uri "&state=" random-state "&scope=r_basicprofile%20r_emailaddress")]
   (when linkedin-app-id
     [:a {:class "btn btn-oauth btn-linkedin" :href linkedin-url :rel "nofollow"}
      [:i {:class "fa fa-linkedin"}]
      (t :oauth/RegisterwithLinkedin)])))

(defn linkedin-unlink [unlink-fn active-atom]
  [:a {:class "btn btn-oauth btn-linkedin" :on-click #(unlink-fn active-atom)}
   [:i {:class "fa fa-linkedin"}]
   (t :oauth/Unlink)])

(defn linkedin-link
  "nil nil = linkedin-login
  nil register = linkedin-register"
  [unlink-fn state]
  (let [linkedin-app-id (session/get-in [:linkedin-app-id])]
    (if linkedin-app-id
      (cond
        (and unlink-fn state) (linkedin-unlink unlink-fn state)
        (= "register" state) (linkedin-register-link linkedin-app-id)
        :else (linkedin-login-link linkedin-app-id)))))

(defn google-link [revoke? register?]
  (let [app-id (session/get-in [:google-app-id])
        redirect-path (if revoke? "/oauth/google/deauthorize" "/oauth/google")
        redirect-uri (js/encodeURIComponent (str (session/get :site-url) (path-for redirect-path)))
        random-state (-> (make-random-uuid) (uuid-string))
        google-url (str "https://accounts.google.com/o/oauth2/v2/auth?response_type=code&client_id=" app-id "&redirect_uri=" redirect-uri "&state=" random-state "&scope=profile%20email")]
    (when app-id
     [:a.btn-oauth {:href google-url :rel "nofollow"}
      (if revoke?
       [:div.btn-google-logged-in
        [:div.content-wrapper
         [:div.icon
          [:img {:src (str "/img/google_login.png")}]]
         [:span.content (t :oauth/Unlink)]]]
       [:div.btn.btn-google
        [:span.icon
         [:img {:src (str "/img/google_login.png")}]]
        [:div.text
         (if register? (t :oauth/RegisterwithGoogle) (t :oauth/LoginwithGoogle))]])])))
