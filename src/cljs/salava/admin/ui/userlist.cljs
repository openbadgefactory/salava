(ns salava.admin.ui.userlist
  (:require [reagent.core :refer [atom cursor]]
            [reagent.session :as session]
            [clojure.string :refer [trim]]
            [salava.core.ui.helper :refer [path-for]]
            [salava.core.ui.ajax-utils :as ajax]
            [salava.core.ui.layout :as layout]
            [salava.core.ui.grid :as g]
            [salava.user.ui.helper :refer [profile-picture]]
            [salava.core.i18n :refer [t]]
            [salava.core.time :refer [date-from-unix-time]]))




(defn ajax-stop [ajax-message-atom]
  (reset! ajax-message-atom nil))

(defn fetch-users [state]
  (let [{:keys [name country-selected common-badges? order_by]} @state
        ajax-message-atom (cursor state [:ajax-message])]
    (reset! ajax-message-atom (t :gallery/Searchingprofiles))
    (ajax/POST
      (path-for "/obpv1/admin/profiles/")
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

(defn deleted-users-checkbox [state]
  (let [common-badges-atom (cursor state [:common-badges?])]
    [:div.form-group
     [:div {:class "col-sm-10 col-sm-offset-2"}
      [:div.checkbox
       [:label
        [:input {:type "checkbox"
                 :checked @common-badges-atom
                 :on-change #(do
                              (reset! common-badges-atom (not @common-badges-atom))
                              (fetch-users state))}](str "piilotetut käyttäjät")]]]]))

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

(defn userlist-form [state]
  [:div {:id "grid-filter"
         :class "form-horizontal"}
   [:div
    [country-selector state]
    [text-field :name (t :gallery/Username) (t :gallery/Searchbyusername) state]
    
    [deleted-users-checkbox state]
    ]
   ])

(defn userlist-table-element [element-data]
  (let [{:keys [id first_name last_name ctime profile_picture common_badge_count email deleted]} element-data
        current-user (session/get-in [:user :id])]
    [:tr {:key id}
     [:th [:a {:href (path-for (str "/user/profile/" id))} first_name " " last_name]]
     [:th
      email]
     [:th
          [:button {:class "btn btn-default"} "unlock"]]
     ]))

(defn userlist-table [state]
  (let [users (:users @state)]
    (into [:table {:class "table"}]
          (for [element-data users]
            (userlist-table-element element-data)))))

(defn content [state]
  [:div {:id "profile-gallery"}
   [userlist-form state]
   (if (:ajax-message @state)
     [:div.ajax-message
      [:i {:class "fa fa-cog fa-spin fa-2x "}]
      [:span (:ajax-message @state)]]
     [userlist-table state])])

(defn init-data [state]
  (let [country (session/get-in [:user :country] "all")]
    (ajax/POST
      (path-for (str "/obpv1/admin/profiles/"))
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
                     :common-badges? nil})]
    (init-data state)
    (fn []
      (layout/default site-navi (content state)))))

