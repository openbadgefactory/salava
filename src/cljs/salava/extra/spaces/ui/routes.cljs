(ns salava.extra.spaces.ui.routes
  (:require
   [salava.core.i18n :refer [t]]
   [salava.core.ui.helper :refer [base-path]]
   [salava.extra.spaces.ui.creator :as sc]
   [salava.extra.spaces.ui.my :as my]
   [salava.extra.spaces.ui.modal :as modal]
   [salava.extra.spaces.ui.block :as block]
   [salava.extra.spaces.ui.stats :as stats]
   [salava.extra.spaces.ui.userlist :as users]
   [salava.extra.spaces.ui.explore :as exp]
   [salava.extra.spaces.ui.admin :as admin]
   [reagent.session :as session]
   [salava.extra.spaces.ui.error :as err]))

(defn ^:export routes [context]
  {(str (base-path context) "/admin/spaces") [["" my/handler]
                                              ["/creator" sc/handler]]
   (str (base-path context) "/connections") [["/spaces" block/manage-spaces-handler]]
   (str (base-path context) "/gallery") [["/spaces" exp/handler]]
   (str (base-path context) "/space") [["/admin" stats/handler]
                                       ["/users" users/handler]
                                       ["/stats" stats/handler]
                                       ["/manage" admin/handler]
                                       ["/edit" admin/edit-handler]
                                       ["/error" err/handler]]})

(defn base-navi [context]
  {(str (base-path context) "/admin/spaces") {:weight 100 :title (t :extra-spaces/Spaces) :site-navi true :breadcrumb (t :admin/Admin " / " :extra-spaces/Spaces)} ;:about (:selfie (about))}})
   (str (base-path context) "/admin/spaces/creator") {:weight 200 :title (t :extra-spaces/Createspace) :breadcrumb (t :admin/Admin " / " :extra-spaces/Spaces " / " :extra-spaces/Create)}
   (str (base-path context) "/connections/spaces") {:weight 70 :title (t :extra-spaces/Spaces) :site-navi true :breadcrumb (t :social/Connections " / " :extra-spaces/Spaces)}
   (str (base-path context) "/connections/spaces\\S+") {:weight 70 :title (t :extra-spaces/Spaces) :breadcrumb (t :social/Connections " / " :extra-spaces/Spaces)}
   (str (base-path context) "/gallery/spaces") {:weight 150 :title (t :extra-spaces/Spaces) :site-navi true :breadcrumb (t :gallery/Gallery " / " :extra-spaces/Spaces)}})

(defn member-admin-navi [context]
  {(str (base-path context) "/space/admin") {:weight 150 :title (t :extra-spaces/Memberadmin) :top-navi true :breadcrumb (t :extra-spaces/Space " / " :extra-spaces/Admin)}
   (str (base-path context) "/space/stats") {:weight 500 :title (t :extra-spaces/Statistics) :site-navi true :breadcrumb (t :extra-spaces/Space " / " :extra-spaces/Statistics)}
   (str (base-path context) "/space/manage") {:weight 300 :title (t :extra-spaces/Manage)  :site-navi true :breadcrumb (t :extra-spaces/Space " / " :extra-spaces/Manage)}
   (str (base-path context) "/space/users") {:weight 400 :title (t :extra-spaces/Users)  :site-navi true :breadcrumb (t :extra-spaces/Space " / " :extra-spaces/Users)}
   (str (base-path context) "/space/edit") {:weight 250 :title (t :extra-spaces/Edit)  :site-navi true :breadcrumb (t :extra-spaces/Space " / " :extra-spaces/Edit)}})

(defn ^:export navi [context]
 (let [member-admin? (and  (not= (session/get-in [:user :role]) "admin") (= "admin" (session/get-in [:user :current-space :role])))]
  (as-> (base-navi context) $
        (if member-admin? (merge $ (member-admin-navi context)) (merge $ {})))))
