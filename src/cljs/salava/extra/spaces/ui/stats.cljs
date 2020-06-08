(ns salava.extra.spaces.ui.stats
  (:require
   [reagent.core :refer [atom cursor]]
   [reagent.session :as session]
   [salava.core.ui.ajax-utils :as ajax]
   [salava.core.ui.helper :refer [path-for]]
   [salava.core.ui.layout :as layout]))

(defn init-stats [state])

(defn content [state]
 [:div
  ""])

(defn handler [site-navi]
  (let [state (atom {})]
   (init-stats state)
   (fn []
     (layout/default site-navi [content state]))))
