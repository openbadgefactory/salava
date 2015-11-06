(ns salava.page.ui.preview
  (:require [reagent.core :refer [atom cursor]]
            [reagent.session :as session]
            [clojure.walk :as walk :refer [keywordize-keys]]
            [ajax.core :as ajax]
            [salava.core.ui.layout :as layout]
            [salava.core.i18n :refer [t]]
            [salava.core.helper :refer [dump]]
            [salava.page.ui.helper :as ph]))


(defn content [state]
  (let [{:keys [id name]} (:page @state)]
    [:div {:id "page-preview"}
     [ph/edit-page-header (str (t :page/Preview) ": " name)]
     [ph/edit-page-buttons id :preview]
     [ph/view-page (:page @state)]]))

  (defn init-data [state id]
    (ajax/GET
      (str "/obpv1/page/view/" id)
      {:handler (fn [data]
                  (let [data-with-kws (keywordize-keys data)]
                    (swap! state assoc :page data-with-kws)))}))

  (defn handler [site-navi params]
    (let [id (:page-id params)
          state (atom {:page {}})]
      (init-data state id)
      (fn []
        (layout/default site-navi (content state)))))
