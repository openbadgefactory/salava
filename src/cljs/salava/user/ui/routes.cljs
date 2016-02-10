(ns salava.user.ui.routes
  (:require [salava.core.ui.layout :as layout]
            [salava.core.i18n :as i18n :refer [t]]
            [salava.user.ui.login :as login]
            [salava.user.ui.activate :as password]
            [salava.user.ui.register :as register]
            [salava.user.ui.edit :as edit]
            [salava.user.ui.email-addresses :as email-addresses]))

(defn placeholder [content]
  (fn [site-navi params]
    #(layout/default site-navi content)))

(defn ^:export routes [context]
  {"/user" [["/login" login/handler]
            [["/login/" :next-url] login/handler]
            [["/activate/" :user-id "/" :timestamp "/" :code] password/handler]
            ["/register" register/handler]
            ["/account" (placeholder [:p "My account"])]
            ["/edit" edit/handler]
            ["/edit/email-addresses" email-addresses/handler]]})

(defn ^:export navi [context]
  {"/user/view"                 {:weight 40 :title (t :user/Myprofile) :breadcrumb   (t :user/User " / " :user/Myprofile)}
   "/user/edit"                 {:weight 41 :title (t :user/Accountsettings) :breadcrumb   (t :user/User " / " :user/Accountsettings)}
   "/user/edit/email-addresses" {:weight 42 :title (t :user/Emailaddresses) :breadcrumb (t :user/User " / "  :user/Emailaddresses)}
   "/user/edit/fboauth"         {:weight 43 :title (t :user/Facebook) :breadcrumb   (t :user/User " / "  :user/Facebook)}
   "/user/edit/linkedin"        {:weight 44 :title (t :user/Linkedin) :breadcrumb   (t :user/User " / "  :user/Linkedin)}})

