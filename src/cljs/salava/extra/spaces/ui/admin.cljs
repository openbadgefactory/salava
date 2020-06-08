(ns salava.spaces.ui.admin
 (:require
  [reagent.core :refer [cursor atom]]
  [reagent.session :as session]
  [salava.core.ui.helper :refer [path-for]]
  [salava.core.ui.ajax-utils :as ajax]
  [salava.core.ui.layout :as layout]))

(defn init-data [state]
 (let [id (session/get-in [:user :current-space :id])]
  (ajax/POST
   (path-for (str "/obpv1/spaces/statistics/" id))
   {:handler (fn [data]
               (swap! state assoc :space data))})))

(defn manage [])

(defn edit [])

(defn content [state]
  [:div "some content"])

(defn handler [site-navi]
  (let [state (atom {:space nil})]
    (init-data space)
    (fn []
      (layout/default site-navi [content state]))))
