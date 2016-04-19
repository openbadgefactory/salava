(ns salava.page.ui.preview
  (:require [reagent.core :refer [atom cursor]]
            [salava.core.ui.ajax-utils :as ajax]
            [salava.core.ui.layout :as layout]
            [salava.core.ui.helper :refer [navigate-to path-for]]
            [salava.core.i18n :refer [t]]
            [salava.core.helper :refer [dump]]
            [salava.page.ui.helper :as ph]))


(defn content [state]
  (let [{:keys [id name]} (:page @state)]
    [:div {:id "page-preview"}
     [ph/edit-page-header (t :page/Preview ": " name) ]
     [ph/edit-page-buttons id :preview (fn [next-url] (navigate-to next-url))]
     [ph/view-page (:page @state)]]))

(defn init-data [state id]
  (ajax/GET
    (path-for (str "/obpv1/page/" id))
    {:handler (fn [data]
                (swap! state assoc :page data))}))

(defn handler [site-navi params]
  (let [id (:page-id params)
        state (atom {:page {}})]
    (init-data state id)
    (fn []
      (layout/default site-navi (content state)))))
