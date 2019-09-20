(ns salava.location.ui.embed
 (:require [salava.core.ui.layout :as layout]
           [salava.location.ui.explore :as ex :refer [icon]]
           [salava.core.ui.helper :refer [path-for base-url base-path]]
           [ajax.core :as ajax]
           [reagent.core :refer [atom create-class cursor]]
           [reagent-modals.modals :as m]
           [salava.core.ui.modal :as mo]
           [salava.location.ui.util :as lu]
           [salava.core.i18n :refer [t translate-text]]
           [clojure.walk :refer [keywordize-keys]]
           [cemerick.url :as url]
           [clojure.string :refer [blank? join]]
           [salava.core.ui.share :refer [clipboard-button]]
           [komponentit.autocomplete :as autocomplete]))

(defn link-builder [state]
  (let [params (as-> {:issuer (get-in @state [:issuer :value] "") :badge (get-in @state [:badge :value] "") :tag (get-in @state [:tag :value] "")} m (remove (fn [[k v]] (blank? v)) m))
        query (when-not (empty? params)(join (cons (str (name (key (first params))) "=" (val (first params)))
                                                   (map #(str (if (not (coll? (val %)))
                                                                (str "&" (name (key %)) "=" (val %))
                                                                (if (empty? (val %)) "" (if (empty? (rest (val %)))
                                                                                          (str "&" (name (key %)) "[0]=" (first (val %)))
                                                                                          (join (cons (str "&" (name (key %)) "[0]=" (first (val %)))
                                                                                                      (map (fn [e] (str "&" (name (key %)) "[" (.indexOf (val %) e) "]=" e)) (rest (val %)))))))))
                                                        (rest params)))))
        url (if (blank? query) (str (base-url)(base-path)"/gallery/map/embed") (str (base-url)(base-path)"/gallery/map/embed?" query))]
    (swap! state assoc :url url :embed-code (str "<iframe width=\"100%\" height=\"1000\" src=\""url"\" frameborder=\"0\"  allowfullscreen=\"true\"></iframe>"))))

(defn query-params [base]
  {:badge_name (get base :badge "")
   :issuer_name (get base :issuer "")
   :tag_name (get base :tag "")})

(defn filter-autocomplete [kind state]
  (let [filter (cursor state [kind])
        class-name (str (name kind) "-filter")
        placeholder (keyword "location" (str (name kind) "FilterField"))]
    (fn []
      [autocomplete/autocomplete
       {:value (:value @filter)
        :on-change (fn [item]
                     (swap! filter assoc :value (:key item))
                     (link-builder state)
                     (.trigger (js/jQuery (str "div.badges-filter ."class-name " input")) "change"))
        :search-fields   [:value]
        :items           (:autocomplete @filter)
        :no-results-text " "
        :placeholder     (t placeholder)
        :control-class   (str "form-control " class-name)
        :max-results     100}])))


