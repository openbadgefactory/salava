(ns salava.extra.spaces.ui.explore
 (:require
  [clojure.string :refer [trim]]
  ;[reagent.core :refer [atom cursor]]
  [reagent-modals.modals :as m]
  [reagent.session :as session]
  [salava.core.ui.ajax-utils :as ajax]
  [salava.core.ui.helper :refer [path-for]]
  [salava.core.ui.layout :as layout]
  [salava.core.ui.modal :as mo]
  [salava.core.ui.grid :as g]
  [salava.core.i18n :refer [t]]
  [salava.extra.spaces.ui.helper :refer [space-card]]
  [reagent.core :refer [atom cursor create-class]]))


(defn ajax-stop [ajax-message-atom]
  (reset! ajax-message-atom nil))

(defn get-spaces [state]
 (let [page-count-atom (cursor state [:params :page_count])
       ajax-message-atom (cursor state [:ajax-message])]
  (reset! ajax-message-atom (t :core/Loading))
  (reset! page-count-atom 0)
  (ajax/GET
   (path-for "/obpv1/spaces/gallery/all")
   {:params (assoc (:params @state) :name (:search @state))
    :handler (fn [data]
               (swap! page-count-atom inc)
              ; (reset! state nil)
               (swap! state assoc :spaces (:spaces data) :space_count (:space_count data)))
    :finally (fn []
               (ajax-stop (cursor state [:ajax-message])))})))

(defn get-more-spaces [state]
 (let [page-count-atom (cursor state [:params :page_count])
       ajax-message-atom (cursor state [:ajax-message])]
  (ajax/GET
   (path-for "/obpv1/spaces/gallery/all")
   {:params (assoc (:params @state) :name (:search @state))
    :handler (fn [data]
               (swap! page-count-atom inc)
               (swap! state assoc :spaces (into (:spaces @state) (:spaces data) ) :space_count (:space_count data)))
    :finally (fn [])})))
               ;(ajax-stop (cursor state [:ajax-message])))})))

(defn search-timer [state]
  (let [timer-atom (cursor state [:timer])]
    (if @timer-atom
      (js/clearTimeout @timer-atom))
    (reset! timer-atom (js/setTimeout (fn [] (get-spaces state)) 500))))

(defn order-radio-values []
  [{:value "mtime" :id "radio-date" :label (t :core/bydate)}
   {:value "name" :id "radio-name" :label (t :core/byname)}
   {:value "member_count" :id "radio-member-count" :label (t :extra-spaces/bynoofmembers)}])

(defn grid-form [state]
 [:div#grid-filter.form-horizontal
  [g/grid-search-field (t :core/Search ":") "spacesearch" (t :core/Searchbyname) :search state search-timer]
  [g/grid-radio-buttons (t :core/Order ":")  "order" (order-radio-values) [:params :order] state get-spaces]])

(defn load-more [state]
  (if (pos? (:space_count @state))
    [:div {:class "media message-item"}
     [:div {:class "media-body"}
      [:span [:a {:href     "#"
                  :id    "loadmore"
                  :on-click #(do
                               (get-more-spaces state)
                               (.preventDefault %))}

              (str (t :social/Loadmore) " (" (:space_count @state) " " (t :extra-spaces/Spacesleft) ")")]]]]))

(defn space-gallery [state]
 (let [spaces @(cursor state [:spaces])]
  [:div
    (into [:div#grid.row.wrap-grid]
      (doall
        (for [space spaces]
           (space-card space state (fn [] (get-spaces state)) false))))

    (load-more state)]))

(defn content [state]
 (fn []
  [:div#space-gallery
   [m/modal-window]
   [grid-form state]
   [:div
    (if @(cursor state [:ajax-message])
      [:div.ajax-message
        [:i.fa.fa-cog.fa-spin.fa-2x] [:span (:ajax-message @state)]]
      [space-gallery state])]]))


(defn handler [site-navi]
 (let [params  {:order "mtime" :page_count 0 :name ""}
       v (atom {:spaces []
                :space_count 0
                :ajax-message nil
                :timer nil
                :search ""
                :params params})]

   (ajax/GET
    (path-for "/obpv1/spaces/gallery/all")
    {:params params
     :handler (fn [data]
                (reset! (cursor v [:params :page_count]) 1)
                (swap! v assoc :spaces (:spaces data) :space_count (:space_count data)))
     :finally (fn []
                (ajax-stop (cursor v [:ajax-message])))})


   (fn []
    (layout/default site-navi [content v]))))
