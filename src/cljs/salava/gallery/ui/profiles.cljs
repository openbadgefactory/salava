(ns salava.gallery.ui.profiles
  (:require [reagent.core :refer [atom cursor]]
            [reagent.session :as session]
            [clojure.string :refer [trim]]
            [salava.core.ui.helper :refer [path-for]]
            [salava.core.ui.ajax-utils :as ajax]
            [salava.core.ui.layout :as layout]
            [salava.core.ui.grid :as g]
            [reagent-modals.modals :as m]
            [salava.user.ui.helper :refer [profile-picture]]
            [salava.core.i18n :refer [t]]
            [salava.core.ui.modal :as mo]
            [salava.core.time :refer [date-from-unix-time]]))

(defn ajax-stop [ajax-message-atom]
  (reset! ajax-message-atom nil))

(defn fetch-users [state]
  (let [{:keys [name country-selected common-badges? order_by]} @state
        ajax-message-atom (cursor state [:ajax-message])]
    (reset! ajax-message-atom (t :gallery/Searchingprofiles))
    (ajax/POST
      (path-for "/obpv1/gallery/profiles/")
      {:params  {:country       country-selected
                 :name          (trim (str name))
                 :common_badges (boolean common-badges?)
                 :order_by      order_by}
       :handler (fn [data] (swap! state assoc :users (:users data)))
       :finally (fn [] (ajax-stop ajax-message-atom))})))

(defn search-timer [state]
  (let [timer-atom (cursor state [:timer])]
    (if @timer-atom
      (js/clearTimeout @timer-atom))
    (reset! timer-atom (js/setTimeout (fn [] (fetch-users state)) 500))))

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
                              (fetch-users state))}
       [:option {:value "all" :key "all"} (t :core/All)]
       (for [[country-key country-name] (map identity (:countries @state))]
         [:option {:value country-key
                   :key country-key} country-name])]]]))

(defn common-badges-checkbox [state]
  (let [common-badges-atom (cursor state [:common-badges?])]
    [:div.form-group
     [:div {:class "col-sm-10 col-sm-offset-2"}
      [:div.checkbox
       [:label
        [:input {:type "checkbox"
                 :checked @common-badges-atom
                 :on-change #(do
                               (reset! common-badges-atom (not @common-badges-atom))
                               (fetch-users state))}](str (t :gallery/Hideuserswithnocommonbadges))]]]]))

(defn order-buttons [state]
  (let [order-atom (cursor state [:order_by])]
    [:div.form-group
     [:label {:class "control-label col-sm-2"} (str (t :core/Order) ":")]
     [:div.col-sm-10
      [:label.radio-inline {:for "radio-date"}
       [:input {:id "radio-date"
                :name "radio-date"
                :type "radio"
                :checked (= @order-atom "ctime")
                :on-change #(do
                              (reset! order-atom "ctime")
                              (fetch-users state))}]
       (t :core/bydatejoined)]
      [:label.radio-inline {:for "radio-name"}
       [:input {:id "radio-name"
                :name "radio-name"
                :type "radio"
                :checked (= @order-atom "name")
                :on-change #(do
                              (reset! order-atom "name")
                              (fetch-users state))}]
       (t :core/byname)]
      [:label.radio-inline {:for "radio-count"}
       [:input {:id "radio-count"
                :name "radio-count"
                :type "radio"
                :checked (= @order-atom "common_badge_count")
                :on-change #(do
                              (reset! order-atom "common_badge_count")
                              (fetch-users state))}]
       (t :core/bycommonbadges)]]]))

(defn profile-gallery-grid-form [state]
  [:div {:id "grid-filter"
         :class "form-horizontal"}
   [:div
    [country-selector state]
    [text-field :name (t :gallery/Username) (t :gallery/Searchbyusername) state]
    [common-badges-checkbox state]]
   [order-buttons state]])

(defn profile-gallery-grid-element [element-data]
  (let [{:keys [id first_name last_name ctime profile_picture common_badge_count]} element-data
        current-user (session/get-in [:user :id])]
    [:div {:class "col-xs-12 col-sm-6 col-md-4"
           :key id}
     [:div {:class "media grid-container"}
      [:a {:href "#" :on-click #(mo/open-modal [:user :profile] {:user-id id}) :style {:text-decoration "none"}}
       [:div.media-content
        [:div.media-left
         [:img {:src (profile-picture profile_picture)
                :alt (str first_name " " last_name)}]]
        [:div.media-body
         [:div {:class "media-heading profile-heading"}
          first_name " " last_name]
         [:div.media-profile
          [:div.join-date
           (t :gallery/Joined) ": " (date-from-unix-time (* 1000 ctime))]]]]
       [:div.common-badges
        (if (= id current-user)
          (t :gallery/ownprofile)
          [:span common_badge_count " " (if (= common_badge_count 1)
                                          (t :gallery/commonbadge) (t :gallery/commonbadges))])]]]]))

(defn profile-gallery-grid [state]
  (let [users (:users @state)]
    (into [:div {:class "row wrap-grid"
                 :id    "grid"}]
          (for [element-data users]
            (profile-gallery-grid-element element-data)))))

(defn content [state]
  [:div
   [m/modal-window]
   [:div {:id "profile-gallery"}
    [profile-gallery-grid-form state]
    (if (:ajax-message @state)
      [:div.ajax-message
       [:i {:class "fa fa-cog fa-spin fa-2x "}]
       [:span (:ajax-message @state)]]
      [profile-gallery-grid state])]])

(defn init-data [state]
  (let [country (session/get-in [:user :country] "all")]
    (ajax/POST
      (path-for (str "/obpv1/gallery/profiles/"))
      {:params {:country country
                :name ""
                :common_badges true
                :order_by "ctime"}
       :handler (fn [{:keys [users countries]} data]
                  (swap! state assoc :users users
                         :countries countries
                         :country-selected country))})))

(defn handler [site-navi]
  (let [state (atom {:users []
                     :countries []
                     :country-selected "all"
                     :name ""
                     :order_by "ctime"
                     :timer nil
                     :ajax-message nil
                     :common-badges? true})]
    (init-data state)
    (fn []
      (layout/default site-navi (content state)))))
