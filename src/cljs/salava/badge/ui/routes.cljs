(ns salava.badge.ui.routes
  (:require [salava.core.i18n :as i18n :refer [t]]))

(defn show-badge [{:keys [badge-id]}]
  [:p "Got badge id " badge-id])

(defn ^:export routes [context]
  {"/badge" [["/"        (constantly [:p (t :badge/mybadges)])]
             [["/show/" :badge-id] show-badge]
             ["/import/" (constantly [:p (t :badge/importbadges)])]
             ["/upload/" (constantly [:p (t :badge/uploadbadges)])]
             ["/export/" (constantly [:p (t :badge/exportbadges)])]
             ["/stats/"  (constantly [:p (t :badge/badgestats)])]]})


(defn ^:export navi [context]
  {"/badge/"        {:weight 20 :title (t :badge/Badges)}
   "/badge/import/" {:weight 21 :title (t :badge/Import)}
   "/badge/upload/" {:weight 22 :title (t :badge/Upload)}
   "/badge/export/" {:weight 23 :title (t :badge/Export)}
   "/badge/stats/"  {:weight 24 :title (t :badge/Stats)}})
