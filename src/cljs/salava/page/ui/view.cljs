(ns salava.page.ui.view
  (:require [reagent.core :refer [atom]]
            [salava.core.ui.ajax-utils :as ajax]
            [salava.core.ui.layout :as layout]
            [salava.core.i18n :refer [t]]
            [salava.core.helper :refer [dump]]
            [salava.page.ui.helper :as ph]))

(defn content [state]
  [:div {:id "page-view"}
   [ph/view-page (:page @state)]])

(defn init-data [state id]
  (ajax/GET
    (str "/obpv1/page/view/" id)
    {:handler (fn [data]
                (swap! state assoc :page data))}))

(defn handler [site-navi params]
  (let [id (:page-id params)
        state (atom {:page {}
                     :page-id id})]
    (init-data state id)
    (fn []
      (layout/default site-navi (content state)))))
