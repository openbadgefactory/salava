(ns salava.badge.ui.routes
  (:require [salava.core.ui.layout :as layout]
            [salava.core.ui.helper :refer [base-path private? plugin-fun path-for]]
            [salava.badge.ui.my :as my]
            [salava.badge.ui.info :as info]
            [salava.badge.ui.embed :as embed]
            [salava.badge.ui.embed-pic :as embed-pic]
            [salava.badge.ui.importer :as imp]
            [salava.badge.ui.exporter :as exp]
            #_[salava.badge.ui.upload :as up]
            [salava.badge.ui.receive :as rec]
            [salava.core.helper :refer [dump]]
            [salava.badge.ui.stats :as stats]
            [reagent.session :as session]
            [salava.badge.ui.modal :as badgemodal]
            [salava.core.i18n :as i18n :refer [t]]
            [salava.badge.ui.endorsement :as e]
            [salava.badge.ui.block :as block]))

(defn placeholder [content]
  (fn [site-navi params]
    #(layout/default site-navi content)))

(def about
  {:mybadges [:div#about-page
                   [:div
                    [:h5 [:b (t :badge/Whatisabadge)]]
                    (t :badge/AnOpenBadgeIs)]
                   [:div
                    [:h5 [:b (t :badge/Whatisapendingbadge)]]
                    (t :badge/Apendingbadgeis)]
                  [:p.page-tip [:em (t :badge/Aboutmybadges)]]]
   :import [:div
            [:div
             (t :badge/Aboutimportbadge)]]})

(defn ^:export routes [context]
  {(str (base-path context) "/badge") [["" my/handler]
                                       ["/mybadges" my/handler]
                                       [["/info/" :badge-id] info/handler]
                                       [["/info/" :badge-id "/embed"] embed/handler]
                                       [["/info/" :badge-id "/pic/embed"] embed-pic/handler]
                                       [["/info/" :badge-id "/full/embed"] info/embed-handler]
                                       ["/import" imp/handler]
                                       [["/receive/" :badge-id] rec/handler]
                                       #_[["/user/endorsements"] e/handler]]})

(defn ^:export navi [context]
  {(str (base-path context) "/badge") {:weight 20 :title (t :badge/Badges)   :top-navi true  :breadcrumb (t :badge/Badges " / " :badge/Mybadges) :about {:heading (t :badge/Mybadges) :content (:mybadges about)}}
   (str (base-path context) "/badge/mybadges") {:weight 20 :title (t :badge/Mybadges) :site-navi true :breadcrumb (t :badge/Badges " / "  :badge/Mybadges) :about {:heading (t :badge/Mybadges) :content (:mybadges about)}}
   (str (base-path context) "/badge/import") {:weight 21 :title (t :badge/Import) :site-navi false :breadcrumb (t :badge/Badges " / " :badge/Import) :about {:heading (t :badge/ImportBadge) :content (:import about)}}
   (str (base-path context) "/badge/info/\\d+") {:weight 22 :breadcrumb   (t :badge/Badges " / " :badge/Badgeinfo) :site-navi false}})
   ;(str (base-path context) "/badge\\S+") {:weight 22 :breadcrumb   (t :badge/Badges " / " :badge/Badgeinfo) :site-navi false}})
   ;(str (base-path context) "/badge/user/endorsements") {:weight 50 :title (t :badge/Myendorsements) :breadcrumb (t :badge/Badges " / " :badge/Myendorsements) :site-navi true}


(defn ^:export quicklinks []
  [{:title [:p (t :social/Iwanttoseemysbadges)]
    :url (str (path-for "/badge"))
    :weight 4}])
