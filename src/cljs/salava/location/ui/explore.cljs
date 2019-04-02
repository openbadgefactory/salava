(ns salava.location.ui.explore
  (:require [reagent.core :refer [atom cursor create-class]]
            [reagent.session :as session]
            [reagent-modals.modals :as m]
            [ajax.core :as ajax]
            [salava.core.ui.modal :as mo]
            [salava.core.ui.helper :refer [js-navigate-to path-for private? plugin-fun]]
            [salava.core.ui.layout :as layout]
            [salava.core.i18n :refer [t translate-text]]))

(def map-opt (clj->js {:maxBounds [[-90 -180] [90 180]]
                       :worldCopyJump true}))

(def tile-url "https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png")

(def tile-opt
  (clj->js
    {:maxZoom 15
     :minZoom 3
     :attribution "Map data © <a href=\"https://openstreetmap.org\">OpenStreetMap</a> contributors"}))

(defn- get-markers [kind my-map layer-group ]
  (let [bounds (.getBounds my-map)
        minll (.getSouthWest bounds)
        maxll (.getNorthEast bounds)
        click-cb (case kind
                   "users"
                   (fn [u] #(mo/open-modal [:user :profile] {:user-id (-> u :id .-rep)}))
                   "badges"
                   (fn [b] #(mo/open-modal [:gallery :badges] {:badge-id (:badge_id b)})))]
    (ajax/GET
      (path-for (str "/obpv1/location/explore/" kind) false)
      {:params {:max_lat (.-lat maxll) :max_lng (.-lng maxll)
                :min_lat (.-lat minll) :min_lng (.-lng minll)}
       :handler
       (fn [data]
         (.clearLayers layer-group)
         (doseq [item (get data (keyword kind))]
           (.addLayer
             layer-group
             (-> (js/L.latLng. (:lat item) (:lng item))
                 js/L.marker.
                 (.on "click" (click-cb item))))))
       })))

(defn map-view []
  (create-class
    {:reagent-render
     (fn []
       [:div.row
        [:div.col-md-12
         [m/modal-window]

         [:div.row
          [:div.col-md-12
           [:label.radio-inline
            [:input {:name "map-type"
                     :type "radio"
                     :value "users"
                     :default-checked true}]
            (t :location/ShowUsers)]

           [:label.radio-inline
            [:input {:name "map-type"
                     :type "radio"
                     :value "badges"}]
            (t :location/ShowBadges)]]]

         [:div {:id "map-view" :style {:height "700px" :margin "20px 0"}}]]])

     :component-did-mount
     (fn []
       (let [timer (atom 0)
             layer-group (js/L.layerGroup. (clj->js []))
             lat-lng (js/L.latLng. 40 -20)
             my-map (-> (js/L.map. "map-view" map-opt)
                        (.setView lat-lng 3)
                        (.addLayer (js/L.TileLayer. tile-url tile-opt)))]

         (.addTo layer-group my-map)

         (.on my-map "moveend"
              (fn []
                (js/clearTimeout @timer)
                (reset! timer
                        (js/setTimeout
                          #(get-markers (.val (js/jQuery "input[name=map-type]:checked")) my-map layer-group)
                          1000))))

         (.on (js/jQuery "input[name=map-type]") "change"
              (fn [e]
                (get-markers (.-target.value e) my-map layer-group)))

         (get-markers "users" my-map layer-group)
         ))
     }))

(defn handler [site-navi]
  (let [visible (atom "users")]
    (fn []
      (layout/default site-navi [map-view]))))
