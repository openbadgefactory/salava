(ns salava.badgeIssuer.ui.routes
  (:require
   [salava.badgeIssuer.ui.block]
   [salava.badgeIssuer.ui.creator :as creator]
   [salava.badgeIssuer.ui.criteria :as criteria]
   [salava.badgeIssuer.ui.helper :as h]
   [salava.badgeIssuer.ui.modal]
   [salava.badgeIssuer.ui.my :as my]
   [salava.badgeIssuer.ui.util :as u]
   [salava.core.i18n :refer [t]]
   [salava.core.ui.helper :refer [base-path path-for]]))

(defn ^:export routes [context]
  {(str (base-path context) "/badge/selfie") [["" my/handler]
                                              ["/create" creator/handler]
                                              [["/create/" :id] creator/handler]]
   (str (base-path context) "/selfie") [[["/criteria/" :id] criteria/handler]]})

(def about
  {:selfie {:heading (t :badge/Badges " / " :badgeIssuer/IssueBadges)
            :content [:div
                      [:p.page-tip [:em (t :badgeIssuer/Issuebadgespageinfo)]]
                      [:div
                       [:p (t :badgeIssuer/Whyshouildicreatebadges)]
                       [:p (t :badge/AnOpenBadgeIs)]]]}


   :create {:heading (t :badgeIssuer/Createselfiebadge)
            :content [:div
                      [:p (t :badgeIssuer/Createnewbadgeinfo)]
                      [:p (t :badgeIssuer/Issuebadgeinfo)]]}})

(defn ^:export navi [context]
  {(str (base-path context) "/badge/selfie") {:weight 100 :title (t :badgeIssuer/IssueBadges) :site-navi true :breadcrumb (t :badge/Badges " / " :badgeIssuer/IssueBadges) :about (:selfie about)}
   (str (base-path context) "/badge/selfie/create") {:weight 101 :title (t :badgeIssuer/Createselfiebadges) :site-navi false :breadcrumb (t :badge/Badges " / " :badgeIssuer/Createselfiebadge) :about (:create about)}})
   ;(str (base-path context) "/badge/selfie/create/\\S+")  {:breadcrumb (t :badge/Badges " / " :badgeIssuer/Editselfiebadge)}})

(defn ^:export quicklinks []
  [{:title [:p (t :badgeIssuer/Iwanttocreateandissue)]
    :url (str (path-for "/badge/selfie"))
    :weight 10}])
