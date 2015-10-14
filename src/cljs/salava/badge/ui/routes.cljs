(ns salava.badge.ui.routes
  (:require [salava.core.ui.layout :as layout]
            [salava.badge.ui.my :as my]
            [salava.badge.ui.info :as info]
            [salava.badge.ui.importer :as imp]
            [salava.core.i18n :as i18n :refer [t]]))

(defn placeholder [content]
  (fn [site-navi params]
    #(layout/default site-navi content)))


(defn ^:export routes [context]
  {"/badge" [[""                   my/handler]
             [["/info/" :badge-id] info/handler]
             ["/import"  imp/handler]
             ["/upload"  (placeholder [:p (t :badge/Uploadbadges)])]
             ["/export"  (placeholder [:p (t :badge/Exportbadges)])]
             ["/stats"   (placeholder [:p (t :badge/Badgestats)])]]})

(defn ^:export navi [context]
  {"/badge"       {:weight 20  :title (t :badge/badges) :breadcrumb (str (t :badge/badges) " / " (t :badge/mybadges))}
   "/badge/import" {:weight 21 :title (t :badge/import) :breadcrumb (str (t :badge/badges) " / " (t :badge/import))}
   "/badge/upload" {:weight 22 :title (t :badge/upload) :breadcrumb (str (t :badge/badges) " / " (t :badge/upload))}
   "/badge/export" {:weight 23 :title (t :badge/export) :breadcrumb (str (t :badge/badges) " / " (t :badge/export))}
   "/badge/stats"  {:weight 24 :title (t :badge/stats)  :breadcrumb (str (t :badge/badges) " / " (t :badge/stats))}})

