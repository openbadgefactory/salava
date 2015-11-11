(ns salava.badge.ui.routes
  (:require [salava.core.ui.layout :as layout]
            [salava.badge.ui.my :as my]
            [salava.badge.ui.info :as info]
            [salava.badge.ui.importer :as imp]
            [salava.badge.ui.exporter :as exp]
            [salava.badge.ui.upload :as up]
            [salava.core.i18n :as i18n :refer [t]]))

(defn placeholder [content]
  (fn [site-navi params]
    #(layout/default site-navi content)))


(defn ^:export routes [context]
  {"/badge" [[""          my/handler]
             ["/mybadges" my/handler]
             [["/info/" :badge-id] info/handler]
             ["/import"  imp/handler]
             ["/upload"  up/handler]
             ["/export"  exp/handler]
             ["/stats"   (placeholder [:p (t :badge/Badgestats)])]]})

(defn ^:export navi [context]
  {"/badge"          {:weight 20 :title (t :badge/Badges) :breadcrumb   (t :badge/Badges " / " :badge/Mybadges)}
   "/badge/mybadges" {:weight 20 :title (t :badge/Mybadges) :breadcrumb (t :badge/Badges " / "  :badge/Mybadges)}
   "/badge/import"   {:weight 21 :title (t :badge/Import) :breadcrumb   (t :badge/Badges " / "  :badge/Import)}
   "/badge/upload"   {:weight 22 :title (t :badge/Upload) :breadcrumb   (t :badge/Badges " / "  :badge/Upload)}
   "/badge/export"   {:weight 23 :title (t :badge/Export) :breadcrumb   (t :badge/Badges " / "  :badge/Export)}
   "/badge/stats"    {:weight 24 :title (t :badge/Stats)  :breadcrumb   (t :badge/Badges " / " :badge/Stats)}})

