(ns salava.extra.application.ui.application
  (:require [reagent.core :refer [atom]]
            [markdown.core :refer [md->html]]
            [salava.core.ui.ajax-utils :as ajax]
            [salava.core.ui.layout :as layout]
            [salava.core.ui.helper :refer [path-for unique-values]]
            [reagent.session :as session]
            [salava.core.helper :refer [dump]]
            [salava.core.i18n :as i18n :refer [t]]))



(defn content [state]
  (let [user-lng (if (= "fi" (session/get-in [:user :language])) "fi" "en")
        applications (:applications @state)
        filtered-applications (filter #(= user-lng (:language %)) applications)]  
    [:div
     (doall
      (for [item filtered-applications]
        [:div {:key (hash (:iframe item)) :class "pull-left application-element" :dangerouslySetInnerHTML {:__html (md->html (:iframe item))}}]))]))

(defn init-data [state]
  (ajax/GET
   (path-for "/obpv1/application/")
   {:handler (fn [applications]
               (swap! state assoc :applications applications))}))

(defn handler [site-navi]
  (let [state (atom {:applications []})]
    (init-data state)
    (fn []
      (layout/default site-navi (content state)))))
