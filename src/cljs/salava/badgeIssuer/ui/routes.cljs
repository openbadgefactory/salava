(ns salava.badgeIssuer.ui.routes
  (:require
   [salava.badgeIssuer.ui.creator :as creator]
   [salava.core.i18n :refer [t]]
   [salava.core.ui.helper :refer [base-path]]))

(defn ^:export routes [context]
  {(str (base-path context) "/badge/issuer") [["" creator/handler]]})

(defn ^:export navi [context]
  {(str (base-path context) "/badge/issuer") {:weight 100 :title (t :badgeIssuer/IssueBadges) :site-navi true :breadcrumb (t :badge/Badge " / " :badgeIssuer/IssuerBadges)}})
