(ns salava.extra.socialuser.ui.connections
  (:require [reagent.core :refer [atom cursor]]
            [salava.core.ui.ajax-utils :as ajax]
            [salava.core.helper :refer [dump]]
            [salava.core.ui.helper :refer [path-for]]
            [salava.core.ui.modal :as mo]
            [salava.user.ui.helper :refer [profile-picture]]
            [salava.core.i18n :refer [t]]
            [reagent-modals.modals :as m]
            [salava.core.ui.layout :as layout]
            [reagent.session :as session]))


(defn init-data [state]
  (ajax/GET
    (path-for (str "/obpv1/socialuser/connections" ))
    {:handler (fn [data]
                (swap! state assoc :followers-users (:followers-users data)
                       :following-users (:following-users data)
                       :visible-area (session/get! :visible-area nil)))}))



(defn deleteconnect [user-id state]
  [:a {:href "#" :on-click #(ajax/DELETE
                              (path-for (str "/obpv1/socialuser/user-connection/" user-id))
                              {:response-format :json
                               :keywords?       true
                               :handler         (fn [data]
                                                  (do
                                                    (init-data state)))
                               :error-handler   (fn [{:keys [status status-text]}]
                                                  (.log js/console (str status " " status-text))
                                                  )})} (t :social/Unfollow) ])


(defn accepted-user-connections [state users visible-area-atom]
  (let [panel-identity :accepted]
    [:div.panel
     [:div.panel-heading
      [:a {:href "#" :on-click #(do (.preventDefault %) (reset! visible-area-atom panel-identity))}
       [:h3
        (str (t :social/Followedusers) " (" (count users) ")")]]]
     (when (= panel-identity @visible-area-atom)
       [:div.panel-body
        [:table {:class "table" :summary (t :social/Followedusers)}
         [:thead
          [:tr
           [:th ""]
           [:th ""]
           [:th ""]]]
         (into [:tbody]
               (for [user users
                     :let [{:keys [user_id profile_picture first_name last_name status]} user]]
                 [:tr
                  [:td.icon [:img.badge-icon {:src (profile-picture profile_picture)
                                              :alt user_id}]]
                  [:td.name [:a {:href     "#"
                                 :on-click #(do
                                              (mo/open-modal [:profile :view] {:user-id user_id})
                                              ;(b/open-modal id false init-data state)
                                              (.preventDefault %)) } (str first_name " " last_name)]]
                  [:td.action  (if (= "accepted" status) (deleteconnect user_id state)  (str (t :social/Pending) "..."))]]))]]
       )]))


(defn accept [owner-id state]
  [:a {:class "btn btn-primary btn-xs"
       :href     "#"
       :on-click #(ajax/POST
                    (path-for (str "/obpv1/socialuser/user-pending-requests/" owner-id "/accepted"))
                    {:response-format :json
                     :keywords?       true
                     :handler         (fn [data]
                                        (do
                                          (init-data state)))
                     :error-handler   (fn [{:keys [status status-text]}]
                                        (.log js/console (str status " " status-text))
                                        )})} (t :social/Accept)])

(defn decline [owner-id state]
  [:a {:class  "btn btn-warning btn-xs"
       :href     "#"
       :on-click #(ajax/POST
                    (path-for (str "/obpv1/socialuser/user-pending-requests/" owner-id "/declined"))
                    {:response-format :json
                     :keywords?       true
                     :handler         (fn [data]
                                        (do
                                          (init-data state)))
                     :error-handler   (fn [{:keys [status status-text]}]
                                        (.log js/console (str status " " status-text))
                                        )})} (t :social/Decline)])




(defn pending-user-connections [state users visible-area-atom]
  (let [panel-identity :pending]
    [:div.panel
     [:div.panel-heading
      [:a {:href "#" :on-click #(do (.preventDefault %) (reset! visible-area-atom panel-identity))}
       [:h3
        (str (t :social/Followersusers) " (" (count users) ")")]]]
     (when (= panel-identity @visible-area-atom)
       [:div.panel-body
        [:table {:class "table" :summary (t :badge/Badgeviews)}
         [:thead
          [:tr
           [:th ""]
           [:th ""]
           [:th ""]]]

         (into [:tbody]
               (for [user users
                     :let [{:keys [owner_id profile_picture first_name last_name status]} user]]
                 [:tr
                  [:td.icon [:img.badge-icon {:src (profile-picture profile_picture)
                                              :alt (str first_name last_name)}]]
                  [:td.name [:a {:href     "#"
                                 :on-click #(do
                                              (mo/open-modal [:profile :view] {:user-id owner_id})
                                              ;(b/open-modal id false init-data state)
                                              (.preventDefault %)) } (str first_name " " last_name)]]
                  (if (= "pending" status)
                    [:td.action (accept owner_id state) " " (decline owner_id state)]
                    [:td.action  (str(t :social/Follows)  " ") ])]))]])]))




(defn content [state]
  (let [visible-area-atom (cursor state [:visible-area])
        followers-users     (cursor state [:followers-users])
        following-users    (cursor state [:following-users])]
    [:div
     [m/modal-window]
     [:h1.uppercase-header (t :connections/UserConnections)]

     [:div {:style {:margin-bottom "10px"}} (t :connections/Userconnectionsinfo)]

     (if-not (empty? @followers-users)
       [:div {:id "badge-stats"}
        (pending-user-connections state @followers-users visible-area-atom)])
     (if-not (empty? @following-users)
       [:div {:id "badge-stats"}
        (accepted-user-connections state @following-users visible-area-atom)])]))

(defn init-stats [state]
  (init-data state)
  (ajax/GET
    (path-for (str "/obpv1/socialuser/user-pending-requests"))
    {:handler (fn [data]
                (swap! state assoc :pending data))}))

(defn stats []
  (let [state (atom {})]
    (init-stats state)
    (fn []
      [:div
       [:div.info-block
        [:a {:href (path-for "/connections/user") :on-click #(do
                                                               (.preventDefault %)
                                                               (session/put! :visible-area :accepted) )}
         [:div.info
          ;[:i.fa.fa-user.icon]
          [:div.text
           [:p.num (->> (:following-users @state) count)]
           [:p.desc (t :social/Followedusers)]
           ]]
         ]
        ]

       [:div.info-block
        [:a {:href (path-for "/connections/user")  :on-click #(do
                                                                (.preventDefault %)
                                                                (session/put! :visible-area :pending) )}
         [:div.info
          (when (pos? (count (:pending @state))) [:span.badge (count (:pending @state))])
          ;[:i.fa.fa-user.icon]
          [:div.text
           [:p.num (->> (:followers-users @state) count)]
           [:p.desc (t :social/Followersusers)]
           ]
          ]
         ]]])))

(defn handler [site-navi]
  (let [state (atom {:visible-area  nil
                     :followers-users  []
                     :following-users []})]
    (init-data state)
    (fn []
      (layout/default site-navi [content state]))))
