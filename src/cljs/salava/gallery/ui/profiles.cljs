(ns salava.gallery.ui.profiles
  (:require [reagent.core :refer [atom cursor]]
            [reagent.session :as session]
            [clojure.string :refer [trim]]
            [salava.core.ui.helper :refer [path-for plugin-fun]]
            [salava.core.ui.ajax-utils :as ajax]
            [salava.core.ui.layout :as layout]
            [salava.core.ui.grid :as g]
            [reagent-modals.modals :as m]
            [salava.user.ui.helper :refer [profile-picture]]
            [salava.core.i18n :refer [t]]
            [salava.core.ui.modal :as mo]
            [salava.core.time :refer [date-from-unix-time]]
            [salava.core.helper :refer [dump]]))

(defn ajax-stop [ajax-message-atom]
  (reset! ajax-message-atom nil))

(defn fetch-users [state]
  (let [{:keys [name country-selected common-badges? order_by url space]} @state
        ajax-message-atom (cursor state [:ajax-message])]
    (reset! ajax-message-atom (t :gallery/Searchingprofiles))
    (ajax/POST
      (path-for (or url "/obpv1/gallery/profiles/"))
      {:params  {:country       country-selected
                 :name          (trim (str name))
                 :common_badges (boolean common-badges?)
                 :order_by      order_by
                 :space-id      (int space)
                 :custom-field-filters @(cursor state [:custom-field-filters])}
       :handler (fn [data] (swap! state assoc :users (:users data)))
       :finally (fn [] (ajax-stop ajax-message-atom))})))

(defn fetch-users+ [state]
  (let [{:keys [name country-selected common-badges? order_by url email]} @state
        ajax-message-atom (cursor state [:ajax-message])]
    (reset! ajax-message-atom (t :gallery/Searchingprofiles))
    (reset! (cursor state [:page_count]) 0)
    (reset! (cursor state [:select-all?]) false)
    (ajax/POST
      (path-for (or url "/obpv1/gallery/profiles"))
      {:params  {:country       country-selected
                 :name          (trim (str name))
                 :common_badges (boolean common-badges?)
                 :order_by      order_by
                 :email         email
                 :page_count @(cursor state [:page_count])}
       :handler (fn [data] (swap! state assoc :users (:users data) :users_count (:users_count data) :page_count (inc @(cursor state [:page_count]))))
       :finally (fn [] (ajax-stop ajax-message-atom))})))

(defn get-more-users [state]
  (let [{:keys [name country-selected common-badges? order_by url email]} @state
        ajax-message-atom (cursor state [:ajax-message])
        page-count-atom (cursor state [:page_count])]
    (ajax/POST
     (path-for (or url "/obpv1/gallery/profiles"))
     {:params  {:country       country-selected
                :name          (trim (str name))
                :common_badges (boolean common-badges?)
                :order_by      order_by
                :email         email
                :page_count @page-count-atom}
      :handler (fn [data]
                 (swap! page-count-atom inc)
                 (swap! state assoc
                        :users (into (:users @state) (:users data))
                        :users_count (:users_count data)))
      :finally (fn [])})))

(defn load-more [state]
  (if (pos? (:users_count @state))
    [:div.col-md-12 {:style {:font-weight 600}}
     ;[:div {:class "media message-item"}
      ;[:div {:class "media-body"}
       [:span [:a {:href     "#"
                   :id    "loadmore"
                   :on-click #(do
                                (get-more-users state)
                                (.preventDefault %))}

               (str (t :social/Loadmore) " (" (:users_count @state) " " (t :extra-spaces/usersleft) ")")]]]))

(defn search-timer [state]
  (let [timer-atom (cursor state [:timer])]
    (if @timer-atom
      (js/clearTimeout @timer-atom))
    (reset! timer-atom (js/setTimeout (fn [] (fetch-users state)) 500))))

(defn search-timer+ [state]
  (let [timer-atom (cursor state [:timer])]
    (if @timer-atom
      (js/clearTimeout @timer-atom))
    (reset! timer-atom (js/setTimeout (fn [] (fetch-users+ state)) 500))))

(defn text-field
 ([opts]
  (let [{:keys [key label placeholder state modal?]} opts
         search-atom (cursor state [key])
         field-id (if modal? (str key "-field-modal") (str key "-field"))]
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

 ([key label placeholder state]
  [text-field {:key key :label label :placeholder placeholder :state state :modal? false}]
  #_(let [search-atom (cursor state [key])
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
                                 (search-timer state))}]]])))

