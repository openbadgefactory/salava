(ns salava.connections.ui.routes
  (:require [salava.core.ui.layout :as layout]
            [salava.core.i18n :as i18n :refer [t]]
            [salava.connections.ui.badge :as b]
            [salava.core.ui.helper :refer [base-path]]
            [salava.badge.ui.stats :as stats]
            [salava.badge.ui.endorsement :as e]))

(defn ^:export routes [context]
  {(str (base-path context) "/connections") [["" b/handler]
                                             ["/badge" b/handler]
                                             ["/stats" stats/handler]
                                             ["/endorsement" e/handler]]})

(defn about []
  {:badge {:heading (t :social/Connections " / " :badge/Badges)
           :content [:div
                     [:p.page-tip (t :connections/Badgeconnectionsinfo)]
                     [:div
                      [:h5 [:b (t :badge/Badges)]]
                      (t :connections/Aboutbadgeconnections)]
                     [:div
                      [:h5 [:b (t :badge/Issuers)]]
                      (t :connections/Aboutissuerconnections)]]}

   :stats {:heading (t :social/Connections " / " :badge/Stats)
           :content [:div
                     [:p.page-tip (t :badge/Aboutbadgestatistics)]
                     [:div
                      [:h5 [:b (t :badge/Howcanigetcongratulated)]]
                      (t :badge/Aboutbadgecongratulations)]]}

   :endorsement {:heading (t :social/Connections " / " :connections/Endorsements)
                 :content [:div
                           [:p.page-tip (t :connections/Aboutendorsementspage)]
                           [:div
                            [:p (t :badge/Aboutbadgeendorsement)]
                            [:p (t :badge/Aboutendorsementrequests)]]]}})


(defn ^:export navi [context]
  {(str (base-path context) "/connections")                 {:weight 45 :title (t :social/Connections) :top-navi true :breadcrumb (t :social/Connections " / " :badge/Badges) :about (:badge (about))}
   (str (base-path context) "/connections/badge")         {:weight 61 :title (t :badge/Badges) :site-navi true :breadcrumb (t :social/Connections " / " :badge/Badges) :about (:badge (about))}
  ; (str (base-path context) "/connections/users")         {:weight 63 :title (t :connections/Users) :site-navi true :breadcrumb (t :social/Connections " / " :connections/Users)}
   (str (base-path context) "/connections/stats")          {:weight 64 :title (t :badge/Stats) :site-navi true :breadcrumb (t :social/Connections " / " :badge/Stats) :about (:stats (about))}
   (str (base-path context) "/connections/endorsement") {:weight 62 :title (t :connections/Endorsements) :breadcrumb (t :social/Connections " / " :connections/Endorsements) :site-navi true :about (:endorsement (about))}})
   ;(str (base-path context) "/social/stats") {:weight 21 :title (t :badge/Stats) :site-navi true :breadcrumb (t :social/Social " / " :badge/Stats)}
