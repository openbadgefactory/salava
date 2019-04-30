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

#_(defn toggle-visibility [visibility-atom]
    (ajax/POST
      (path-for "/obpv1/user/profile/set_visibility")
      {:params  {:visibility (if (= "internal" @visibility-atom) "public" "internal")}
       :handler (fn [new-value]
                  (reset! visibility-atom new-value)
                  )}))

#_(defn profile-visibility-input [visibility-atom]
    [:div.col-xs-12
     [:div.checkbox
      [:label
       [:input {:name      "visibility"
                :type      "checkbox"
                :on-change #(toggle-visibility visibility-atom)
                :checked   (= "public" @visibility-atom)}]
       (t :user/Publishandshare)]]])

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

(defn edit-profile [state]
  (let [profile-info-block (pb/userprofileinfo state)]
    [:div#page-edit
     [:div.panel.thumbnail
      [:div.panel-heading
       [:h3 (t :profile/Personalinformation)]]
      [:div.panel-body
       [profile-info-block]]]
     [ph/block (atom {}) state "badges" 0]
     [ph/block (atom {}) state "pages" 1]]
    )
  )

(defn view-profile [state]
  (let [profile-info-block (pb/userprofileinfo state)]
    [:div
     ; [m/modal-window]
     ; [profile-navi state]
     ;[ph/manage-buttons state]
     [:div.panel.thumbnail
      [:div.panel-heading
       [:h3 (t :profile/Personalinformation)]]
      [:div.panel-body
       [profile-info-block]
       [ph/recent-badges state]
       [ph/recent-pages state]]]]))

(defn content [state]
  [:div
   [m/modal-window]
   [:div#profile
    [profile-navi state]
   [ph/manage-buttons state]]
   (if @(cursor state [:edit-mode])
     [edit-profile state]
     [view-profile state]
     )
   ]
  )



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
                     :toggled nil})
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
