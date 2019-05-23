(ns salava.admin.ui.userlist
  (:require [reagent.core :refer [atom cursor]]
            [reagent.session :as session]
            [salava.core.ui.modal :as mo]
            [reagent-modals.modals :as m]
            [clojure.string :refer [trim]]
            [clojure.string :as s]
            [salava.core.ui.helper :refer [path-for]]
            [salava.core.ui.ajax-utils :as ajax]
            [salava.core.ui.layout :as layout]
            [salava.core.ui.grid :as g]
            [salava.user.ui.helper :refer [profile-picture]]
            [salava.core.i18n :refer [t]]
            [salava.core.helper :refer [dump]]
            [salava.admin.ui.admintool :refer [admintool-admin-page]]
            [salava.core.time :refer [date-from-unix-time]]))

(defn ajax-stop [ajax-message-atom]
  (reset! ajax-message-atom nil))

(defn fetch-users [state]
  (let [{:keys [name country-selected common-badges? order_by email filter]} @state
        ajax-message-atom (cursor state [:ajax-message])]
    (reset! ajax-message-atom (t :gallery/Searchingprofiles))
    (ajax/POST
      (path-for "/obpv1/admin/profiles")
      {:params  {:country       country-selected
                 :name          (trim (str name))
                 :order_by      order_by
                 :email         email
                 :filter        filter}
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



(defn filter-buttons [state]
  (let [filter-atom (cursor state [:filter])]
    [:div.form-group
     [:label {:class "control-label col-sm-2"} (str (t :core/Show) ":")]
     [:div.col-sm-10
      [:label.radio-inline {:for "radio-date"}
       [:input {:id "radio-date"
                :name "radio-date"
                :type "radio"
                :checked (= @filter-atom 0)
                :on-change #(do
                              (reset! filter-atom 0)
                              (fetch-users state))}]
       (t :core/All)]
      [:label.radio-inline {:for "radio-name"}
       [:input {:id "radio-name"
                :name "radio-name"
                :type "radio"
                :checked (= @filter-atom 1)
                :on-change #(do
                              (reset! filter-atom 1)
                              (fetch-users state))}]
       (t :admin/Deleted)]
      ]]))

(defn userlist-form [state]
  [:div {:id "grid-filter"
         :class "form-horizontal"}
   [:div
    [country-selector state]
    [text-field :name (t :gallery/Username) (t :gallery/Searchbyusername) state]
    [text-field :email (t :badge/Email)  (t :admin/Searchbyemail) state]
    [filter-buttons state]
    ]
   ])

(defn email-parser [email]
  (let [splitted (s/split email #",")
        list (map #(vec (re-seq #"\S+" %)) splitted)
        email-map (map #(assoc {} :email (get % 0) :primary (js/parseInt (get % 1))) list)]
    email-map))

(defn email-item [{:keys [email primary]}]
  [:div {:key (hash email) :class (if (pos? primary) "primary-address"  "") }  email])

(defn userlist-table-element [element-data state]
  (let [{:keys [id first_name last_name ctime profile_picture common_badge_count email deleted terms]} element-data
        current-user (session/get-in [:user :id])
        email-list (reverse (sort-by :primary (email-parser email)))]
    [:tr {:key id}
     [:td
      (if deleted
        [:div [:i {:class "fa fa-ban" :aria-hidden "true"}] (str " " first_name " " last_name) ]
        [:a {:href "#"
             :on-click #(do
                          (mo/open-modal  [:profile :view] {:user-id id})
                          (.preventDefault %)) }  first_name " " last_name])]
     [:td
      (doall
        (for [i email-list]
          (email-item i)))]
     [:td
      [:div
       (if terms
         (t (keyword (str "social/"terms))) nil)]
      ]
     [:td
      (admintool-admin-page id "user" state fetch-users)

      ]
     ]))

(defn userlist-table [state]
  (let [users (:users @state)]
    [:table {:id "userlist" :class "table"}
     [:thead
      [:tr
       [:th (t :admin/Name)]
       [:th (t :badge/Email)]
       [:th (t :user/Terms)]
       [:th ""]]]
     [:tbody
      (doall
        (for [element-data users]
          (userlist-table-element element-data state)))]]))

(defn content [state]
  [:div
   [m/modal-window]
   [userlist-form state]
   (if (:ajax-message @state)
     [:div.ajax-message
      [:i {:class "fa fa-cog fa-spin fa-2x "}]
      [:span (:ajax-message @state)]]
     [userlist-table state])])

(defn init-data [state]
  (let [country "all"]
    (ajax/POST
      (path-for (str "/obpv1/admin/profiles"))
      {:params {:country "all"
                :name ""
                :order_by "ctime"
                :email ""
                :filter 0}
       :handler (fn [{:keys [users countries]} data]
                  (swap! state assoc :users users
                         :countries countries
                         :country-selected country))})))

(defn handler [site-navi]
  (let [state (atom {:users [{:email ""
                              :last_name ""
                              :first_name ""
                              :id ""
                              :deleted false
                              :terms nil}]
                     :countries []
                     :country-selected "all"
                     :name ""
                     :order_by "ctime"
                     :filter 0
                     :email ""
                     :timer nil
                     :ajax-message nil})]
    (init-data state)
    (fn []
      (layout/default site-navi (content state)))))
