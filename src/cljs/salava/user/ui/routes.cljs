(ns salava.user.ui.routes
  (:require [salava.core.ui.layout :as layout]
            [salava.core.i18n :as i18n :refer [t]]
            [salava.user.ui.login :as login]
            [salava.user.ui.activate :as password]
            [salava.user.ui.register :as register]
            [salava.user.ui.edit :as edit]))

(defn placeholder [content]
  (fn [site-navi params]
    #(layout/default site-navi content)))

(defn ^:export routes [context]
  {"/user" [["/login" login/handler]
            [["/login/" :next-url] login/handler]
            [["/activate/" :user-id "/" :timestamp "/" :code] password/handler]
            ["/register" register/handler]
            ["/account" (placeholder [:p "My account"])]
            ["/edit" edit/handler]]})

(defn ^:export navi [context]
  {"/user/view"               {:weight 40 :title (t :user/Profile) :breadcrumb   (t :user/User " / " :user/Profile)}
   "/user/edit"               {:weight 41 :title (t :user/Edit) :breadcrumb   (t :user/User " / " :user/Edit)}
   "/user/edit/email-address" {:weight 42 :title (t :user/Emailaddresses) :breadcrumb (t :user/User " / "  :user/Emailaddresses)}
   "/user/edit/fboauth"       {:weight 43 :title (t :user/Facebook) :breadcrumb   (t :user/User " / "  :user/Facebook)}
   "/user/edit/linkedin"      {:weight 44 :title (t :user/Linkedin) :breadcrumb   (t :user/User " / "  :user/Linkedin)}})

