(ns salava.extra.spaces.ui.routes
  (:require
   [salava.core.i18n :refer [t]]
   [salava.core.ui.helper :refer [base-path]]
   [salava.extra.spaces.ui.creator :as sc]
   [salava.extra.spaces.ui.my :as my]
   [salava.extra.spaces.ui.modal :as modal]
   [salava.extra.spaces.ui.block :as block]
   [reagent.session :as session]))

(defn ^:export routes [context]
  {(str (base-path context) "/admin/spaces") [["" my/handler]
                                              ["/creator" sc/handler]]
   (str (base-path context) "/connections") [["/spaces" block/manage-spaces-handler]]})


(defn base-navi [context]
  {(str (base-path context) "/admin/spaces") {:weight 100 :title (t :extra-spaces/Members) :site-navi true :breadcrumb (t :admin/Admin " / " :extra-spaces/Members)} ;:about (:selfie (about))}})
   (str (base-path context) "/admin/spaces/creator") {:weight 200 :title (t :extra-spaces/CreateSpace) :breadcrumb (t :admin/Admin " / " :extra-spaces/Members " / " :extra-spaces/Create)}
   (str (base-path context) "/connections/spaces") {:weight 70 :title (t :extra-spaces/Organizations) :site-navi true :breadcrumb (t :social/Connections " / " :extra-spaces/Organizations)}})

(defn ^:export navi [context]
 (as-> (base-navi context) $
       (if (and  (not= (session/get-in [:user :role]) "admin") (= "admin" (session/get-in [:user :current-space :role])))
           (assoc $ (str (base-path context) "/spaces/user/admin") {:weight 300 :title (t :extra-spaces/Memberadmin) :top-navi true})
           (merge $ {}))))
   ;(if-not (blank? (session/get-in [:facebook-app-id])) (assoc $ (str (base-path context) "/user/oauth/facebook") {:weight 44 :title (t :oauth/Facebook) :site-navi true :breadcrumb (t :user/User " / " :oauth/Facebook) :about (:facebook (about))}) (merge $ {}))
   ;(if-not (blank? (session/get-in [:linkedin-app-id])) (assoc $ (str (base-path context) "/user/oauth/linkedin") {:weight 45 :title (t :oauth/Linkedin) :site-navi true :breadcrumb (t :user/User " / " :oauth/Linkedin) :about (:linkedin (about))}) (merge $ {}))
   ;(if-not (blank? (session/get-in [:google-app-id])) (assoc $ (str (base-path context) "/user/oauth/google") {:weight 46 :title (t :oauth/Google) :site-navi true :breadcrumb (t :user/User " / " :oauth/Google) :about (:google (about))}) (merge $ {}))))