(defn text-field+ [opts]
  (let [{:keys [key label placeholder state modal?]} opts
        search-atom (cursor state [key])
        field-id (if modal? (str key "-field-modal") (str key "-field"))]
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
                              (search-timer+ state))}]]]))
(defn country-selector
  ([state]
   (let [country-atom (cursor state [:country-selected])]
     [:div.form-group
      [:label {:class "control-label col-sm-2" :for "country-selector"} (str (t :gallery/Country) ": ")]
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
  ([state modal?]
   (let [country-atom (cursor state [:country-selected])]

     [:div.form-group
      [:label {:class "control-label col-sm-2" :for "country-selector-modal"} (str (t :gallery/Country) ": ")]
      [:div.col-sm-10
       [:select {:class     "form-control"
                 :id        "country-selector-modal"
                 :name      "country"
                 :value     @country-atom
                 :on-change #(do
                               (reset! country-atom (.-target.value %))
                               (fetch-users state))}
        [:option {:value "all" :key "all"} (t :core/All)]
        (for [[country-key country-name] (map identity (:countries @state))]
          [:option {:value country-key
                    :key country-key} country-name])]]])))

(defn country-selector+ [state]
  (let [country-atom (cursor state [:country-selected])]

    [:div.form-group
     [:label {:class "control-label col-sm-2" :for "country-selector-modal"} (str (t :gallery/Country) ": ")]
     [:div.col-sm-10
      [:select {:class     "form-control"
                :id        "country-selector-modal"
                :name      "country"
                :value     @country-atom
                :on-change #(do
                              (reset! country-atom (.-target.value %))
                              (fetch-users+ state))}
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

(defn order-buttons
  ([state]
   (let [order-atom (cursor state [:order_by])]
     [:div.form-group
      [:span._label.filter-opt {:class "control-label col-sm-2" :aria-label (str (t :core/Order))}
       (str (t :core/Order) ":")]
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

  ([state modal?]
   (let [order-atom (cursor state [:order_by])]
     [:div.form-group
      [:span._label {:class "control-label filter-opt col-sm-2"} (str (t :core/Order) ":")]
      [:div.col-sm-10
       [:label.radio-inline {:for "radio-date_"}
        [:input {:id "radio-date_"
                 :name "radio-date_"
                 :type "radio"
                 :checked (= @order-atom "ctime")
                 :on-change #(do
                               (reset! order-atom "ctime")
                               (fetch-users state))}]
        (t :core/bydatejoined)]
       [:label.radio-inline {:for "radio-name_"}
        [:input {:id "radio-name_"
                 :name "radio-name_"
                 :type "radio"
                 :checked (= @order-atom "name")
                 :on-change #(do
                               (reset! order-atom "name")
                               (fetch-users state))}]
        (t :core/byname)]
       [:label.radio-inline {:for "radio-count_"}
        [:input {:id "radio-count_"
                 :name "radio-count_"
                 :type "radio"
                 :checked (= @order-atom "common_badge_count")
                 :on-change #(do
                               (reset! order-atom "common_badge_count")
                               (fetch-users state))}]
        (t :core/bycommonbadges)]]])))

(defn order-buttons+ [state]
 (let [order-atom (cursor state [:order_by])]
   [:div.form-group
    [:span._label {:class "control-label filter-opt col-sm-2"} (str (t :core/Order) ":")]
    [:div.col-sm-10
     [:label.radio-inline {:for "radio-date_"}
      [:input {:id "radio-date_"
               :name "radio-date_"
               :type "radio"
               :checked (= @order-atom "ctime")
               :on-change #(do
                             (reset! order-atom "ctime")
                             (fetch-users+ state))}]
      (t :core/bydatejoined)]
     [:label.radio-inline {:for "radio-name_"}
      [:input {:id "radio-name_"
               :name "radio-name_"
               :type "radio"
               :checked (= @order-atom "name")
               :on-change #(do
                             (reset! order-atom "name")
                             (fetch-users+ state))}]
      (t :core/byname)]]]))

(defn custom-field-filters [field state]
  (into [:div]
    (for [f (plugin-fun (session/get :plugins) field "custom_field_filter")]
       (when f [f state (fn [] (fetch-users state))])))) ;(:users @state)]))))

