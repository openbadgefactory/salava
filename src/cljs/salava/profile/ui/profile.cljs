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
            [reagent-modals.modals :as m]))

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
             (ph/block-for-edit (cursor blocks [index]) state index))
           )
     [ph/field-after blocks state position]
     ]))

(defn edit-profile [state]
  (let [profile-info-block (pb/userprofileinfo state)]
    [:div#page-edit
     [:div.panel.thumbnail
      [:div.panel-heading
       [:h3 (t :profile/Personalinformation)]]
      [:div.panel-body
       [profile-info-block]]]
     [profile-blocks state]]))

(defn view-profile [state]
  (let [profile-info-block (pb/userprofileinfo state)
        blocks (cursor state [:blocks])]
    [:div
     ; [m/modal-window]
     ; [profile-navi state]
     ;[ph/manage-buttons state]
     [:div.panel.thumbnail
      [:div.panel-heading
       [:h3 (t :profile/Personalinformation)]]
      [:div.panel-body
       [profile-info-block]
       (into [:div]
          (for [index (range (count @blocks))]
             (ph/block (cursor blocks [index]) state index)))]]]))

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
                (swap! state assoc :permission "success" ;:content [view-profile state]
                       )
                (swap! state merge data))}))

(defn handler [site-navi params]
  (let [user-id (:user-id params)
        state (atom {:user-id user-id
                     :permission "initial"
                     :badge-small-view false
                     :pages-small-view true
                     :active-index 0
                     :edit-mode false
                     :toggle-move-mode false
                     :blocks []})
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
