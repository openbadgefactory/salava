(ns salava.extra.customField.ui.routes
 (:require
  [salava.extra.customField.ui.gender]
  [salava.extra.customField.ui.block]
  [salava.core.i18n :refer [t]]
  [salava.core.ui.helper :refer [base-path]]
  [salava.extra.customField.ui.manage :as manage]))

(defn ^:export routes [context]
  {(str (base-path context) "/admin/customField") [["" manage/handler]]})

(defn ^:export navi [context]
  {(str (base-path context) "/admin/customField") {:weight 600 :title (t :extra-customField/Config) :site-navi true :breadcrumb (t :admin/Admin " / " :extra-customField/customField "/" :extra-customField/Manage)}})