(defn profile-gallery-grid-form
  ([state]
   [:div {:id "grid-filter"
          :class "form-horizontal"}
    [:div
     (if (empty? (session/get-in [:user :current-space]))
         [country-selector state]
         (into [:div]
          (for [f (plugin-fun (session/get :plugins) "block" "gallery_profiles_space_select")]
           (when (ifn? f) [f state (fn [] (fetch-users state))]))))
     [text-field :name (t :gallery/Username) (t :gallery/Searchbyusername) state]
     [common-badges-checkbox state]]
    [order-buttons state]
    (when (= "admin" (session/get-in [:user :role] "user"))
      [:div
       [custom-field-filters "gender" state]
       [custom-field-filters "organization" state]])])


  ([state modal?]
   (if-not modal?
     [profile-gallery-grid-form state]
     [:div {:id "grid-filter-modal"
            :class "form-horizontal"}
      [:div
       [country-selector state modal?]
       [text-field {:key :name :label (t :gallery/Username) :placeholder (t :gallery/Searchbyusername) :state state :modal? true}]
       [common-badges-checkbox state]]
      [order-buttons state modal?]])))

(defn profile-gallery-grid-form+ [state]
  [:div#grid-filter-modal.form-horizontal
   [:div
    [country-selector+ state]
    [text-field+ {:key :name :label (t :gallery/Username) :placeholder (t :gallery/Searchbyusername) :state state :modal? true}]
    [text-field+ {:key :email :label (t :badge/Email) :placeholder (t :admin/Searchbyemail) :state state :modal? true}]]
   [order-buttons+ state]])

(defn profile-gallery-grid-element [element-data]
  (let [{:keys [id first_name last_name ctime profile_picture common_badge_count]} element-data
        current-user (session/get-in [:user :id])]
    [:div {:class "col-xs-12 col-sm-6 col-md-4"
           :key id}
     [:div {:class "media grid-container"}
      [:a {:href "#" :on-click #(mo/open-modal [:profile :view] {:user-id id}) :style {:text-decoration "none"}}
       [:div.media-content
        [:div.media-left
         [:img {:src (profile-picture profile_picture)
                :alt "" #_(str first_name " " last_name)}]]
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
  (let [country (session/get-in [:user :country] "all")
        filter-options (session/get :filter-options nil)
        common-badges? (if filter-options (:common-badges filter-options) true)
        ajax-message-atom (cursor state [:ajax-message])
        space-id (session/get-in [:user :current-space :id] 0)]
    (reset! ajax-message-atom (str (t :core/Loading) "..."))
    (ajax/POST
      (path-for (str "/obpv1/gallery/profiles/"))
      {:params {:country (if (pos? space-id) "all" (session/get-in [:filter-options :country] country))
                :name ""
                :common_badges common-badges?
                :order_by "ctime"
                :space-id space-id}
       :handler (fn [{:keys [users countries]} data]
                  (swap! state assoc :users users
                         :countries countries
                         :country-selected (session/get-in [:filter-options :country] country)))
       :finally (fn []
                  (ajax-stop ajax-message-atom))})))

(defn handler [site-navi]
  (let [ filter-options (session/get :filter-options nil)
         state (atom {:users []
                      :countries []
                      :country-selected "all"
                      :name ""
                      :order_by "ctime"
                      :timer nil
                      :ajax-message nil
                      :common-badges? (if filter-options (:common-badges filter-options) true)
                      :space (session/get-in [:user :current-space :id] 0)})]
    (init-data state)
    (fn []
      (layout/default site-navi (content state)))))
