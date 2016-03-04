(ns salava.gallery.ui.badges
  (:require [reagent.core :refer [atom cursor]]
            [reagent-modals.modals :as m]
            [clojure.string :refer [trim]]
            [salava.core.ui.ajax-utils :as ajax]
            [salava.core.ui.layout :as layout]
            [salava.core.ui.grid :as g]
            [salava.core.i18n :refer [t]]
            [salava.gallery.ui.badge-content :refer [badge-content-modal]]
            [salava.core.helper :refer [dump]]))

(defn open-modal [badge-content-id]
  (ajax/GET
    (str "/obpv1/gallery/public_badge_content/" badge-content-id)
    {:handler (fn [data]
                (m/modal! [badge-content-modal data] {:size :lg}))}))

(defn ajax-stop [ajax-message-atom]
  (reset! ajax-message-atom nil))

(defn fetch-badges [state]
  (let [{:keys [user-id country-selected badge-name recipient-name issuer-name]} @state
        ajax-message-atom (cursor state [:ajax-message])]
    (reset! ajax-message-atom (t :gallery/Searchingbadges))
    (ajax/POST
      (str "/obpv1/gallery/badges/" user-id)
      {:params  {:country   (trim country-selected)
                 :badge     (trim badge-name)
                 :recipient (trim recipient-name)
                 :issuer    (trim issuer-name)}
       :handler (fn [data]
                  (swap! state assoc :badges (:badges data)))
       :finally (fn []
                  (ajax-stop ajax-message-atom))})))

(defn search-timer [state]
  (let [timer-atom (cursor state [:timer])]
    (if @timer-atom
      (js/clearTimeout @timer-atom))
    (reset! timer-atom (js/setTimeout (fn []
                                        (fetch-badges state)) 500))))

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
                              (search-timer state))}]]]))

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
       [:option {:value "all" :key "all"} (t :core/All)]
       (for [[country-key country-name] (map identity (:countries @state))]
         [:option {:value country-key
                   :key country-key} country-name])]]]))

(defn order-radio-values []
  [{:value "mtime" :id "radio-date" :label (t :core/bydate)}
   {:value "name" :id "radio-name" :label (t :core/byname)}
   {:value "issuer_content_name" :id "radio-issuer-name" :label (t :core/byissuername)}])

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
  (let [{:keys [id image_file name description issuer_content_name issuer_content_url recipients badge_content_id]} element-data
        badge-id (or badge_content_id id)]
    [:div {:class "col-xs-12 col-sm-6 col-md-4"
           :key id}
     [:div {:class "media grid-container"}
      [:div.media-content
       (if image_file
         [:div.media-left
          [:img {:src (str "/" image_file)}]])
       [:div.media-body
        [:div.media-heading
         [:a.heading-link {:on-click #(open-modal badge-id)}
          name]]
        [:div.media-issuer
         [:a {:href issuer_content_url
              :target "_blank"} issuer_content_name]]
        (if recipients
          [:div.media-recipients
           recipients " " (if (= recipients 1)
                            (t :gallery/recipient)
                            (t :gallery/recipients))])
        [:div.media-description description]]]
      [:div.media-bottom
       [:a.bottom-link {:href (str "/gallery/badgeview/" badge-id)} [:i {:class "fa fa-share-alt"}] (t :badge/Share)]]]]))

(defn gallery-grid [state]
  (let [badges (:badges @state)
        order (keyword (:order @state))
        badges (if (= order :mtime)
                 (sort-by order > badges)
                 (sort-by (comp clojure.string/upper-case order) badges))]
    (into [:div {:class "row"
                 :id    "grid"}]
          (for [element-data badges]
            (badge-grid-element element-data)))))

(defn content [state]
  [:div {:id "badge-gallery"}
   [m/modal-window]
   [gallery-grid-form state]
   (if (:ajax-message @state)
     [:div.ajax-message
      [:i {:class "fa fa-cog fa-spin fa-2x "}]
      [:span (:ajax-message @state)]]
     [gallery-grid state])])

(defn init-data [state user-id]
  (ajax/POST
    (str "/obpv1/gallery/badges/" user-id)
    {:params {:country ""
              :badge ""
              :issuer ""
              :recipient ""}
     :handler (fn [data]
                (let [{:keys [badges countries user-country]} data]
                  (swap! state assoc :badges badges
                                     :countries countries
                                     :country-selected user-country)))}))

(defn handler [site-navi params]
  (let [user-id (:user-id params)
        state (atom {:user-id user-id
                     :badges []
                     :countries []
                     :country-selected "Finland"
                     :advanced-search false
                     :badge-name ""
                     :recipient-name ""
                     :issuer-name ""
                     :order "mtime"
                     :timer nil
                     :ajax-message nil})]
    (init-data state user-id)
    (fn []
      (layout/default site-navi (content state)))))