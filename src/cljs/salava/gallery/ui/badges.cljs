(ns salava.gallery.ui.badges
  (:require [reagent.core :refer [atom cursor]]
            [ajax.core :as ajax]
            [clojure.walk :refer [keywordize-keys]]
            [clojure.string :refer [trim]]
            [salava.core.ui.layout :as layout]
            [salava.core.ui.grid :as g]
            [salava.core.i18n :refer [t]]
            [salava.core.helper :refer [dump]]))

(defn fetch-badges [state]
  (let [{:keys [user-id country-selected badge-name recipient-name issuer-name]} @state]
    (ajax/POST
      (str "/obpv1/gallery/badges/" user-id)
      {:params  {:country   (trim country-selected)
                 :badge     (trim badge-name)
                 :recipient (trim recipient-name)
                 :issuer    (trim issuer-name)}
       :handler (fn [data]
                  (let [data-with-kws (keywordize-keys data)
                        badges (:badges data-with-kws)]
                    (swap! state assoc :badges badges)))})))

(defn text-field [key label placeholder state]
  (let [search-atom (cursor state [key])
        field-id (str key "-field")]
    [:div.form-group
     [:label {:class "control-label col-sm-2" :for field-id} (str label ":")]
     [:div.col-sm-10
      [:input {:class       (str "form-control")
               :id          field-id
               :type        "text"
               :placeholder placeholder
               :value       @search-atom
               :on-change   #(do
                              (reset! search-atom (.-target.value %))
                              (fetch-badges state))}]]]))

(defn country-selector [state]
  (let [country-atom (cursor state [:country-selected])]
    [:div.form-group
     [:label {:class "control-label col-sm-2" :for "country-selector"} (str (t :gallery/Country) ":")]
     [:div.col-sm-10
      [:select {:class     "form-control"
                :id        "country-selector"
                :name      "country"
                :value     @country-atom
                :on-change #(do
                             (reset! country-atom (.-target.value %))
                             (fetch-badges state))}
       (for [[country-key country-name] (map identity (:countries @state))]
         [:option {:value country-key
                   :key country-key} country-name])]]]))

(defn order-radio-values []
  [{:value "mtime" :id "radio-date" :label (t :core/bydate)}
   {:value "name" :id "radio-name" :label (t :core/byname)}
   {:value "issuer_name" :id "radio-issuer-name" :label (t :core/byissuername)}])

(defn gallery-grid-form [state]
  (let [show-advanced-search (cursor state [:advanced-search])]
    [:div {:id "grid-filter"
           :class "form-horizontal"}
     (if (not (:user-id @state))
       [:div
        [country-selector state]
        [:div
         [:a {:on-click #(reset! show-advanced-search (not @show-advanced-search))
              :href "#"}
          (if @show-advanced-search
            (t :gallery/Hideadvancedsearch)
            (t :gallery/Showadvancedsearch))]]
        (if @show-advanced-search
          [:div
           [text-field :badge-name (t :gallery/Badgename) (t :gallery/Searchbybadgename) state]
           [text-field :recipient-name (t :gallery/Recipient) (t :gallery/Searchbyrecipient) state]
           [text-field :issuer-name (t :gallery/Issuer) (t :gallery/Searchbyissuer) state]])])
     [g/grid-radio-buttons (str (t :core/Order) ":") "order" (order-radio-values) :order state]]))

(defn badge-grid-element [element-data]
  (let [{:keys [id image_file name description issuer_name issuer_url recipients]} element-data]
    [:div {:class "col-xs-12 col-sm-6 col-md-4"
           :key id}
     [:div {:class "media grid-container"}
      [:div.media-content
       (if image_file
         [:div.media-left
          [:img {:src (str "/" image_file)}]])
       [:div.media-body
        [:div.media-heading
         [:a.heading-link {:href (str "/badge/info/" id)}
          name]]
        [:div.media-issuer
         [:a {:href issuer_url
              :target "_blank"} issuer_name]]
        (if recipients
          [:div.media-recipients
           recipients " " (t :gallery/recipients)])
        [:div.media-description description]]]]]))

(defn gallery-grid [state]
  (let [badges (:badges @state)
        order (keyword (:order @state))
        badges (if (= order :mtime)
                 (sort-by order > badges)
                 (sort-by order badges))]
    (into [:div {:class "row"
                 :id    "grid"}]
          (for [element-data badges]
            (badge-grid-element element-data)))))

(defn content [state]
  [:div {:id "gallery-badges"}
   [gallery-grid-form state]
   [gallery-grid state]])

(defn init-data [state user-id]
  (ajax/POST
    (str "/obpv1/gallery/badges/" user-id)
    {:params {:country ""
              :badge ""
              :issuer ""
              :recipient ""}
     :handler (fn [data]
                (let [data-with-kws (keywordize-keys data)
                      {:keys [badges countries country]} data-with-kws]
                  (swap! state assoc :badges badges
                                     :countries countries
                                     :country-selected country)))}))

(defn handler [site-navi params]
  (let [user-id (:user-id params)
        state (atom {:user-id user-id
                     :badges []
                     :country-selected "Finland"
                     :advanced-search false
                     :badge-name ""
                     :recipient-name ""
                     :issuer-name ""
                     :order "name"})]
    (init-data state user-id)
    (fn []
      (layout/default site-navi (content state)))))