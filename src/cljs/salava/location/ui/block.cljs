(ns salava.location.ui.block
  (:require [reagent.core :refer [atom cursor create-class]]
            [reagent.session :as session]
            [salava.core.ui.ajax-utils :as ajax]
            [salava.core.ui.layout :as layout]
            [salava.core.ui.field :as f]
            [salava.core.i18n :refer [t]]
            [salava.core.ui.helper :refer [js-navigate-to path-for private?]]))

(def tile-url "https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png")

(def tile-opt
  (clj->js
    {:maxZoom 14
     :attribution "Map data © <a href=\"https://openstreetmap.org\">OpenStreetMap</a> contributors"}))

(defn midpoint [items]
  {:lat (/ (apply + (map :lat items)) (count items))
   :lng (/ (apply + (map :lng items)) (count items))})


(defn gallery-badge-content [badge-id visible]
  (create-class
    {:reagent-render
     (fn []
       [:div.row {:style {:display (if @visible "block" "none")}}
        [:div.col-md-12
         [:div {:id (str "map-view-badge-" badge-id) :style {:height "400px" :margin "20px 0"}}]]])

     :component-did-mount
     (fn []
       (js/window.setTimeout
         (fn []
           (ajax/GET
             (path-for (str "/obpv1/location/explore/badge/" badge-id) true)
             {:handler (fn [data]
                         (if (seq (:badges data))
                           (let [lat-lng (js/L.latLng. (clj->js (midpoint (:badges data))))
                                 my-map (-> (js/L.map. (str "map-view-badge-" badge-id))
                                            (.setView lat-lng 8)
                                            (.addLayer (js/L.TileLayer. tile-url tile-opt)))]
                             (doseq [b (:badges data)]
                               (-> (js/L.latLng. (:lat b) (:lng b))
                                   js/L.marker.
                                   (.addTo my-map))))
                           (reset! visible false))
                         )})) 300)
       )}))


(defn badge-settings-content [user-badge-id visible]
  (create-class
    {:reagent-render
     (fn []
       [:div.row {:style {:display (if @visible "block" "none")}}
        [:label.col-md-12.sub-heading (t :location/Location)]
        [:div.col-md-12
         [:div {:id "map-view-badge" :style {:height "400px" :margin "20px 0"}}]]])

     :component-did-mount
     (fn []
       (js/window.setTimeout
         (fn []
           (ajax/GET
             (path-for (str "/obpv1/location/user_badge/" user-badge-id) true)
             {:handler (fn [{:keys [lat lng]}]
                         (if (and lat lng)
                           (let [lat-lng (js/L.latLng. lat lng)
                                 my-marker (js/L.marker. lat-lng)
                                 my-map (-> (js/L.map. "map-view-badge")
                                            (.setView lat-lng 5)
                                            (.addLayer (js/L.TileLayer. tile-url tile-opt))
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
                             (.addTo my-marker my-map))
                           (reset! visible false)))})) 300)
       )}))


(defn user-profile-content [user-id visible]
  (create-class
    {:reagent-render
     (fn []
       [:div.row {:style {:display (if @visible "block" "none")}}
        [:div.col-md-12
         [:div {:id (str "map-view-user-" user-id) :style {:height "400px" :margin "20px 0"}}]]])

     :component-did-mount
     (fn []
       (js/window.setTimeout
         (fn []
           (ajax/GET
             (path-for (str "/obpv1/location/user/" user-id) true)
             {:handler (fn [{:keys [lat lng]}]
                         (if (and lat lng)
                           (let [lat-lng (js/L.latLng. lat lng)
                                 my-marker (js/L.marker. lat-lng)
                                 my-map (-> (js/L.map. (str "map-view-user-" user-id))
                                            (.setView lat-lng 8)
                                            (.addLayer (js/L.TileLayer. tile-url tile-opt)))]
                             (.addTo my-marker my-map))
                           (reset! visible false)))})) 300)
       )}))

(defn- user-settings-map [{:keys [lat lng]}]
  (let [lat-lng (js/L.latLng. lat lng)
        my-marker (js/L.marker. lat-lng)
        my-map (-> (js/L.map. "map-view-user")
                   (.setView lat-lng 5)
                   (.addLayer (js/L.TileLayer. tile-url tile-opt))
                   (.on "click" (fn [e]
                                  (.setLatLng my-marker (.-latlng e))
                                  (ajax/POST
                                    (path-for "/obpv1/location/user" true)
                                    {:params (.-latlng e)
                                     :handler (fn [data]
                                                (if-not (:success data)
                                                  (js/alert "Error: failed to save location. Please try again.")))}))))]
    (.addTo my-marker my-map)))

(defn user-edit-profile-content [state]
  (create-class
    {:reagent-render
     (fn []
       [:div.row
        [:label.col-xs-12 (t :location/Location)]
        [:div.col-xs-12
         [:div
          [:div.checkbox
           [:label
            [:input {:name      "enabled"
                     :type      "checkbox"
                     :value     1
                     :on-change (fn [e]
                                  (if (.-target.checked e)
                                    (do
                                      (swap! state assoc :enabled true)
                                      (ajax/POST (path-for "/obpv1/location/user" true) {:params (:default @state)}))
                                    (do
                                      (swap! state assoc :enabled false)
                                      (swap! state assoc :public  false)
                                      (ajax/POST (path-for "/obpv1/location/public" true) {:params {:public false}})
                                      (ajax/POST (path-for "/obpv1/location/user" true) {:params {:lat nil :lng nil}}))))
                     :checked (:enabled @state)}]
            (t :location/LocationEnabled)]
          [:p.help-block (t :location/LocationEnabledInfo)]]

          [:div.checkbox {:style {:display (if (:enabled @state) "block" "none")}}
           [:label
            [:input {:name      "public"
                     :type      "checkbox"
                     :value     1
                     :on-change (fn [e]
                                  (let [public? (.-target.checked e)]
                                    (swap! state assoc :public public?)
                                    (ajax/POST (path-for "/obpv1/location/public" true) {:params {:public public?}})))
                     :checked (:public @state)}]
            (t :location/LocationPublic)]
           [:p.help-block (t :location/LocationPublicInfo)]]]

         [:div {:id "map-view-user" :style {:display (if (:enabled @state) "block" "none") :height "600px" :margin "20px 0"}}]
         ]])

     :component-did-mount
     (fn []
       (ajax/GET
         (path-for "/obpv1/location/user" true)
         {:handler (fn [data]
                     (user-settings-map (or (:enabled data) (:country data)))
                     (swap! state assoc :public (:public data))
                     (swap! state assoc :default (or (:enabled data) (:country data)))
                     (when-not (:enabled data)
                       (swap! state assoc :enabled false)))
          })
       )}))

(defn ^:export gallery_badge [badge-id]
  (let [visible (atom true)]
    [gallery-badge-content badge-id visible]))


(defn ^:export badge_settings [user-badge-id]
  (let [visible (atom true)]
    [badge-settings-content user-badge-id visible]))


(defn ^:export user_profile [user-id]
  (let [visible (atom true)]
    [user-profile-content user-id visible]))


(defn ^:export user_edit_profile []
  (let [state (atom {:enabled true :public false :default {:lat nil :lng nil}})]
    [user-edit-profile-content state]))
