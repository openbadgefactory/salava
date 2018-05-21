(ns salava.core.ui.error
  (:require [reagent.core :refer [atom cursor]]
            [salava.core.ui.layout :as layout]
            [salava.core.helper :refer [dump]]
            [salava.core.i18n :refer [t]]
            [salava.core.ui.helper :refer [path-for]])
  )

(defn error-not-found []
  [:div
   [:h4 "404 Not Found"]])

(defn error-content []
  [:div
   [:h4 (t :core/Errorpage)]])


(defn content [params]
  (let []
    [:div
     [:h2 (str "Dammit, something went wrong! Error - " (:status params))]
     [:div [:i {:class "fa fa-bolt" :aria-hidden "true"}]
      "Maybe problems was ours maybe it wasn't.. Well better luck next time "
      [:i {:class "fa fa-bolt" :aria-hidden "true"}]
      ]]))

(defn init-data [state id]
  )

(defn handler [site-navi params]
  (let [state (atom {})]                                        
    (fn []
      (layout/default-no-sidebar site-navi (content params)))))
