(ns salava.social.ui.stream
   (:require [salava.core.ui.layout :as layout]
             [salava.core.i18n :as i18n :refer [t]]
             [salava.core.ui.ajax-utils :as ajax]
             [reagent.core :refer [atom cursor]]
             [salava.social.ui.badge-message-modal :refer [badge-message-stream-link]]
             [reagent-modals.modals :as m]
             [salava.core.helper :refer [dump]]
             [salava.core.time :refer [date-from-unix-time]]
             [salava.user.ui.helper :refer [profile-picture]]
             [salava.core.ui.helper :as h :refer [unique-values navigate-to path-for]]))


(defn message-item [{:keys [message first_name last_name ctime id profile_picture user_id]}]
  [:div {:class "media" :key id}
   
   [:div {:class "media-body"}
    [:h4 {:class "media-heading"} (str first_name " "last_name " (" (date-from-unix-time (* 1000 ctime) "minutes") "):")]
    [:span message]]
   ]
  )

(defn stream-item [subject verb object image_file message]
  [:div {:class "media message-item"}
   [:div.media-left
    [:a {:href "#"}
     [:img {:src  (str "/" image_file)} ]]]
   [:div.media-body
    [:h3 {:class "media-heading"}
     [:a {:href "#"} subject]]
    [:button {:type       "button"
                 :class      "close"
                 :aria-label "OK"
                 :on-click   #(do
                                (.preventDefault %))
                 }
        [:span {:aria-hidden "true"
                
                :dangerouslySetInnerHTML {:__html "&times;"}}]]
    (message-item message)
    object
    ]])

(defn content [state]
  (let [events (:events @state)]
    [:div {:class "my-badges pages"}
     [m/modal-window]
     (into [:div {:class "row"}]
           (for [event events]
             (do
               (stream-item (:name event) (:verb event) [badge-message-stream-link {:new-messages (:count event)}  (:object event)]  (:image_file event) (:message event)))))]))

(defn init-data [state]
  (ajax/GET
    (path-for "/obpv1/social/events" true)
    {:handler (fn [data]
                (swap! state assoc :events data))}))

(defn handler [site-navi]
  (let [state (atom {:events []})]
    (init-data state)
    (fn []
      (layout/default site-navi (content state)))))
