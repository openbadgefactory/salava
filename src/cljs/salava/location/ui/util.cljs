(ns salava.location.ui.util
  (:require [reagent.core :refer [atom]]))

(def map-opt (clj->js {:maxBounds [[-90 -180] [90 180]]
                       :worldCopyJump true}))

(def tile-url "https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png")

(def tile-opt
  (clj->js
    {:maxZoom 15
     :minZoom 3
     :attribution "Map data © <a href=\"https://openstreetmap.org\">OpenStreetMap</a> contributors"}))

(def user-icon  (js/L.divIcon. (clj->js {:className "location-icon-user" :iconSize [36 36] :html "<i class=\"fa fa-user-circle fa-3x\"></i>"})))

(def user-icon-ro (js/L.divIcon. (clj->js {:className "location-icon-user location-icon-readonly" :iconSize [36 36] :html "<i class=\"fa fa-user-circle fa-3x\"></i>"})))

(def badge-icon (js/L.Icon.Default.))

(def badge-icon-ro (js/L.Icon.Default. (clj->js {:className "location-icon-readonly"})))
