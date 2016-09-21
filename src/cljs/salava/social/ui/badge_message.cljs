(ns salava.social.ui.badge-message
  (:require [reagent.core :refer [atom cursor]]
            [reagent.session :as session]
            [reagent-modals.modals :as m]
            [clojure.string :refer [trim]]
            [reagent.session :as session]
            [salava.core.ui.ajax-utils :as ajax]
            [salava.core.ui.layout :as layout]
            [salava.user.ui.helper :refer [profile-picture]]
            [salava.core.ui.helper :refer [path-for current-path navigate-to input-valid?]]
            [salava.core.i18n :refer [t]]
            [salava.core.helper :refer [dump]]
            [salava.core.time :refer [date-from-unix-time]]
            
            ))

(defn init-data [state]
  (ajax/GET
   (path-for (str "/obpv1/social/messages/" (:badge_content_id @state)))
   {:handler (fn [data]
              (swap! state assoc :messages data
                                 :message ""))})
  )


(defn save-message [state]
  (let [{:keys [message user_id badge_content_id]} @state]
    (ajax/POST
     (path-for (str "/obpv1/social/messages/" badge_content_id))
     {:response-format :json
      :keywords? true
      :params {:message message
               :user_id user_id}
      :handler (fn [data]
                 (do
                   (init-data state)))
      :error-handler (fn [{:keys [status status-text]}]
                       )})))

(defn message-list-item [{:keys [message first_name last_name ctime id profile_picture]}]
  [:div {:class "media message-item" :key id}
   [:span {:class "pull-left"}
    [:img {:class "message-profile-img" :src (profile-picture profile_picture)}]]
   [:div {:class "media-body"}
    [:h4 {:class "media-heading"} (str first_name " "last_name " " (date-from-unix-time (* 1000 ctime) "minutes"))]
    [:span message]]
   ]
  )

(defn message-list [messages]
  [:div {:id ""}
   (doall
    (for [item messages]
      (message-list-item item)))])

(defn message-textarea [state]
  (let [message-atom (cursor state [:message])]
    [:div
     [:div {:class "form-group"}
      [:textarea {:class    "form-control"
                  :rows     "5"
                  :value    @message-atom
                  :onChange #(reset! message-atom (.-target.value %))} ]]
     [:div {:class "form-group"}
      [:button {:class    "btn btn-primary"
                :on-click #(do
                             (save-message state)
                             (.preventDefault %))} 
       "Post new"]]]))


(defn refresh-button [state]
  [:a {:href "#" 
       :class "pull-right" 
       :on-click #(do
                    (init-data state)
                    (.preventDefault %))} "Refresh"])

(defn content [state]
  (let [{:keys [messages]} @state]
    [:div
     [:h2 "Message board:"]
     (message-list messages)
     (refresh-button state)
     (message-textarea state)]))



(defn badge-message-handler [badge_content_id]
  (let [state (atom {:messages [] 
                     :user_id (session/get-in [:user :id])
                     :message ""
                     :badge_content_id badge_content_id})]
    (init-data state)
    (fn []
      (content state)
      )))
