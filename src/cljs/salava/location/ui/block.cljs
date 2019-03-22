(ns salava.location.ui.block
  (:require [reagent.core :refer [atom cursor create-class]]
            [reagent.session :as session]
            [salava.core.ui.ajax-utils :as ajax]
            [salava.core.ui.layout :as layout]
            [salava.core.ui.field :as f]
            [salava.core.i18n :refer [t]]
            [salava.core.ui.helper :refer [js-navigate-to path-for private?]]))

(defn badge-settings-content [user-badge-id]
  (create-class
    {:reagent-render
     (fn []
       [:div.row
        [:label.col-md-12.sub-heading (t :location/Location)]
        [:div.col-md-12
         [:div {:id "map-view" :style {:height "400px"}}]]])

     :component-did-mount
     (fn []
       (ajax/GET
         (path-for (str "/obpv1/location/user_badge/" user-badge-id) true)
         {:handler (fn [{:keys [lat lng]}]
                     (let [lat-lng (js/L.latLng. (or lat 65.01) (or lng 25.47))
                           my-marker (js/L.marker. lat-lng)
                           my-map (-> (js/L.map. "map-view")
                                      (.setView lat-lng 5)
                                      (.addLayer (js/L.TileLayer.
                                                   "https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
                                                   #js {:maxZoom 18
                                                        :attribution "Map data © <a href=\"https://openstreetmap.org\">OpenStreetMap</a> contributors"}))
                                      (.on "click" (fn [e]
                                                     (.setLatLng my-marker (.-latlng e))
                                                     (ajax/POST
                                                       (path-for (str "/obpv1/location/user_badge/" user-badge-id) true)
                                                        {:params (.-latlng e)
                                                         :handler (fn [data]
                                                                    (if-not (:success data)
                                                                      (js/alert "Error: failed to save location. Please try again.")))})))
                                      )
                           ]
                       (.addTo my-marker my-map)

                       )
                     )})
       )}))


(defn user-profile-content []
  (create-class
    {:reagent-render
     (fn []
       [:div.row
        [:label.col-xs-12 (t :location/Location)]
        [:div.col-xs-12
         [:div {:id "map-view" :style {:height "600px"}}]]])

     :component-did-mount
     (fn []
       (ajax/GET
         (path-for "/obpv1/location/user" true)
         {:handler (fn [{:keys [lat lng]}]
                     (let [lat-lng (js/L.latLng. (or lat 65.01) (or lng 25.47))
                           my-marker (js/L.marker. lat-lng)
                           my-map (-> (js/L.map. "map-view")
                                      (.setView lat-lng 5)
                                      (.addLayer (js/L.TileLayer.
                                                   "https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
                                                   #js {:maxZoom 18
                                                        :attribution "Map data © <a href=\"https://openstreetmap.org\">OpenStreetMap</a> contributors"}))
                                      (.on "click" (fn [e]
                                                     (.setLatLng my-marker (.-latlng e))
                                                     (ajax/POST
                                                       (path-for "/obpv1/location/user" true)
                                                        {:params (.-latlng e)
                                                         :handler (fn [data]
                                                                    (if-not (:success data)
                                                                      (js/alert "Error: failed to save location. Please try again.")))})))
                                      )
                           ]
                       (.addTo my-marker my-map)

                       )
                     )})
       )}))

(defn ^:export badge_settings [user-badge-id]
  [badge-settings-content user-badge-id])

(defn ^:export user_profile []
  [user-profile-content])
