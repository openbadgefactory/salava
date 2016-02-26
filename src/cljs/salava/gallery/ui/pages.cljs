(ns salava.gallery.ui.pages
  (:require [reagent.core :refer [atom cursor]]
            [reagent-modals.modals :as m]
            [clojure.string :refer [trim]]
            [salava.core.ui.ajax-utils :as ajax]
            [salava.page.ui.helper :refer [view-page-modal]]
            [salava.core.ui.layout :as layout]
            [salava.core.ui.grid :as g]
            [salava.core.i18n :refer [t]]
            [salava.core.time :refer [date-from-unix-time]]
            [salava.gallery.ui.badge-content :refer [badge-content-modal]]))

(defn open-modal [page-id]
  (ajax/GET
    (str "/obpv1/page/view/" page-id)
    {:handler (fn [data]
                (m/modal! [view-page-modal (:page data)] {:size :lg}))}))

(defn ajax-stop [ajax-message-atom]
  (reset! ajax-message-atom nil))

(defn fetch-pages [state]
  (let [{:keys [user-id country-selected owner-name]} @state
        ajax-message-atom (cursor state [:ajax-message])]
    (reset! ajax-message-atom (t :gallery/Searchingpages))
    (ajax/POST
      (str "/obpv1/gallery/pages/" user-id)
      {:params  {:country (trim country-selected)
                 :owner   (trim owner-name)}
       :handler (fn [data]
                  (swap! state assoc :pages (:pages data)))
       :finally (fn []
                  (ajax-stop ajax-message-atom))})))

(defn search-timer [state]
  (let [timer-atom (cursor state [:timer])]
    (if @timer-atom
      (js/clearTimeout @timer-atom))
    (reset! timer-atom (js/setTimeout (fn []
                                        (fetch-pages state)) 500))))

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
                             (fetch-pages state))}
       [:option {:value "all" :key "all"} (t :core/All)]
       (for [[country-key country-name] (map identity (:countries @state))]
         [:option {:value country-key
                   :key country-key} country-name])]]]))

(defn order-radio-values []
  [{:value "mtime" :id "radio-date" :label (t :core/bydate)}
   {:value "name" :id "radio-name" :label (t :core/byname)}])

(defn page-gallery-grid-form [state]
  [:div {:id "grid-filter"
         :class "form-horizontal"}
   (if (not (:user-id @state))
     [:div
      [country-selector state]
      [text-field :owner-name (t :gallery/Pageowner) (t :gallery/Searchbypageowner) state]])
   [g/grid-radio-buttons (str (t :core/Order) ":") "order" (order-radio-values) :order state]])

(defn page-gallery-grid-element [element-data]
  (let [{:keys [id name first_name last_name badges mtime]} element-data]
    [:div {:class "col-xs-12 col-sm-6 col-md-4"
           :key id}
     [:div {:class "media grid-container"}
      [:div.media-content
       [:div.media-body
        [:div.media-heading
         [:a.heading-link {:on-click #(open-modal id)}
          name]]
        [:div.media-content
         [:div.page-owner
          [:a {:href "#"}
           first_name " " last_name]]
         [:div.page-create-date
          (date-from-unix-time (* 1000 mtime) "minutes")]
         (into [:div.page-badges]
               (for [badge badges]
                 [:img {:title (:name badge)
                        :src (str "/" (:image_file badge))}]))]]
       [:div {:class "media-right"}
        [:img {:src "/img/user_default.png"}]]]]]))

(defn page-gallery-grid [state]
  (let [pages (:pages @state)
        order (keyword (:order @state))
        pages (if (= order :mtime)
                 (sort-by order > pages)
                 (sort-by order pages))]
    (into [:div {:class "row"
                 :id    "grid"}]
          (for [element-data pages]
            (page-gallery-grid-element element-data)))))

(defn content [state]
  [:div {:id "page-gallery"}
   [m/modal-window]
   [page-gallery-grid-form state]
   (if (:ajax-message @state)
     [:div.ajax-message
      [:i {:class "fa fa-cog fa-spin fa-2x "}]
      [:span (:ajax-message @state)]]
     [page-gallery-grid state])])

(defn init-data [state user-id]
  (ajax/POST
    (str "/obpv1/gallery/pages/" user-id)
    {:params {:country ""
              :owner ""}
     :handler (fn [data]
                (let [{:keys [pages countries user-country]} data]
                  (swap! state assoc :pages pages
                         :countries countries
                         :country-selected user-country)))}))

(defn handler [site-navi params]
  (let [user-id (:user-id params)
        state (atom {:user-id user-id
                     :pages []
                     :countries []
                     :country-selected "Finland"
                     :owner-name ""
                     :order "mtime"
                     :timer nil
                     :ajax-message nil})]
    (init-data state user-id)
    (fn []
      (layout/default site-navi (content state)))))
