(ns salava.badge.ui.routes
  (:require [salava.badge.ui.main :as main]
            [salava.badge.ui.info :as info]
            [salava.core.i18n :as i18n :refer [t]]))

(defn foo [params]
  (info/init params))

(defn ^:export routes [context]
  {"/badge" [[""       (main/my-badges)]
             [["/info/" :badge-id] foo]
             ["/import" (constantly [:p (t :badge/Importbadges)])]
             ["/upload" (constantly [:p (t :badge/Uploadbadges)])]
             ["/export" (constantly [:p (t :badge/Exportbadges)])]
             ["/stats"  (constantly [:p (t :badge/Badgestats)])]]})

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
