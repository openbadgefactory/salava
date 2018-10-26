(ns salava.badge.ui.routes
  (:require [salava.core.ui.layout :as layout]
            [salava.core.ui.helper :refer [base-path private? plugin-fun]]
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
            [salava.extra.application.ui.application :as app]))

(defn placeholder [content]
  (fn [site-navi params]
    #(layout/default site-navi content)))


(defn ^:export routes [context]
  (let [app-handler (first (plugin-fun (session/get :plugins) "application" "handler"))]
    {(str (base-path context) "/badge") [["" my/handler]
                                         ["/mybadges" my/handler]
                                         [["/info/" :badge-id] info/handler]
                                         [["/info/" :badge-id "/embed"] embed/handler]
                                         [["/info/" :badge-id "/pic/embed"] embed-pic/handler]
                                         ["/import" imp/handler]
                                         [["/receive/" :badge-id] rec/handler]
                                         ["/application" app/handler #_app-handler]]}))

(defn ^:export navi [context]
  (let [app-handler (first (plugin-fun (session/get :plugins) "application" "handler"))
        navi {(str (base-path context) "/badge") {:weight 20 :title (t :badge/Badges)   :top-navi true  :breadcrumb (t :badge/Badges " / " :badge/Mybadges)}
              (str (base-path context) "/badge/mybadges") {:weight 20 :title (t :badge/Mybadges) :site-navi true :breadcrumb (t :badge/Badges " / "  :badge/Mybadges)}
              (str (base-path context) "/badge/import") {:weight 22 :title (t :badge/Import) :site-navi false :breadcrumb (t :badge/Badges " / " :badge/Import)}
              (str (base-path context) "/badge/info/\\d+") {:breadcrumb   (t :badge/Badges " / " :badge/Badgeinfo)}}]
    ;(if app-handler
      (merge navi {(str (base-path context) "/badge/application") {:weight 45 :title (t :extra-application/Application)  :site-navi true :breadcrumb (t :badge/Badges  " / " :extra-application/Application)}})
  )
    );)

