(ns salava.location.ui.explore
  (:require [reagent.core :refer [atom cursor create-class]]
            [reagent.session :as session]
            [reagent-modals.modals :as m]
            [ajax.core :as ajax]
            [salava.core.ui.modal :as mo]
            [salava.core.ui.helper :refer [js-navigate-to path-for private? plugin-fun]]
            [salava.core.ui.layout :as layout]
            [salava.core.i18n :refer [t translate-text]]))


(defn- set-markers [my-map data]
  (doseq [b (:badges data)]
    (-> (js/L.latLng. (:lat b) (:lng b))
        js/L.marker.
        (.on "click" (fn [e]
                       (mo/open-modal [:gallery :badges] {:badge-id (:badge_id b)})))
        (.addTo my-map))))


(defn content []
  (create-class
    {:reagent-render
     (fn []
       [:div.row
        [:div.col-md-12
         [m/modal-window]
         [:div {:id "map-view" :style {:height "700px"}}]]])

     :component-did-mount
     (fn []
       (ajax/GET
         (path-for "/obpv1/location/user" true)
         {:handler
          (fn [{:keys [lat lng]}]
            (ajax/GET
               (path-for "/obpv1/location/explore" true)
               {:handler
                (fn [data]
                  (let [lat-lng (js/L.latLng. lat lng)
                        my-map (-> (js/L.map. "map-view")
                                   (.setView lat-lng 8)
                                   (.addLayer (js/L.TileLayer.
                                                "https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
                                                #js {:maxZoom 18
                                                     :attribution "Map data © <a href=\"https://openstreetmap.org\">OpenStreetMap</a> contributors"})))]

                    (set-markers my-map data)))
                }))
          }))
     }))

(defn handler [site-navi]
  (fn []
    (layout/default site-navi [content])))
