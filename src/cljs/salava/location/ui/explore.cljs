(ns salava.location.ui.explore
  (:require [reagent.core :refer [atom cursor create-class]]
            [reagent.session :as session]
            [reagent-modals.modals :as m]
            [ajax.core :as ajax]
            [komponentit.autocomplete :as autocomplete]
            [salava.core.ui.modal :as mo]
            [salava.core.ui.helper :refer [js-navigate-to path-for private? plugin-fun]]
            [salava.core.ui.layout :as layout]
            [salava.core.i18n :refer [t translate-text]]
            [salava.location.ui.util :as lu]
            ))

(def icon {"users"  lu/user-icon
           "badges" lu/badge-icon})


(defn- get-markers [kind my-map layer-group opt]
  (let [bounds (.getBounds my-map)
        click-cb (case kind
                   "users"
                   (fn [u] #(mo/open-modal [:user :profile] {:user-id (:id u)}))
                   "badges"
                   (fn [b] #(mo/open-modal [:gallery :badges] {:badge-id (:badge_id b)})))
        item-name (case kind
                   "users"
                   (fn [u] (str (:first_name u) " " (:last_name u)))
                   "badges" :badge_name)]
    (ajax/GET
      (path-for (str "/obpv1/location/explore/" kind) false)
      {:params (merge opt {:max_lat (.getNorth bounds) :max_lng (.getEast bounds)
                           :min_lat (.getSouth bounds) :min_lng (.getWest bounds)})
       :handler
       (fn [data]
         (.clearLayers layer-group)
         (doseq [item (get data (keyword kind))]
           (.addLayer
             layer-group
             (-> (js/L.latLng. (:lat item) (:lng item))
                 (js/L.marker. (clj->js {:icon (get icon kind) :title (item-name item)}))
                 (.on "click" (click-cb item))))))
       })))

(defn filter-autocomplete [kind state]
  (let [filter (cursor state [kind])
        class-name (str (name kind) "-filter")
        placeholder (keyword "location" (str (name kind) "FilterField"))]
    (fn []
      [autocomplete/autocomplete
       {:value (:value @filter)
        :cb    (fn [item]
                 (swap! filter assoc :value (:key item))
                 (.trigger (js/jQuery (str "div.badges-filter ."class-name " input")) "change"))
        :search-fields   [:value]
        :items           (:autocomplete @filter)
        :no-results-text " "
        :placeholder     (t placeholder)
        :control-class   (str "form-control " class-name)
        :max-results     100
        }])))

(defn map-view [state]
  (create-class
    {:reagent-render
     (fn []
       [:div.row
        [:div.col-md-12
         [m/modal-window]

         [:div.row
          [:div.col-md-12
           [:p.help-block (t :location/exploreEnableHelp)]
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

         [:hr]

         [:div.form-horizontal

          [:div.form-group.users-filter {:style {:display "block"}}
           [:div.col-md-6
            [:input.form-control {:name "user_name"
                                  :type "text"
                                  :placeholder (t :location/SearchUsers)}]
            ]]

          [:div.form-group.badges-filter {:style {:display "none"}}
           [:div.col-md-6
            [filter-autocomplete :badge state]
            ]
           [:div.col-md-1 {:style {:padding-left 0}}
            [:button.btn.btn-link
             {:style {:padding-left 0 :font-weight "bold"}
              :on-click #(do (swap! state assoc-in [:badge :value] "")
                             (.trigger (js/jQuery (str "div.badges-filter .badge-filter input")) "change"))}
             "X"]]]

          [:div.form-group.badges-filter {:style {:display "none"}}
           [:div.col-md-6
            [filter-autocomplete :issuer state]
            ]
           [:div.col-md-1 {:style {:padding-left 0}}
            [:button.btn.btn-link
             {:style {:padding-left 0 :font-weight "bold"}
              :on-click #(do (swap! state assoc-in [:issuer :value] "")
                             (.trigger (js/jQuery (str "div.badges-filter .issuer-filter input")) "change"))}
             "X"]]]

          [:div.form-group.badges-filter {:style {:display "none"}}
           [:div.col-md-6
            [filter-autocomplete :tag state]
            ]
           [:div.col-md-1 {:style {:padding-left 0}}
            [:button.btn.btn-link
             {:style {:padding-left 0 :font-weight "bold"}
              :on-click #(do (swap! state assoc-in [:tag :value] "")
                             (.trigger (js/jQuery (str "div.badges-filter .tag-filter input")) "change"))}
             "X"]]]
          ]

         [:div {:id "map-view" :style {:height "700px" :margin "20px 0"}}]]])

     :component-did-mount
     (fn []
       (let [timer (atom 0)
             layer-group (js/L.layerGroup. (clj->js []))
             lat-lng (js/L.latLng. 40 -20)
             my-map (-> (js/L.map. "map-view" lu/map-opt)
                        (.setView lat-lng 3)
                        (.addLayer (js/L.TileLayer. lu/tile-url lu/tile-opt)))

             query-opt (fn []
                         (case (.val (js/jQuery "input[name=map-type]:checked"))
                           "users"  {:user_name (.val (js/jQuery "input[name=user_name]"))}
                           "badges" {:badge_name  (.val (js/jQuery ".badge-filter input"))
                                     :issuer_name (.val (js/jQuery ".issuer-filter input"))
                                     :tag_name    (.val (js/jQuery ".tag-filter input"))}))

             redraw-map! (fn []
                          (js/clearTimeout @timer)
                          (reset! timer
                                  (js/setTimeout
                                    #(get-markers (.val (js/jQuery "input[name=map-type]:checked")) my-map layer-group (query-opt))
                                    1000)))]

         (.addTo layer-group my-map)

         (.on my-map "moveend" redraw-map!)

         (.on (js/jQuery "div.users-filter input, div.badges-filter input") "keyup change"
              (fn [e]
                (let [len (-> e .-target.value count)]
                (when (or (= len 0) (>= len 3))
                  (redraw-map!)))))

         (.on (js/jQuery "input[name=map-type]") "change"
              (fn [e]
                (let [kind (.-target.value e)]
                  (-> (js/jQuery "div.users-filter input, div.badges-filter input") (.val ""))
                  (.toggle (js/jQuery "div.users-filter"))
                  (.toggle (js/jQuery "div.badges-filter"))
                  (get-markers kind my-map layer-group (query-opt)))))


         (get-markers "users" my-map layer-group {})
         ))
     }))

(defn handler [site-navi]
  (let [state (atom {:tag    {:value "" :autocomplete {}}
                     :badge  {:value "" :autocomplete {}}
                     :issuer {:value "" :autocomplete {}}
                     })]
    (ajax/GET
      (path-for "/obpv1/location/explore/filters" false)
      {:handler
       (fn [data]
         (swap! state assoc-in [:tag    :autocomplete] (reduce (fn [coll v] (assoc coll v v)) {} (:tag_name    data)))
         (swap! state assoc-in [:badge  :autocomplete] (reduce (fn [coll v] (assoc coll v v)) {} (:badge_name  data)))
         (swap! state assoc-in [:issuer :autocomplete] (reduce (fn [coll v] (assoc coll v v)) {} (:issuer_name data)))
         )})
    (fn []
      (layout/default site-navi [map-view state]))))
