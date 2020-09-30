(ns salava.admin.ui.modal
  (:require
   [reagent.core :refer [atom cursor]]
   [reagent.session :as session]
   [salava.core.ui.grid :as g]
   [salava.core.ui.helper :refer [plugin-fun path-for]]
   [salava.core.ui.ajax-utils :as ajax]
   [salava.core.ui.modal :as mo]
   [salava.gallery.ui.badges :as gallery]
   [salava.core.i18n :refer [t]]
   [salava.admin.ui.report :as report]
   [salava.badgeIssuer.ui.my :as bmy]))

(defn query-params [base]
  {:country (get base :country "all")
   :order (get base :order "mtime")
   :page_count 0
   :name (get base :name "")
   :creator (get base :creator "")})

(defn fetch-more [state]
  (let [page-count-atom (cursor state [:params :page_count])]
    (reset! (cursor state [:fetching-more]) true)
    (ajax/POST
     (path-for "/obpv1/selfie/admin_preview/all")
     {:params  (:params @state)
      :handler (fn [data]
                 ;(value-helper state (get-in data [:tags]))
                 (swap! page-count-atom inc)
                 (swap! state assoc
                        :selfies (into (:selfies @state) (:selfies data))
                        :selfie_count (:selfie_count data)))
      :finally (fn [] (reset! (cursor state [:fetching-more]) false))})))

(defn fetch-selfies [state]
  (let [ajax-message-atom (cursor state [:loading])
        page-count-atom (cursor state [:params :page_count])]
    (reset! ajax-message-atom true)
    (reset! page-count-atom 0)
    (ajax/POST
     (path-for "/obpv1/selfie/admin_preview/all")
     {:params  (:params @state)
      :handler (fn [data]
                 ;(value-helper state (get-in data [:tags]))
                 (swap! page-count-atom inc)
                 (swap! state assoc
                        :selfies (:selfies data)
                        :selfie_count (:selfie_count data)))
      :finally (fn []
                 (reset! ajax-message-atom false))})))

(defn search-timer [state]
  (let [timer-atom (cursor state [:timer])]
    (if @timer-atom
      (js/clearTimeout @timer-atom))
    (reset! timer-atom (js/setTimeout (fn []
                                        (fetch-selfies state)) 500))))

(defn country-selector [state]
  (let [country (cursor state [:params :country])]
    [:div.form-group
     [:label {:class "control-label col-sm-2" :for "country-selector"} (str (t :gallery/Country) ":")]
     [:div.col-sm-10
      [:select {:class     "form-control"
                :id        "country-selector"
                :name      "country"
                :value     @country
                :on-change #(do
                              (reset! country (.-target.value %))
                              (swap! state assoc :params (query-params {:country @country}))
                              (fetch-selfies state))}
       [:option {:value "all" :key "all"} (t :core/All)]
       (for [[country-key country-name] (map identity (:countries @state))]
         [:option {:value country-key
                   :key country-key} country-name])]]]))

(defn text-field [key label placeholder state]
  (let [search-atom (cursor state [:params key])
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

(defn order-radio-values []
  [{:value "mtime" :id "radio-date" :label (t :core/bydate)}
   {:value "name" :id "radio-name" :label (t :core/byname)}
   {:value "recipients" :id "radio-recipients" :label (t :core/byrecipients)}
   {:value "creator" :id "radio-issuer-name" :label (t :badgeIssuer/Badgecreator)}])

(defn gallery-grid-form [state]
  (let [show-advanced-search (cursor state [:advanced-search])]
    [:div {:id "grid-filter"
           :class "form-horizontal"}
     [:div
      [country-selector state]
      [text-field :name (t :gallery/Badgename) (t :gallery/Searchbybadgename) state]
      [text-field :creator (t :badgeIssuer/Badgecreator) "" state]
      [g/grid-radio-buttons (str (t :core/Order) ":") "order" (order-radio-values) [:params :order] state fetch-selfies]]]))

(defn load-more [state]
  (if (pos? @(cursor state [:selfie_count]))
     [:div {:style {:margin "10px 0"}}
      (if @(cursor state [:fetching-more])
        [:span [:i.fa.fa-cog.fa-spin.fa-lg.fa-fw] (str (t :core/Loading) "...")]
        [:span [:a {:href     "#"
                    :id    "loadmore"
                    :on-click #(do
                                 (reset! (cursor state [:fetching-more]) true)
                                 (fetch-more state)
                                 (.preventDefault %))}
                 (str (t :social/Loadmore) " (" @(cursor state [:selfie_count])) " " (t :gallery/Badgesleft) ")"]])]))

(defn gallery-grid [state]
  (let [badges (:selfies @state)]
    [:div#badges (into [:div {:class "row wrap-grid"
                              :id    "grid"}]
                       (for [element-data badges]
                         (bmy/grid-element element-data state))) ;"pickable" gallery/fetch-badges)))
     (load-more state)]))

(defn init-selfie-badges [state]
  (reset! (cursor state [:loading]) true)
  (ajax/POST
   (path-for "/obpv1/selfie/admin_preview/selfie_countries")
   {:handler (fn [data] (swap! state assoc :countries (:countries data)))})

  (ajax/POST
   (path-for "/obpv1/selfie/admin_preview/all")
   {:params (:params @state)
    :handler (fn [data]
               (reset! (cursor state [:selfies]) (:selfies data))
               (reset! (cursor state [:selfie_count]) (:selfie_count data))
               (swap! (cursor state [:params :page_count]) inc))
    :finally (fn [] (reset! (cursor state [:loading]) false))}))

(defn selfies-modal [params]
  (let [params (query-params {:country (session/get-in [:user :country] "all")
                              :order "mtime"
                              :page_count 0
                              :name ""
                              :creator ""})
        state (atom {:params params
                     :selfies []
                     :selfie_count 0
                     :advanced-search false
                     :loading false
                     :fetching-more false})]
    (init-selfie-badges state)
    (fn []
      [:div#badge-gallery
       [:div.col-md-12
        [gallery-grid-form state]
        (if (:ajax-message @state)
          [:div.ajax-message
           [:i {:class "fa fa-cog fa-spin fa-2x "}]
           [:span (str (t :core/Loading) "...")]]
          [gallery-grid state])]])))


(def ^:export modalroutes
  {:report {:badges report/badges-modal}
   :admin {:selfie selfies-modal}})
