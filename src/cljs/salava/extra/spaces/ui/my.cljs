(ns salava.extra.spaces.ui.my
  (:require
   [clojure.set :refer [intersection]]
   [clojure.string :refer [upper-case]]
   [reagent.core :refer [atom cursor]]
   [reagent-modals.modals :as m]
   [salava.core.i18n :refer [t]]
   [salava.core.ui.ajax-utils :as ajax]
   [salava.core.ui.helper :refer [path-for unique-values]]
   [salava.core.ui.layout :as layout]
   [salava.core.ui.modal :as mo]
   [salava.core.ui.grid :as g]
   [salava.extra.spaces.ui.helper :refer [space-card]]))

(defn init-data [state]
  (ajax/GET
   (path-for "/obpv1/spaces/")
   {:handler (fn [data]
               (swap! state assoc :spaces data))}))

#_(defn create! [state]
    (ajax/POST
     (path-for "/obpv1/spaces/create")
     {:params @(cursor state [:new])}
     {:handler (fn [data]
                 (if (= "success" (:status data))
                   "" ""))}))

(defn element-visible? [element state]
  (if (and
       (or (> (count
               (intersection
                (into #{} (:status-selected @state))
                #{(:status element)}))
                ;(into #{} (:status element))))
              0)
           (= (:status-all @state) true))
       (or (empty? (:search @state))
           (not= (.indexOf
                  (.toLowerCase (:name element))
                  (.toLowerCase (:search @state)))
                 -1)))
    true false))

(defn space-gallery [state]
 (let [spaces @(cursor state [:spaces])
       order (keyword (:order @state))
       spaces (case order
                (:name) (sort-by (comp upper-case str order) spaces)
                (:mtime) (sort-by order > spaces)
                (sort-by order > spaces))]
  (into [:div#grid.row.wrap-grid
         [:div.col-xs-12.col-sm-6.col-md-4
          [:div {:class "media grid-container space-card"}
           [:a.add-element-link {:href  (path-for "/admin/spaces/creator")}
             [:div.media-content
              [:div.media-body
               [:div.text-center {:id "add-element-icon"}
                [:i.fa.fa-plus]]
               [:div {:id "add-element-link"}
                (t :extra-spaces/New)]]]]]]]
    (doall
      (for [space spaces]
       (when (element-visible? space state)
         (space-card space state nil true)))))))

(defn order-radio-values []
  [{:value "mtime" :id "radio-date" :label (t :core/bydate)}
   {:value "name" :id "radio-name" :label (t :core/byname)}
   {:value "member_count" :id "radio-member-count" :label (t :extra-spaces/bynoofmembers)}])

(defn grid-form [state]
 [:div#grid-filter.form-horizontal
  [g/grid-search-field (t :core/Search ":") "spacesearch" (t :core/Searchbyname) :search state]
  [g/translated-grid-buttons  (t :extra-spaces/Status ":") (unique-values :status @(cursor state [:spaces]))  "status-selected" "status-all" state "extra-spaces"]
  [g/grid-radio-buttons (t :core/Order ":")  "order" (order-radio-values) :order state]])

(defn content [state]
  [:div#space-gallery
   [m/modal-window]
   [grid-form state]
   [space-gallery state]])


(defn handler [site-navi]
  (let [state (atom {:spaces []
                     :status-all true
                     :status-selected []
                     ;:new nil
                     :order "mtime"})]
   (init-data state)
   (fn []
     (layout/default site-navi [content state]))))
