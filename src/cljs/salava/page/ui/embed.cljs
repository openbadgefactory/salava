(ns salava.page.ui.embed
  (:require [reagent.core :refer [atom cursor]]
            [ajax.core :as ajax]
            [salava.core.ui.layout :as layout]
            [salava.core.i18n :refer [t]]
            [salava.core.ui.helper :refer [navigate-to path-for]]
            [salava.page.ui.helper :as ph]             ))

(defn page-content [page state]
  (let [show-link-or-embed-atom (cursor state [:show-link-or-embed-code])]
    [:div {:id "page-view"}
     [ph/view-page page]]))

(defn content [state]
  (let [page (:page @state)]
    [:div {:id "page-container"}
       [page-content page state]]))

(defn init-data [state id]
  (ajax/GET
    (path-for (str "/obpv1/page/view/" id))
    {:response-format :json
     :keywords?       true
     :handler         (fn [data]
                        (swap! state assoc :page (:page data) :ask-password (:ask-password data)))
     :error-handler   (fn [{:keys [status status-text]}]
                        )}))

(defn handler [site-navi params]
  (let [id (:page-id params)
        state (atom {:page {}
                     :page-id id
                     :show-link-or-embed-code nil})]
    (init-data state id)
    (fn []
      (layout/embed-page (content state)))))
