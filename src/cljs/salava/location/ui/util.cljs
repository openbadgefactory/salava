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

(def badge-icon (js/L.Icon.Default.))

(def seed (atom 3))

(defn- fake-rand []
  (let [x (* (js/Math.sin (swap! seed inc)) 10000)]
    (- x (js/Math.floor x))))

(defn noise
  ([v] (noise v 1))
  ([v m]
   (-> v (+ (* (fake-rand) 0.003 m)) (- (* (fake-rand) 0.003 m)))))

(defn noise-seed []
  (reset! seed 3))