(ns salava.extra.spaces.ui.routes
  (:require
   [salava.core.i18n :refer [t]]
   [salava.core.ui.helper :refer [base-path]]
   [salava.extra.spaces.ui.creator :as sc]
   [salava.extra.spaces.ui.my :as my]
   [salava.extra.spaces.ui.modal :as modal]))


(defn ^:export routes [context]
  {(str (base-path context) "/admin/spaces") [["" my/handler]
                                              ["/creator" sc/handler]]})


(defn ^:export navi [context]
  {(str (base-path context) "/admin/spaces") {:weight 100 :title (t :extra-spaces/Members) :site-navi true :breadcrumb (t :admin/Admin " / " :extra-spaces/Members)} ;:about (:selfie (about))}})
   (str (base-path context) "/admin/spaces/creator") {:weight 200 :title (t :extra-spaces/CreateSpace) :breadcrumb (t :admin/Admin " / " :extra-spaces/Members " / " :extra-spaces/Create)}})
