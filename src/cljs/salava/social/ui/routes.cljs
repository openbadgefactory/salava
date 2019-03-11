(ns salava.social.ui.routes
   (:require [salava.core.ui.layout :as layout]
             [salava.core.i18n :as i18n :refer [t]]
             [salava.social.ui.connections :as c]
             [salava.social.ui.stream :as s]
             [salava.social.ui.dashboard :as d]
             [salava.core.ui.helper :refer [base-path]]
             [salava.badge.ui.stats :as stats]))

(defn ^:export routes [context]
  {(str (base-path context) "/social") [["" d/handler]
                                        ;["" s/handler]
                                        ;["/stream" s/handler]
                                        ["/connections" c/handler]
                                        ["/stats" stats/handler]]})

(defn ^:export navi [context]
  {(str (base-path context) "/social")                 {:weight 1 :title (t :social/Social) :top-navi true :breadcrumb (t :social/Social " / " "Dashboard" #_:social/Stream)}
   (str (base-path context) "/social/stream")         {:weight 11 :title (t :social/Stream) :site-navi false :breadcrumb (t :social/Social " / " :social/Stream)}
   ;(str (base-path context) "/social/connections")         {:weight 12 :title (t :social/Connections) :site-navi true :breadcrumb (t :social/Social " / " :social/Connections)}
   ;(str (base-path context) "/social/stats") {:weight 21 :title (t :badge/Stats) :site-navi true :breadcrumb (t :social/Social " / " :badge/Stats)}
   })
