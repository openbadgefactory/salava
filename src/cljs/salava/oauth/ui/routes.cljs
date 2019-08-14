(ns salava.oauth.ui.routes
  (:require [salava.core.i18n :refer [t]]
            [salava.core.ui.helper :refer [base-path]]
            [salava.oauth.ui.status :as oauth-status]
            [salava.oauth.ui.authorize :as auth]
            ))

(defn ^:export routes [context]
  {(str (base-path context) "/user") [[["/oauth2/authorize"] auth/handler]
                                      [["/oauth/" :service] oauth-status/handler]]})

(defn ^:export navi [context]
  {(str (base-path context) "/user/oauth/facebook") {:weight 44 :title (t :oauth/Facebook) :site-navi true :breadcrumb (t :user/User " / " :oauth/Facebook)}
   (str (base-path context) "/user/oauth/linkedin") {:weight 45 :title (t :oauth/Linkedin) :site-navi true :breadcrumb (t :user/User " / " :oauth/Linkedin)}
   })
