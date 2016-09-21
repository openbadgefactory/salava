(ns salava.extra.application.ui.routes
   (:require [salava.core.ui.layout :as layout]
             [salava.core.i18n :as i18n :refer [t]]
             [salava.core.ui.helper :refer [base-path]]
             [salava.extra.application.ui.application :as a]))

(defn placeholder [content]
  (fn [site-navi params]
    #(layout/default site-navi content)))

(defn ^:export routes [context]
  {(str (base-path context) "/gallery") [
                                       ["/application" a/handler]]})

(defn ^:export navi [context]
  {(str (base-path context) "/gallery/application")         {:weight 45 :title (t :extra-application/Application)  :site-navi true :breadcrumb (t :gallery/Gallery " / " :extra-application/Application)}})