(defn- get-markers [kind my-map layer-group opt]
  (let [bounds (.getBounds my-map)
        rounded (if (> (.getZoom my-map) 6) #(.toFixed % 2) #(js/Math.round (+ % 0.5)))
        group-fn (fn [coll v]
                   (update coll [(-> v :lat rounded) (-> v :lng rounded)] conj v)) ; Put items at same lat/lng into a list
        click-cb (case kind
                   "users"
                   (fn [u u-count]
                     (if (= 1 u-count)
                       #(mo/open-modal [:profile :view] {:user-id (-> u first :id)})
                       #(mo/open-modal [:location :userlist] {:users u})))
                   "badges"
                   (fn [b b-count]
                     (if (= 1 b-count)
                       #(mo/open-modal [:gallery :badges] {:badge-id (-> b first :badge_id) :gallery-id (-> b first :gallery_id)})
                       #(mo/open-modal [:location :badgelist] {:badges b}))))
        item-name (case kind
                   "users"
                   (fn [u] (str (:first_name u) " " (:last_name u)))
                   "badges" :badge_name)]
    (ajax/GET
      (path-for (str "/obpv1/location/explore/" kind) false)
      {:params (merge opt {:max_lat (.getNorth bounds) :max_lng (if (<= -180 (.getEast bounds) 180) (.getEast bounds) (if (pos? (.getEast bounds)) 180 -180));(.getEast bounds)
                           :min_lat (.getSouth bounds) :min_lng (if (<= -180 (.getWest bounds) 180) (.getWest bounds) (if (pos? (.getWest bounds)) 180 -180))})
       :handler
       (fn [data]
         (.clearLayers layer-group)
         (doseq [item (->> kind keyword (get data) (reduce group-fn {}) vals)]
           (let [item-1 (first item)
                 unique-key (case kind
                              "users"  :id
                              "badges" :gallery_id)
                 unique-count (->> item (map unique-key) set count)
                 icon  (icon kind unique-count)
                 title (if (= unique-count 1) (item-name item-1) "")]
             (.addLayer
               layer-group
               (-> (js/L.latLng. (:lat item-1) (:lng item-1))
                   (js/L.marker. (clj->js {:icon icon :title title}))
                   (.on "click" (click-cb item unique-count)))))))})))


(defn map-view [state]
  (create-class
    {:reagent-render
     (fn []
       [:div.row
        [:div.col-md-12
         [m/modal-window]
         [:div {:id "map-view" :style {:height "1100px" :margin "20px 0"}}]]])

     :component-did-mount
     (fn []
       (let [timer (atom 0)
             layer-group (js/L.layerGroup. (clj->js []))
             lat-lng (js/L.latLng. 40 -20)
             my-map (-> (js/L.map. "map-view" lu/map-opt)
                        (.setView lat-lng 3)
                        (.addLayer (js/L.TileLayer. lu/tile-url lu/tile-opt)))
             redraw-map! (fn []
                          (js/clearTimeout @timer)
                          (reset! timer
                                  (js/setTimeout
                                    #(get-markers "badges" my-map layer-group (or (:query-params @state) {}))
                                    1000)))]

         (.addTo layer-group my-map)
         (.on my-map "moveend" redraw-map!)
         (get-markers "badges" my-map layer-group (or (:query-params @state) {}))))}))

(defn input-button [name id textatom]
  (let [status (atom "")]
    (fn []
      [:div {:class "form-group" :key id}
       [:fieldset
        [:label {:class " sub-heading"} name]
        [:div.input-group
         [:input {:class       "form-control"
                  :id          id
                  :name        "email-text"
                  :type        "text"
                  :read-only true
                  :value       @textatom}]
         [:span {:class "input-group-btn"}
          [clipboard-button (str "#" id) status]]]]])))


(defn generate-link-form [state]
 (fn []
  [:div
   [:div.form-horizontal
    [:div.form-group.badges-filter
     [:div.col-md-6
      [filter-autocomplete :badge state]]

     [:div.col-md-1 {:style {:padding-left 0}}
      [:button.btn.btn-link
       {:title (t :location/clearField)
        :style {:padding-left 0 :font-weight "bold"}
        :on-click #(do (swap! state assoc-in [:badge :value] "")
                       (link-builder state)
                       (.trigger (js/jQuery (str "div.badges-filter .badge-filter input")) "change"))}
       [:i.fa.fa-refresh]]]]

    [:div.form-group.badges-filter
     [:div.col-md-6
      [filter-autocomplete :issuer state]]

     [:div.col-md-1 {:style {:padding-left 0}}
      [:button.btn.btn-link
       {:title (t :location/clearField)
        :style {:padding-left 0 :font-weight "bold"}
        :on-click #(do (swap! state assoc-in [:issuer :value] "")
                       (link-builder state)
                       (.trigger (js/jQuery (str "div.badges-filter .issuer-filter input")) "change"))}
       [:i.fa.fa-refresh]]]]

    [:div.form-group.badges-filter
     [:div.col-md-6
      [filter-autocomplete :tag state]]

     [:div.col-md-1 {:style {:padding-left 0}}
      [:button.btn.btn-link
       {:title (t :location/clearField)
        :style {:padding-left 0 :font-weight "bold"}
        :on-click #(do (swap! state assoc-in [:tag :value] "")
                       (link-builder state)
                       (.trigger (js/jQuery (str "div.badges-filter .tag-filter input")) "change"))}
       [:i.fa.fa-refresh]]]]]
   [:div.row [:div.col-md-6 [:hr.border]]]
   [:div.form-horizontal {:style {:margin "10px 0"}}
    [:div.row [:div.col-md-12 [:div.col-md-6 [input-button (t :core/Embedcode) "embed" (cursor state [:embed-code]) #_(str "<iframe width=\"100%\" height=\"1000\" src=\""@(cursor state [:url])"\" frameborder=\"0\"  allowfullscreen=\"true\"></iframe>")]]]]
    [:div.row [:div.col-md-12 [:div.col-md-6 [input-button (t :core/Link) "link" (cursor state [:url])]]]]]]))


(defn handler [site-navi]
  (let [query (-> js/window .-location .-href url/url :query keywordize-keys)
        params (query-params query)
        state (atom {:initializing true})]
    (swap! state assoc :query-params params)
    (fn []
      (layout/embed-page [map-view state]))))

(defn link-handler [site-navi]
 (let [state (atom {:tag    {:value "" :autocomplete {}}
                    :badge  {:value "" :autocomplete {}}
                    :issuer {:value "" :autocomplete {}}
                    :query-params {:tag "" :issuer "" :badge ""}
                    :embed-code ""
                    :url ""})]

   (ajax/GET
     (path-for "/obpv1/location/explore/filters" false)
     {:handler
      (fn [data]
        (swap! state assoc-in [:tag    :autocomplete] (reduce (fn [coll v] (assoc coll v v)) {} (:tag_name    data)))
        (swap! state assoc-in [:badge  :autocomplete] (reduce (fn [coll v] (assoc coll v v)) {} (:badge_name  data)))
        (swap! state assoc-in [:issuer :autocomplete] (reduce (fn [coll v] (assoc coll v v)) {} (:issuer_name data)))
        (link-builder state))})

   (fn [] (layout/landing-page site-navi [generate-link-form state]))))
