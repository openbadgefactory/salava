(ns salava.profile.ui.routes
  (:require [salava.core.ui.layout]
            [salava.core.ui.helper :refer [base-path]]
            [salava.user.ui.profile :as profile]
            [salava.core.i18n :as i18n :refer [t]]
            [salava.page.ui.my :as page]
            [salava.profile.ui.block :as block]
            [salava.profile.ui.profile :as p]
            [salava.profile.ui.edit :as pe]
            [salava.profile.ui.modal :as m]))

(defn ^:export routes [context]
  {(str (base-path context) "/profile") [[["/" [#"\d+" :user-id]] p/handler]]
   (str (base-path context) "/page") [[["/mypages" page/handler]]]})

(defn ^:export navi [context]
  {(str (base-path context) "/profile/\\d+")  {:breadcrumb (t :user/User " / " :user/Profile)}
   (str (base-path context) "/profile/" (get-in context [:user :id])) {:weight 30 :title (t :user/Profile) :top-navi true :site-navi true :breadcrumb (str (t :user/Profile) " / " (get-in context [:user :first_name]) " " (get-in context [:user :last_name]))}})
