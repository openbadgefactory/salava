(ns salava.badgeIssuer.ui.routes
  (:require
   [salava.badgeIssuer.ui.creator :as creator]
   [salava.badgeIssuer.ui.helper :as h]
   [salava.badgeIssuer.ui.modal]
   [salava.badgeIssuer.ui.my :as my]
   [salava.badgeIssuer.ui.util :as u]
   [salava.core.i18n :refer [t]]
   [salava.core.ui.helper :refer [base-path]]))

(defn ^:export routes [context]
  {(str (base-path context) "/badge/selfie") [["" my/handler]
                                              ["/create" creator/handler]
                                              [["/create/" :id] creator/handler]]})

(defn ^:export navi [context]
  {(str (base-path context) "/badge/selfie") {:weight 100 :title (t :badgeIssuer/IssueBadges) :site-navi true :breadcrumb (t :badge/Badge " / " :badgeIssuer/IssuerBadges)}
   (str (base-path context) "/badge/selfie/create") {:weight 101 :title (t :badgeIssuer/Createselfiebadges) :site-navi false :breadcrumb (t :badge/Badge " / " :badgeIssuer/IssuerBadges)}})
