(ns salava.metabadge.ui.routes
  (:require [salava.metabadge.ui.modal :as metabadgemodal]
            [salava.core.ui.layout :as layout]
            [salava.core.i18n :as i18n :refer [t]]
            [salava.core.ui.helper :refer [base-path]]
            [salava.metabadge.ui.my :as my]
            [salava.metabadge.ui.metabadge]
            [salava.metabadge.ui.block]))

(defn ^:export routes [context]
  {(str (base-path context) "/badge") [["/metabadges" my/handler]]})

(def about
  {:heading (t :badge/Badges " / " :metabadge/Milestonebadges)
   :content [:div
             [:p.page-tip [:em (t :metabadge/Milestonebadgespageinfo)]]
             [:p (t :metabadge/Aboutmilestonebadge)]]})


(defn ^:export navi [context]
  {(str (base-path context) "/badge/metabadges") {:weight 45 :title (t :metabadge/Milestonebadges) :site-navi true :breadcrumb (t :badge/Badges " / " :metabadge/Milestonebadges) :about about}})
