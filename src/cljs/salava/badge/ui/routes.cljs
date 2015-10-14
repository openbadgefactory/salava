(ns salava.badge.ui.routes
  (:require [salava.core.ui.layout :as layout]
            [salava.badge.ui.my :as my]
            [salava.badge.ui.info :as info]
            [salava.core.i18n :as i18n :refer [t]]))

(defn placeholder [content]
  (fn [site-navi params]
    #(layout/default site-navi content)))


(defn ^:export routes [context]
  {"/badge" [[""                   my/handler]
             [["/info/" :badge-id] info/handler]
             ["/import"  (placeholder [:p (t :badge/Importbadges)])]
             ["/upload"  (placeholder [:p (t :badge/Uploadbadges)])]
             ["/export"  (placeholder [:p (t :badge/Exportbadges)])]
             ["/stats"   (placeholder [:p (t :badge/Badgestats)])]]})

(defn ^:export navi [context]
  {"/badge/"       {:weight 20 :title (t :badge/Badges)}
   "/badge/import" {:weight 21 :title (t :badge/Import)}
   "/badge/upload" {:weight 22 :title (t :badge/Upload)}
   "/badge/export" {:weight 23 :title (t :badge/Export)}
   "/badge/stats"  {:weight 24 :title (t :badge/Stats)}})

(defn ^:export heading [context]
  {"/badge/" "Badges / My badges"
   "/badge/import/" "Badges / Import"
   "/badge/upload/" "Badges / Upload"
   "/badge/export/" "Badges / Export"
   "/badge/stats/" "Badges / Stats"})
