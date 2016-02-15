(ns salava.user.ui.routes
  (:require [salava.core.ui.layout :as layout]
            [salava.core.i18n :as i18n :refer [t]]
            [salava.user.ui.login :as login]
            [salava.user.ui.activate :as password]
            [salava.user.ui.profile :as profile]
            [salava.user.ui.register :as register]
            [salava.user.ui.edit :as edit]
            [salava.user.ui.email-addresses :as email-addresses]))

(defn placeholder [content]
  (fn [site-navi params]
    #(layout/default site-navi content)))

(defn ^:export routes [context]
  {"/user" [[["/profile/" [#"\d+" :user-id]] profile/handler]
            ["/login" login/handler]
            [["/login/" :next-url] login/handler]
            [["/activate/" :user-id "/" :timestamp "/" :code] password/handler]
            ["/register" register/handler]
            ["/account" (placeholder [:p "My account"])]
            ["/edit" edit/handler]
            ["/edit/email-addresses" email-addresses/handler]]})

(defn ^:export navi [context]
  {"/user/profile/\\d+"                                {:breadcrumb (t :user/User " / " (t :user/Profile))}
   (str "/user/profile/" (get-in context [:user :id])) {:weight 40 :title (t :user/Myprofile) :site-navi true :breadcrumb (t :user/User " / " (get-in context [:user :fullname]))}
   "/user/edit"                                        {:weight 41 :title (t :user/Accountsettings) :site-navi true :breadcrumb (t :user/User " / " :user/Accountsettings)}
   "/user/edit/email-addresses"                        {:weight 42 :title (t :user/Emailaddresses) :site-navi true :breadcrumb (t :user/User " / " :user/Emailaddresses)}
   "/user/edit/fboauth"                                {:weight 43 :title (t :user/Facebook) :site-navi true :breadcrumb (t :user/User " / " :user/Facebook)}
   "/user/edit/linkedin"                               {:weight 44 :title (t :user/Linkedin) :site-navi true :breadcrumb (t :user/User " / " :user/Linkedin)}})

