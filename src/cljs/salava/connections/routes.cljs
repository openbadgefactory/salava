(ns salava.connections.ui.routes
  (:require [salava.core.ui.layout :as layout]
            [salava.core.i18n :as i18n :refer [t]]
            [salava.connections.ui.badges :as b]
            [salava.core.ui.helper :refer [base-path]]
            [salava.badge.ui.stats :as stats]
            [salava.badge.ui.endorsement :as e]))

(defn ^:export routes [context]
  {(str (base-path context) "/connections") [["" b/handler]
                                             ["/badges" b/handler]
                                             ;["/connections" c/handler]
                                             ["/stats" stats/handler]
                                             ["/endorsement" e/handler] ]})

(defn ^:export navi [context]
  {(str (base-path context) "/connections")                 {:weight 45 :title (t :social/Connections) :top-navi true :breadcrumb (t :social/Connections " / " :badge/Badges)}
   (str (base-path context) "/connections/badges")         {:weight 61 :title (t :badge/Badges) :site-navi true :breadcrumb (t :social/Connections " / " :badge/Badges)}
   (str (base-path context) "/connections/people")         {:weight 62 :title (t :social/People) :site-navi true :breadcrumb (t :social/Connections " / " :social/People)}
   (str (base-path context) "/connections/stats")          {:weight 64 :title (t :badge/Stats) :site-navi true :breadcrumb (t :social/Connections " / " :badge/Stats)}
   (str (base-path context) "/connections/endorsement") {:weight 63 :title (t :badge/Myendorsements) :breadcrumb (t :badge/Badges " / " :badge/Myendorsements) :site-navi true}


   ;(str (base-path context) "/social/stats") {:weight 21 :title (t :badge/Stats) :site-navi true :breadcrumb (t :social/Social " / " :badge/Stats)}
   })
