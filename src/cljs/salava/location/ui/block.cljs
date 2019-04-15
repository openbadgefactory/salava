(ns salava.location.ui.block
  (:require [reagent.core :refer [atom cursor create-class]]
            [reagent.session :as session]
            [salava.core.ui.ajax-utils :as ajax]
            [salava.core.ui.layout :as layout]
            [salava.core.ui.field :as f]
            [salava.core.i18n :refer [t]]
            [salava.core.ui.helper :refer [js-navigate-to path-for private?]]
            [salava.location.ui.util :as lu]
            [salava.core.ui.modal :as mo]
            ))


(defn put-handler [data]
  (if-not (:success data)
    (js/alert "Error: Update failed. Please try again.")))


(defn midpoint [items]
  ;;FIXME Averaging works poorly if badges are spread out.
  #_(let [c (count items)]
      (when (> c 0)
        {:lat (/ (apply + (map :lat items)) c)
         :lng (/ (apply + (map :lng items)) c)}))
  ;; Just use first item as midpoint for now.
  {:lat (-> items first :lat) :lng (-> items first :lng)})


(defn badge-info-content [user-badge-id visible]
  (create-class
    {:reagent-render
     (fn []
       [:div.row {:style {:display (if @visible "block" "none")}}
        [:div.col-md-12
         [:h2.uppercase-header (t :location/Location)]
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
                                 my-marker (js/L.marker. lat-lng (clj->js {:icon lu/badge-icon-ro}))
                                 my-map (-> (js/L.map. "map-view-badge" lu/map-opt)
                                            (.setView lat-lng 5)
                                            (.addLayer (js/L.TileLayer. lu/tile-url lu/tile-opt)))]
                             (.addTo my-marker my-map))
                           (reset! visible false)))})) 300)
       )}))

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
                                 my-map (-> (js/L.map. (str "map-view-badge-" badge-id) lu/map-opt)
                                            (.setView lat-lng 6)
                                            (.addLayer (js/L.TileLayer. lu/tile-url lu/tile-opt)))]
                             (doseq [b (:badges data)]
                               (-> (js/L.latLng. (:lat b) (:lng b))
                                   (js/L.marker. (clj->js {:icon lu/user-icon}))
                                   (.on "click" #(mo/open-modal [:user :profile] {:user-id (:user_id b)}))
                                   (.addTo my-map))))
                           (reset! visible false))
                         )})) 300)
       )}))


(defn badge-share-content [user-badge-id visible]
  (create-class
    {:reagent-render
     (fn []
       [:div.row {:style {:display (if @visible "block" "none")}}
        [:label.col-md-12.sub-heading (t :location/Location)]
        [:div.col-md-12
         [:label (t :location/setLocationHere)]
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
                                 my-marker (js/L.marker. lat-lng (clj->js {:icon lu/badge-icon}))
                                 my-map (-> (js/L.map. "map-view-badge" lu/map-opt)
                                            (.setView lat-lng 5)
                                            (.addLayer (js/L.TileLayer. lu/tile-url lu/tile-opt))
                                            (.on "click" (fn [e]
                                                           (.setLatLng my-marker (aget e "latlng"))
                                                           (ajax/PUT
                                                             (path-for (str "/obpv1/location/user_badge/" user-badge-id))
                                                             {:params (aget e "latlng")
                                                              :handler put-handler})))
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
                                 my-marker (js/L.marker. lat-lng (clj->js {:icon lu/user-icon-ro}))
                                 my-map (-> (js/L.map. (str "map-view-user-" user-id) lu/map-opt)
                                            (.setView lat-lng 8)
                                            (.addLayer (js/L.TileLayer. lu/tile-url lu/tile-opt)))]
                             (.addTo my-marker my-map))
                           (reset! visible false)))})) 300)
       )}))

(defn- user-settings-map [{:keys [lat lng]}]
  (let [lat-lng (js/L.latLng. lat lng)
        my-marker (js/L.marker. lat-lng (clj->js {:icon lu/user-icon}))
        my-map (-> (js/L.map. "map-view-user" lu/map-opt)
                   (.setView lat-lng 5)
                   (.addLayer (js/L.TileLayer. lu/tile-url lu/tile-opt))
                   (.on "click" (fn [e]
                                  (.setLatLng my-marker (aget e "latlng"))
                                  (ajax/PUT
                                    (path-for "/obpv1/location/self")
                                    {:params (aget e "latlng")
                                     :handler put-handler}))))]
    (.addTo my-marker my-map)))

(defn user-edit-content [state]
  (create-class
    {:reagent-render
     (fn []
       [:div.form-group
        [:label.col-md-3
         (t :location/Location)]
        [:div.col-md-9
         [:div.row
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
                                        (ajax/PUT (path-for "/obpv1/location/self") {:params (:default @state) :handler put-handler}))
                                      (do
                                        (swap! state assoc :enabled false)
                                        (swap! state assoc :public  false)
                                        (ajax/PUT (path-for "/obpv1/location/self/reset") {:handler put-handler}))))
                       :checked (:enabled @state)}]
              (t :location/LocationEnabled)]
             [:p.help-block (t :location/LocationEnabledInfo)]
             [:p.help-block {:style {:display (if (:enabled @state) "block" "none")}} (t :location/LocationEnabledInfo2)]]

            [:div.checkbox {:style {:display (if (:enabled @state) "block" "none")}}
             [:label
              [:input {:name      "public"
                       :type      "checkbox"
                       :value     1
                       :on-change (fn [e]
                                    (let [public? (.-target.checked e)]
                                      (swap! state assoc :public public?)
                                      (ajax/PUT (path-for "/obpv1/location/self/public") {:params {:public public?} :handler put-handler})))
                       :checked (:public @state)}]
              (t :location/LocationPublic)]
             [:p.help-block (t :location/LocationPublicInfo)]]]

           [:label (t :location/setLocationHere)]
           [:div {:id "map-view-user" :style {:display (if (:enabled @state) "block" "none") :height "600px" :margin "20px 0"}}]
           ]]
         ]])


     :component-did-mount
     (fn []
       (ajax/GET
         (path-for "/obpv1/location/self" true)
         {:handler (fn [data]
                     (user-settings-map (or (:enabled data) (:country data)))
                     (swap! state assoc :public (:public data))
                     (swap! state assoc :default (or (:enabled data) (:country data)))
                     (when-not (:enabled data)
                       (swap! state assoc :enabled false)))
          })
       )}))

(defn ^:export badge_info [badge-id]
  (let [visible (atom true)]
    [badge-info-content badge-id visible]))

(defn ^:export gallery_badge [badge-id]
  (let [visible (atom true)]
    [gallery-badge-content badge-id visible]))


(defn ^:export badge_share [user-badge-id]
  (let [visible (atom true)]
    [badge-share-content user-badge-id visible]))


(defn ^:export user_profile [user-id]
  (let [visible (atom true)]
    [user-profile-content user-id visible]))


(defn ^:export user_edit []
  (let [state (atom {:enabled true :public false :default {:lat nil :lng nil}})]
    [user-edit-content state]))
