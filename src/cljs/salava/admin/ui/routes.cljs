(ns salava.admin.ui.routes
   (:require [salava.core.ui.layout :as layout]
             [salava.core.i18n :as i18n :refer [t]]
             [salava.core.ui.helper :refer [base-path]]
             [salava.admin.ui.tickets :as t]
             [salava.admin.ui.statistics :as s]))

(defn placeholder [content]
  (fn [site-navi params]
    #(layout/default site-navi content)))

(defn ^:export routes [context]
  {(str (base-path context) "/admin") [["" s/handler]
                                       ["/tickets" t/handler]
                                       ["/statistics" s/handler]]})

(defn admin-view [context]
  {(str (base-path context) "/admin")         {:weight 50 :title (t :admin/Admin) :top-navi true :breadcrumb (t :admin/Admin)}
   (str (base-path context) "/admin/tickets") {:weight 51 :title (t :admin/Tickets) :site-navi true :breadcrumb (t :admin/Admin " / "  :admin/Tickets)}
   (str (base-path context) "/admin/statistics") {:weight 52 :title (t :admin/Statistics) :site-navi true :breadcrumb (t :admin/Admin " / "  :admin/Statistics)}}
  )

(defn ^:export navi [context]
  (if (= "admin" (get-in context [:user :role]))
    (admin-view context)
    {}))
