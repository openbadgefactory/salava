(ns salava.user.ui.routes
  (:require [salava.core.ui.layout :as layout]
            [salava.core.i18n :as i18n :refer [t]]
            [salava.core.ui.helper :refer [base-path]]
            [salava.user.ui.login :as login]
            [salava.user.ui.activate :as password]
            [salava.user.ui.profile :as profile]
            [salava.user.ui.embed :as embed]
            [salava.user.ui.register :as register]
            [salava.user.ui.reset :as reset]
            [salava.user.ui.edit :as edit]
            [salava.user.ui.edit-password :as edit-password]
            [salava.user.ui.email-addresses :as email-addresses]
            [salava.user.ui.edit-profile :as edit-profile]
            [salava.user.ui.cancel :as cancel]
            [salava.user.ui.modal :as usermodal]
            [salava.user.ui.data :as data]
            [salava.user.ui.terms :as terms]
            [salava.user.ui.delete-user :as delete-user]))

(defn placeholder [content]
  (fn [site-navi params]
    #(layout/default site-navi content)))

(defn ^:export routes [context]
  {(str (base-path context) "/user") [[["/profile/" [#"\d+" :user-id]] profile/handler]
                                      [["/profile/" [#"\d+" :user-id] "/embed"] embed/handler]
                                      ["/login" login/handler]
                                      [["/login/" :lang] login/handler]
                                      [["/activate/" :user-id "/" :timestamp "/" :code] password/handler]
                                      [["/activate/" :user-id "/" :timestamp "/" :code "/" :lang] password/handler]
                                      ["/register" register/handler]
                                      [["/register/" :lang] register/handler]
                                      ["/reset" reset/handler]
                                      [["/reset/" :lang] reset/handler]
                                      ["/edit" edit/handler]
                                      ["/edit/password" edit-password/handler]
                                      ["/edit/email-addresses" email-addresses/handler]
                                      ["/edit/profile" edit-profile/handler]
                                      ["/cancel" cancel/handler]
                                      ["/terms" terms/handler]
                                      ["/delete-user" delete-user/handler]
                                      [["/data/" [#"\d+" :user-id]] data/handler]]})


(defn ^:export navi [context]
  {;(str (base-path context) "/user/profile/\\d+")                          {:breadcrumb (t :user/User " / " :user/Profile)}
   ;(str (base-path context) "/user/profile/" (get-in context [:user :id])) {:weight 40 :title (t :user/Myprofile) :site-navi true :breadcrumb (str (t :user/User) " / " (get-in context [:user :first_name]) " " (get-in context [:user :last_name]))}
   (str (base-path context) "/user/edit/profile")                          {:breadcrumb (t :user/User " / " :user/Editprofile)}
   (str (base-path context) "/user/edit")                                  {:weight 41 :title (t :user/Accountsettings) :site-navi true :breadcrumb (t :user/User " / " :user/Accountsettings)}
   (str (base-path context) "/user/edit/password")                         {:weight 42 :title (t :user/Passwordsettings) :site-navi true :breadcrumb (t :user/User " / " :user/Passwordsettings)}
   (str (base-path context) "/user/edit/email-addresses")                  {:weight 43 :title (t :user/Emailaddresses) :site-navi true :breadcrumb (t :user/User " / " :user/Emailaddresses)}
   (str (base-path context) "/user/cancel")                                {:weight 49 :title (t :user/Cancelaccount) :site-navi true :breadcrumb (t :user/User " / " :user/Cancelaccount)}
   (str (base-path context) "/user/data/" (get-in context [:user :id]))     {:weight 50 :title (t :user/Mydata) :site-navi true :breadcrumb (t :user/User " / " :user/Mydata)}})

