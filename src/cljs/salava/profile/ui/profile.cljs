(ns salava.profile.ui.profile
  (:require [salava.core.ui.helper :refer [plugin-fun path-for]]
            [salava.core.ui.ajax-utils :as ajax]
            [salava.profile.ui.block :as pb]
            [salava.core.ui.error :as err]
            [reagent.core :refer [atom]]
            [reagent.session :as session]
            [salava.core.ui.layout :as layout]
            [salava.core.i18n :refer [t]]))

(defn tabs [state])

(defn profile-navi [state]
  [:div.profile-navi
   [:ul.nav.nav-tabs
    [:li.nav-item {:class (if (= 0 (:active-index @state)) "active")}
     [:a.nav-link {:on-click #(do
                                (.prevent-default %)
                                (swap! state assoc :active-index 0))} (t :user/Myprofile)]]
    (when (:edit-mode @state) [:i.fa.fa-plus-square])
    ]
   ]
  )
(defn top-buttons [state])

(defn recent-badges [state]
 (let [block (first (plugin-fun (session/get :plugins) "block" "recentbadges"))]
   (if block [block state] [:div ""])))

(defn view-profile [state]
  (let [profile-info-block (pb/userprofileinfo state)
        recent-badges-block ()
        recent-pages-block ()]

    [:div#profile
     [profile-navi state]
     [:div.panel.thumbnail
      [:div.panel-heading
       [:h3 (t :profile/Personalinformation)]
       ]
      [:div.panel-body [profile-info-block]]]
     [:div.field.thumbnail
      [recent-badges state]
      ]

     ]
    )
  )

(defn edit-profile [state])

(defn init-data [user-id state]
  (ajax/GET
    (path-for (str "/obpv1/profile/" user-id) true)
    {:handler (fn [data]
                (swap! state assoc :permission "success" )
                (swap! state merge data))}))

(defn handler [site-navi params]
  (let [user-id (:user-id params)
        state (atom {:user-id user-id
                     :permission "initial"
                     :badge-small-view false
                     :pages-small-view true
                     :active-index 0
                     :edit-mode true
                     })
        user (session/get :user)]
    (init-data user-id state)

    (fn []
      (cond
        (= "initial" (:permission @state)) (layout/default site-navi [:div])
        (and user (= "error" (:permission @state)))(layout/default-no-sidebar site-navi (err/error-content))
        (= "error" (:permission @state)) (layout/landing-page site-navi (err/error-content))
        (= (:id user) (js/parseInt user-id)) (layout/default site-navi (view-profile state))
        (and (= "success" (:permission @state)) user) (layout/default-no-sidebar site-navi (view-profile state))
        :else (layout/landing-page site-navi (view-profile state))))))
