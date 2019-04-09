(ns salava.oauth.ui.helper
  (:require [reagent.session :as session]
            [cljs-uuid-utils.core :refer [make-random-uuid uuid-string]]
            [salava.core.ui.ajax-utils :as ajax]
            [salava.core.ui.helper :refer [path-for]]
            [salava.core.helper :refer [dump]]
            [salava.core.i18n :refer [t]]))


(defn facebook-link [deauthorize? register?]
   (let [fb-app-id (session/get-in [:facebook-app-id])
         redirect-path (if deauthorize? "/oauth/facebook/deauthorize" "/oauth/facebook")
         redirect-uri (js/encodeURIComponent (str (session/get :site-url) (path-for redirect-path)))
         facebook-url (str "https://www.facebook.com/dialog/oauth?client_id=" fb-app-id "&redirect_uri=" redirect-uri "&scope=email")]

     (if fb-app-id
       [:a {:class "btn btn-oauth btn-facebook" :href facebook-url :rel "nofollow"}
        [:i {:class "fa fa-facebook"}]
        (if deauthorize?
          (t :oauth/Unlink)
          (if register?
            (t :oauth/RegisterwithFacebook)
            (t :oauth/LoginwithFacebook)))])))

(defn linkedin-login-link [linkedin-app-id]
  ""
  (let [redirect-uri (js/encodeURIComponent (str (session/get :site-url) (path-for "/oauth/linkedin")))
        random-state (-> (make-random-uuid) (uuid-string))
        linkedin-url (str "https://www.linkedin.com/oauth/v2/authorization?response_type=code&client_id=" linkedin-app-id "&redirect_uri=" redirect-uri "&state=" random-state "&scope=r_liteprofile%20r_emailaddress%20w_member_social") #_(str "https://www.linkedin.com/uas/oauth2/authorization?response_type=code&client_id=" linkedin-app-id "&redirect_uri=" redirect-uri "&state=" random-state "&scope=r_basicprofile%20r_emailaddress")]
    [:a {:class "btn btn-oauth btn-linkedin" :href linkedin-url :rel "nofollow"}
     [:i {:class "fa fa-linkedin"}]
     (t :oauth/LoginwithLinkedin)]))

(defn linkedin-register-link [linkedin-app-id]
  (let [redirect-uri (js/encodeURIComponent (str (session/get :site-url) (path-for "/oauth/linkedin")))
        random-state (-> (make-random-uuid) (uuid-string))
        linkedin-url (str "https://www.linkedin.com/oauth/v2/authorization?response_type=code&client_id=" linkedin-app-id "&redirect_uri=" redirect-uri "&state=" random-state "&scope=r_liteprofile%20r_emailaddress%20w_member_social") #_(str "https://www.linkedin.com/uas/oauth2/authorization?response_type=code&client_id=" linkedin-app-id "&redirect_uri=" redirect-uri "&state=" random-state "&scope=r_basicprofile%20r_emailaddress")]
    [:a {:class "btn btn-oauth btn-linkedin" :href linkedin-url :rel "nofollow"}
     [:i {:class "fa fa-linkedin"}]
     (t :oauth/RegisterwithLinkedin)]))

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
