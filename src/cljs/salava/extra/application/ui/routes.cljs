(ns salava.extra.application.ui.routes
   (:require [salava.core.ui.layout :as layout]
             [salava.core.i18n :as i18n :refer [t]]
             [salava.core.ui.helper :refer [base-path path-for]]
             [salava.extra.application.ui.application :as a]
    [salava.extra.application.ui.embed :as embed]
    [salava.extra.application.ui.modal :as advertmodal]))

(defn placeholder [content]
  (fn [site-navi params]
    #(layout/default site-navi content)))

(defn ^:export routes [context]
  {(str (base-path context) "/badge") [["/application" a/handler]
                                       [["/application/"[#"\d+" :user-id] "/embed"] embed/handler]]})
(defn about []
  {:heading (t :badge/Badges " / " :extra-application/Application)
   :content [:p.page-tip (t :extra-application/AboutEarnbadges)]})

(defn ^:export navi [context]
  {(str (base-path context) "/badge/application") {:weight 45 :title (t :extra-application/Application)  :site-navi true :breadcrumb (t :badge/Badges " / " :extra-application/Application) :about (about)}})

(defn ^:export quicklinks []
  [{:title [:p (t :social/Iwanttoearnnewbadges)]
    :url (str (path-for "/badge/application"))
    :weight 5}])
