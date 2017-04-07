(ns salava.extra.socialuser.ui.connections
  (:require [reagent.core :refer [atom cursor]]
            [salava.core.ui.ajax-utils :as ajax]
            [salava.core.helper :refer [dump]]
            [salava.core.ui.helper :refer [path-for]]
            [salava.user.ui.helper :refer [profile-picture]]
            [salava.core.i18n :refer [t]]))


(defn init-data [state]
  (ajax/GET
   (path-for (str "/obpv1/socialuser/connections" ))
   {:handler (fn [data]
               (swap! state assoc :pending-users (:pending-users data)
                      :accepted-users (:accepted-users data)))}))

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
                                                 )})} "unfollow"])


(defn accepted-user-connections [state users visible-area-atom]
  (let [panel-identity :accepted]
    [:div.panel
     [:div.panel-heading
      [:h3
       [:a {:href "#" :on-click #(do (.preventDefault %) (reset! visible-area-atom panel-identity))} (str (t :social/followedusers) " (" (count users) ")")]]]
     (if (= panel-identity @visible-area-atom)
         [:div.panel-body
          [:table {:class "table" :summary (t :badge/Badgeviews)}
           [:thead
            [:tr
             [:th ""]
             [:th (t :badge/Name)]
             [:th ""]]]
           (into [:tbody]
                 (for [user users
                       :let [{:keys [user_id profile_picture first_name last_name]} user]]
                   [:tr
                    [:td [:img.badge-icon {:src (profile-picture profile_picture) 
                                           :alt name}]]
                    [:td.name [:a {:href     (path-for (str "/user/profile/" user_id))
                                   :on-click #(do
                                        ;(b/open-modal id false init-data state)
                                                (.preventDefault %)) } (str first_name " " last_name)]]
                    [:td  (deleteconnect user_id state)]]))]])]))


(defn accept [owner-id state]
  [:a {:href     "#"
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
  [:a {:href     "#"
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
     [:div.panel-body
      [:table {:class "table" :summary (t :badge/Badgeviews)}
       [:thead
        [:tr
         [:th (t :badge/Name)]
         [:th (t :badge/Name)]
         [:th ""]]]
       (into [:tbody]
             (for [user users
                   :let [{:keys [owner_id profile_picture first_name last_name]} user]]      
               [:tr
                [:td [:img.badge-icon {:src (profile-picture profile_picture) 
                                       :alt name}]]
                [:td.name [:a {:href     "#"
                               :on-click #(do
                                        ;(b/open-modal id false init-data state)
                                            (.preventDefault %)) } (str first_name " " last_name)]]
                [:td (accept owner_id state) " " (decline owner_id state)]]))]]]))




(defn content [state]
  (let [visible-area-atom (cursor state [:visible-area])
        pending-users     (cursor state [:pending-users])
        accepted-users    (cursor state [:accepted-users])]
    [:div     
     (if-not (empty? @pending-users)
       [:div {:id "badge-stats"}
      (pending-user-connections state @pending-users visible-area-atom)])
     (if-not (empty? @accepted-users)
       [:div {:id "badge-stats"}
        (accepted-user-connections state @accepted-users visible-area-atom)])]))

(defn handler []
  (let [state (atom {:visible-area   :accepted
                     :pending-users  []
                     :accepted-users []})]
    (init-data state)
    (fn []
      (content state))))
