(ns salava.profile.ui.profile
  (:require [salava.core.ui.helper :refer [plugin-fun path-for not-activated? private?]]
            [salava.core.ui.ajax-utils :as ajax]
            [salava.profile.ui.block :as pb]
            [salava.core.ui.error :as err]
            [reagent.core :refer [atom cursor]]
            [reagent.session :as session]
            [salava.core.ui.layout :as layout]
            [salava.core.i18n :refer [t]]
            [salava.profile.ui.helper :as ph]
            [reagent-modals.modals :as m]
            [cljs-uuid-utils.core :refer [make-random-uuid uuid-string]]
            [salava.profile.ui.edit :as pe]
             [salava.page.themes :refer [themes borders]]))


(defn random-key []
  (-> (make-random-uuid)
      (uuid-string)))

(defn tabs [state])

(defn connect-user [user-id]
  (let [connectuser (first (plugin-fun (session/get :plugins) "block" "connectuser"))]
    (if connectuser
      [connectuser user-id]
      [:div ""])))

(defn profile-navi [state]
  [:div.profile-navi
   [:ul.nav.nav-tabs
    [:li.nav-item {:class (if (= 0 (:active-index @state)) "active")}
     [:a.nav-link {:on-click #(do
                                (.prevent-default %)
                                (swap! state assoc :active-index 0))} (t :user/Myprofile)]]
    (when (:edit-mode @state) [:i.fa.fa-plus-square])]])

(defn profile-blocks [state]
  (let [blocks (cursor state [:blocks])
        block-count (count @blocks)
        position (if (pos? block-count) (dec block-count) nil)]
    [:div {:id "field-editor"}
     (into [:div {:id "page-blocks"}]
           (for [index (range (count @blocks))]
             (ph/block-for-edit (cursor blocks [index]) state index)))
     [ph/field-after blocks state nil]
     ]))

(defn edit-profile-content [state]
  [:div#page-edit
   [:div.panel.thumbnail
    [:div.panel-heading
     [:h3 (t :profile/Personalinformation)]]
    [:div.panel-body
     [pe/edit-profile]]]
   [profile-blocks state]])

(defn view-profile [state]
  (let [profile-info-block (pb/userprofileinfo state)
        blocks (cursor state [:blocks])]
    [:div#page-view
     [:div {:id (str "theme-" (or @(cursor state [:theme]) 0))
            :class "page-content"}
     ; [m/modal-window]
     ; [profile-navi state]
     ;[ph/manage-buttons state]
     [:div.panel
      [:div.panel-left
       [:div.panel-right
        [:div.panel-content
      [:div.panel-heading
       [:h3 (t :profile/Personalinformation)]]
      [:div.panel-body
       [profile-info-block]
       (into [:div]
             (for [index (range (count @blocks))]
               (ph/block (cursor blocks [index]) state index)))]]]]]]]))

(defn theme-selection [theme-atom themes]
  (reduce (fn [r theme]
            (conj r [:div {:id (str "theme-" (:id theme))}
                     [:a {:href "#" :on-click #(do
                                                 (.preventDefault %)
                                                 (reset! theme-atom (js/parseInt (:id theme)) ))
                          :alt (t (:name theme)) :title (t (:name theme))}[:div {:class (str "panel-right theme-container" (if (= @theme-atom (:id theme)) " selected"))} " " ]]])
            )[:div {:id "theme-container"}] themes))

