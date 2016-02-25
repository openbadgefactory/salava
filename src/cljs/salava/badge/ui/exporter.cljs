(ns salava.badge.ui.exporter
  (:require [reagent.core :refer [atom]]
            [reagent.session :as session]
            [clojure.set :refer [intersection]]
            [salava.core.ui.ajax-utils :as ajax]
            [salava.core.ui.layout :as layout]
            [salava.core.ui.helper :refer [unique-values]]
            [salava.core.ui.grid :as g]
            [salava.core.i18n :refer [t]]))

(defn email-options [state]
  (let [emails (unique-values :email (:data @state))]
    (map #(hash-map :value % :title %) emails)))

(defn visibility-options []
  [{:value "all" :title (t :badge/All)}
   {:value "public"  :title (t :badge/Public)}
   {:value "shared"  :title (t :badge/Shared)}
   {:value "private" :title (t :badge/Private)}])

(defn order-radio-values []
  [{:value "mtime" :id "radio-date" :label (t :badge/bydate)}
   {:value "name" :id "radio-name" :label (t :badge/byname)}])

(defn badge-grid-form [state]
  [:div {:id "grid-filter"
         :class "form-horizontal"}
   [g/grid-select        (t :badge/Email ":") "select-email" :email-selected (email-options state) state]
   [g/grid-search-field  (t :badge/Search ":") "badgesearch" (t :badge/Searchbyname) :search state]
   [g/grid-select        (t :badge/Show ":") "select-visibility" :visibility (visibility-options) state]
   [g/grid-buttons       (t :badge/Tags ":") (unique-values :tags (:data @state)) :tags-selected :tags-all state]
   [g/grid-radio-buttons (t :badge/Order ":") "order" (order-radio-values) :order state]])

(defn badge-visible? [element state]
  (if (and
        (= (:email-selected @state) (:email element))
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

(defn grid-element [element-data state]
  (let [{:keys [id image_file name description visibility assertion_url]} element-data]
    [:div {:class "col-xs-12 col-sm-6 col-md-4"
           :key id}
     [:div {:class "media grid-container"}
      [:div.media-content
       (if image_file
         [:div.media-left
          [:img {:src (if-not (re-find #"http" image_file)
                        (str "/" image_file)
                        image_file)}]])
       [:div.media-body
        [:div.media-heading
         [:a.badge-link {:href (str "/badge/info/" id)}
          name]]
        [:div.visibility-icon
         (case visibility
           "private" [:i {:class "fa fa-lock"}]
           "internal" [:i {:class "fa fa-group"}]
           "public" [:i {:class "fa fa-globe"}]
           nil)]
        [:div.media-description description]]]
      [:div {:class "media-bottom"}
       [:div.row
        [:div.col-xs-8
         (let [checked? (boolean (some #(= id %) (:badges-selected @state)))]
           [:div.checkbox
            [:label
             [:input {:type "checkbox"
                      :on-change (fn []
                                   (if checked?
                                     (swap! state assoc :badges-selected (remove #(= % id) (:badges-selected @state)))
                                     (swap! state assoc :badges-selected (conj (:badges-selected @state) id))))
                      :checked checked?}]
             (t :badge/Exporttobackpack)]])]
        [:div {:class "col-xs-4 text-right"}
         [:a {:href (str "https://backpack.openbadges.org/baker?assertion=" (js/encodeURIComponent assertion_url)) :class "badge-download"}
          [:i {:class "fa fa-download"}]]]]]]]))

(defn badge-grid [state]
  [:div {:class "row"
         :id "grid"}
   (doall (let [badges (:data @state)
                order (:order @state)]
            (for [element-data (sort-by (keyword order) badges)]
              (if (badge-visible? element-data state)
                (grid-element element-data state)))))])

(defn export-badges [state]
  (let [badges-to-export (:badges-selected @state)
        assertion-urls (map :assertion_url (filter (fn [b] (and (some #(= % (:id b)) badges-to-export)
                                                                (badge-visible? b state))) (:data @state)))]
    (if-not (empty? badges-to-export)
      (.issue js/OpenBadges (clj->js assertion-urls)))))

(defn content [state]
  [:div {:id "export-badges"}
   [:h1.uppercase-header (t :badge/Exportordownload)]
   [badge-grid-form state]
   [:div.export-button
    [:button {:class    "btn btn-primary"
              :on-click #(export-badges state)
              :disabled (= 0 (count (:badges-selected @state)))}
     (t :badge/Exportselected)]]
   [badge-grid state]])

(defn init-data [state]
  (ajax/GET
    "/obpv1/badge/export"
    {:handler (fn [data]
                (swap! state assoc :data data)
                (swap! state assoc :email-selected (first (unique-values :email data))))}))

(defn handler [site-navi]
  (let [state (atom {:data []
                     :email-selected ""
                     :visibility "all"
                     :order ""
                     :tags-all true
                     :tags-selected []
                     :badges-all false
                     :badges-selected []})]
    (init-data state)
    (fn []
      (layout/default site-navi (content state)))))
