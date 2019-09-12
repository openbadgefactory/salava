(ns salava.oauth.ui.routes
  (:require [salava.core.i18n :refer [t]]
            [salava.core.ui.helper :refer [base-path]]
            [salava.oauth.ui.status :as oauth-status]
            [salava.oauth.ui.block]
            [reagent.session :as session]))

(defn ^:export routes [context]
  {(str (base-path context) "/user") [[["/oauth/" :service] oauth-status/handler]]})

#_(defn ^:export navi [context]
    {(str (base-path context) "/user/oauth/facebook") {:weight 44 :title (t :oauth/Facebook) :site-navi true :breadcrumb (t :user/User " / " :oauth/Facebook)}
     (str (base-path context) "/user/oauth/linkedin") {:weight 45 :title (t :oauth/Linkedin) :site-navi true :breadcrumb (t :user/User " / " :oauth/Linkedin)}
     (str (base-path context) "/user/oauth/google") {:weight 46 :title (t :oauth/Google) :site-navi true :breadcrumb (t :user/User " / " :oauth/Google)}})

(defn ^:export navi [context]
 (or (as-> {} $
       (when (session/get-in [:facebook-app-id]) (assoc $ (str (base-path context) "/user/oauth/facebook") {:weight 44 :title (t :oauth/Facebook) :site-navi true :breadcrumb (t :user/User " / " :oauth/Facebook)}))
       (when (session/get-in [:linkedin-app-id]) (assoc $ (str (base-path context) "/user/oauth/linkedin") {:weight 45 :title (t :oauth/Linkedin) :site-navi true :breadcrumb (t :user/User " / " :oauth/Linkedin)}))
       (when (session/get-in [:google-app-id]) (assoc $ (str (base-path context) "/user/oauth/google") {:weight 46 :title (t :oauth/Google) :site-navi true :breadcrumb (t :user/User " / " :oauth/Google)})))
  {}))