(defn edit-theme [state]
      [:div {:id "page-edit-theme"}
     ;[ph/edit-page-header (t :page/Choosetheme ": " name)]
     #_[ph/edit-page-buttons id :theme state]
     ;[ph/edit-page-buttons id :theme (fn [next-url] (save-theme state next-url)) state]
     [:div {:class "panel page-panel thumbnail" :id "theme-panel"}
      [:div.panel-heading
       [:h3 (t :page/Selecttheme)]]
      [:div.panel-body
        [theme-selection (cursor state [:theme]) themes]]
      #_[:form.form-horizontal
       [:div.form-group
        [:label.col-xs-4 {:for "select-theme"}
         (str (t :page/Selecttheme) ":")]
        [:div.col-xs-8
         [theme-selection (cursor state [:theme]) themes]]]
       #_[:div.form-group
        [:label.col-xs-4 {:for "select-padding"}
         (str (t :page/Selectpadding) ":")]
        [:div.col-xs-8
         [padding-selection (cursor state [:page :padding])]]]
       #_[:div.form-group
        [:label.col-xs-4 {:for "select-border"}
         (str (t :page/Selectborder) ":")]
        #_[:div.col-xs-8
         [border-selection (cursor state [:page :border]) borders]]]
       #_[:div.row
          [:div.col-md-12
           [:button {:class    "btn btn-primary"
                     :on-click #(do
                                  (.preventDefault %)
                                  (save-theme state (str "/profile/page/settings/" id)))}
            (t :page/Save)]]]]]
     #_[ph/manage-page-buttons :theme (cursor state [:page :id]) state]
     #_[ph/manage-page-buttons (fn [] (save-theme state (str "/profile/page/settings/" id))) state (str "/profile/page/settings/" id) (str "/profile/page/edit/" id) false]

     [view-profile state]]
  )

(defn edit-profile [state]
  (let [profile-info-block (pb/userprofileinfo state)
        content @(cursor state [:edit :active-tab])]
    [:div

    (case content
      :content [edit-profile-content state]
      :theme [edit-theme state]
      nil)]

    #_[:div#page-edit
     [:div.panel.thumbnail
      [:div.panel-heading
       [:h3 (t :profile/Personalinformation)]]
      [:div.panel-body

       [pe/edit-profile]
       #_[profile-info-block]]]
     [profile-blocks state]]))

#_(defn view-profile [state]
  (let [profile-info-block (pb/userprofileinfo state)
        blocks (cursor state [:blocks])]
    [:div#page-view
     [:div {:id (str "theme-" (or @(cursor state [:theme]) 0))
            :class "page-content"}
     ; [m/modal-window]
     ; [profile-navi state]
     ;[ph/manage-buttons state]
     [:div.panel.thumbnail
      [:div.panel-left
       [:div.panel-right
        [:div.panel-content
      [:div.panel-heading
       [:h3 (t :profile/Personalinformation)]]
      [:div.panel-body
       [profile-info-block]
       (into [:div]
             (for [index (range (count @blocks))]
               (ph/block (cursor blocks [index]) state index)))]]
     [:div.col-md-1 ""]]]]]]))

(defn content [state]
  [:div
   [m/modal-window]
   [:div#profile
    [profile-navi state]
    [ph/manage-buttons state]]
   (if @(cursor state [:edit-mode])
     [edit-profile state]
     [view-profile state])])



(defn init-data [user-id state]
  (ajax/GET
    (path-for (str "/obpv1/profile/" user-id) true)
    {:handler (fn [data]
                (let [data-with-uuids (assoc data :blocks (vec (map #(assoc % :key (random-key))
                                                                    (get data :blocks))))]
                  (swap! state assoc :permission "success")
                  (swap! state merge data-with-uuids)))}))

(defn handler [site-navi params]
  (let [user-id (:user-id params)
        state (atom {:user-id user-id
                     :permission "initial"
                     :badge-small-view false
                     :pages-small-view true
                     :active-index 0
                     :edit-mode false
                     :toggle-move-mode false
                     :blocks []
                     :edit {:active-tab :content}
                     :theme 0})
        user (session/get :user)]
    (init-data user-id state)

    (fn []
      (cond
        (= "initial" (:permission @state)) (layout/default site-navi [:div])
        (and user (= "error" (:permission @state)))(layout/default-no-sidebar site-navi (err/error-content))
        (= "error" (:permission @state)) (layout/landing-page site-navi (err/error-content))
        (= (:id user) (js/parseInt user-id)) (layout/default site-navi (content state))
        (and (= "success" (:permission @state)) user) (layout/default-no-sidebar site-navi (content state))
        :else (layout/landing-page site-navi (content state))))))
