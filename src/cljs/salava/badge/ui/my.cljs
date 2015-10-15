(ns salava.badge.ui.my
  (:require [reagent.core :as reagent :refer [atom]]
            [reagent.session :as session]
            [clojure.set :as set :refer [intersection]]
            [clojure.walk :as walk :refer [keywordize-keys]]
            [ajax.core :as ajax]
            [salava.core.ui.helper :as h :refer [unique-values]]
            [salava.core.ui.layout :as layout]
            [salava.core.ui.grid :as g]
            [salava.core.i18n :as i18n :refer [t]]))

(defn visibility-select-values []
  [{:value "all" :title (t :badge/All)}
   {:value "public"  :title (t :badge/Public)}
   {:value "shared"  :title (t :badge/Shared)}
   {:value "private" :title (t :badge/Private)}])

(defn order-radio-values []
  [{:value "mtime" :id "radio-date" :label (t :badge/Bydate)}
   {:value "name" :id "radio-name" :label (t :badge/Byname)}])

(defn badge-grid-form [state]
  [:div {:class "form-horizontal"}
   [g/grid-search-field (t :badge/Search) "badgesearch" (t :badge/Searchbyname) :search state]
   [g/grid-select (t :badge/Show) "select-visibility" :visibility (visibility-select-values) state]
   [g/grid-buttons (t :badge/Tags) (unique-values :tags (:badges @state)) :tags-selected :tags-all state]
   [g/grid-radio-buttons (t :badge/Orderby) "order" (order-radio-values) :order state]])

(defn badge-visible? [element state]
  (if (and
        (or (= (:visibility @state) "all")
            (= (:visibility @state) (:visibility element)))
        (or (> (count
                 (intersection
                   (into #{} (:tags-selected @state))
                   (into #{} (:tags element)))
                 )
               0)
            (= (:tags-all @state)
               true))
        (or (empty? (:search @state))
            (not= (.indexOf
                    (.toLowerCase (:name element))
                    (.toLowerCase (:search @state)))
                  -1)))
    true false))

(defn badge-grid [state]
  [:div {:class "row"
         :id "grid"}
   (doall (let [badges (:badges @state)
                order (:order @state)]
            (for [element-data (sort-by (keyword order) badges)]
              (if (badge-visible? element-data state)
                (g/badge-grid-element element-data)))))])

(defn update-status [id new-status state]
  (ajax/POST
    (str "obpv1/badge/set_status/" id)
    {:params  {:status new-status}
     :handler (fn []
                (let [badge (first (filter #(= id (:id %)) (:pending @state)))]
                  (swap! state assoc :pending (remove #(= badge %) (:pending @state)))
                  (if (= new-status "accepted")
                    (swap! state assoc :badges (conj (:badges @state) badge)))))}))

(defn badge-pending [{:keys [id image_file name description]} state]
  [:div.row {:key id}
   [:div.col-md-12
    [:div.badge-container-pending
     [:div.row
      [:div.col-md-12
       [:div.media
        [:div.pull-left
         [:img.badge-image {:src image_file}]]
        [:div.media-body
         [:h4.media-heading
          name]
         [:div
          description]]]]]
     [:div.row
      [:div.col-md-12
       [:button {:class "btn btn-default btc-accept"
                 :on-click #(update-status id "accepted" state)}
        (t :badge/Accept)]
       [:button {:class "btn btn-default btc-decline"
                 :on-click #(update-status id "declined" state)}
        (t :badge/Decline)]]]]]])

(defn badges-pending [state]
  [:div
   (for [badge (:pending @state)]
     (badge-pending badge state))])

(defn content [state]
  [:div {:class "badge-grid"}
   [badges-pending state]
   [badge-grid-form state]
   [badge-grid state]])

(defn init-data [state]
  (ajax/GET
    "/obpv1/badge/1"
    {:handler (fn [x]
                (let [data (map keywordize-keys x)]
                  (swap! state assoc :badges (filter #(= "accepted" (:status %)) data))
                  (swap! state assoc :pending (filter #(= "pending" (:status %)) data))))}))

(defn handler [site-navi params]
  (let [state (atom {:badges []
                     :pending []
                     :visibility "all"
                     :order ""
                     :tags-all true
                     :tags-selected []})]
    (init-data state)
    (fn []
      (layout/default site-navi (content state)))))
