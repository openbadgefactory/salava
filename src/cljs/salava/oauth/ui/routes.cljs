(ns salava.oauth.ui.routes
  (:require [salava.core.i18n :refer [t]]
            [salava.core.ui.helper :refer [base-path]]
            [salava.oauth.ui.status :as oauth-status]
            [salava.oauth.ui.authorize :as auth]
            [salava.oauth.ui.block]
            [reagent.session :as session]
            [clojure.string :refer [blank?]]))

(defn ^:export routes [context]
  {(str (base-path context) "/user") [[["/oauth2/authorize"] auth/handler]
                                      [["/oauth/" :service] oauth-status/handler]]})

#_(defn ^:export navi [context]
    {(str (base-path context) "/user/oauth/facebook") {:weight 44 :title (t :oauth/Facebook) :site-navi true :breadcrumb (t :user/User " / " :oauth/Facebook)}
     (str (base-path context) "/user/oauth/linkedin") {:weight 45 :title (t :oauth/Linkedin) :site-navi true :breadcrumb (t :user/User " / " :oauth/Linkedin)}
     (str (base-path context) "/user/oauth/google") {:weight 46 :title (t :oauth/Google) :site-navi true :breadcrumb (t :user/User " / " :oauth/Google)}})

(def about
  {:google {:heading (t :oauth/Google)
            :content [:div
                      (t :oauth/AboutGooglelogin)]}
   :facebook {:heading (t :oauth/Facebook)
              :content [:div
                        (t :oauth/AboutFacebooklogin)]}
   :linkedin {:heading (t :oauth/LinkedIn)
              :content [:div
                        (t :oauth/AboutLinkedinlogin)]}})

(defn ^:export navi [context]
 (as-> {} $
   (if-not (blank? (session/get-in [:facebook-app-id])) (assoc $ (str (base-path context) "/user/oauth/facebook") {:weight 44 :title (t :oauth/Facebook) :site-navi true :breadcrumb (t :user/User " / " :oauth/Facebook) :about (:facebook about)}) (merge $ {}))
   (if-not (blank? (session/get-in [:linkedin-app-id])) (assoc $ (str (base-path context) "/user/oauth/linkedin") {:weight 45 :title (t :oauth/Linkedin) :site-navi true :breadcrumb (t :user/User " / " :oauth/Linkedin) :about (:linkedin about)}) (merge $ {}))
   (if-not (blank? (session/get-in [:google-app-id])) (assoc $ (str (base-path context) "/user/oauth/google") {:weight 46 :title (t :oauth/Google) :site-navi true :breadcrumb (t :user/User " / " :oauth/Google) :about (:google about)}) (merge $ {}))))
