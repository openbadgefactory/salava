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

(defn init-data [state]
  (ajax/GET
    (path-for "/obpv1/social/events" true)
    {:handler (fn [data]
                (swap! state assoc :events data))}))


(defn message-item [{:keys [message first_name last_name ctime id profile_picture user_id]}]
  [:div {:class "media" :key id}
   
   [:div {:class "media-body"}
    [:h4 {:class "media-heading"}
    [:a {:href (str "/user/profile/" user_id) }(str first_name " "last_name)]
    [:span (str (t :social/Commented) ":") ]
    ]
;(date-from-unix-time (* 1000 ctime) "minutes")

    [:span message]]
   ]
  )



(defn stream-item [event object state]
  (let [{:keys [subject verb image_file message ctime event_id name]}  event
          new_messages  (get-in event [:message :new_messages])]
    [:div {:class (if (pos? new_messages) "media message-item new " "media message-item" )}
    [:button {:type       "button"
                :class      "close"
                :aria-label "OK"
                :on-click   #(do
                               (ajax/POST
                                (path-for (str "/obpv1/social/hide_event/" event_id))
                                {:response-format :json
                                 :keywords?       true          
                                 :handler         (fn [data]
                                                    (do
                                                      (init-data state)))
                                 :error-handler   (fn [{:keys [status status-text]}]
                                                    )})
                               (.preventDefault %))
                }
       [:span {:aria-hidden "true"
               :dangerouslySetInnerHTML {:__html "&times;"}}]]
     [:div.media-left
      [:a {:href "#"}
       [:img {:src (str "/" image_file)} ]]]
     [:div.media-body
      [:div.date (date-from-unix-time (* 1000 ctime) "days") ]
      [:h3 {:class "media-heading"}
      (if (pos? new_messages)[:span.new  (str new_messages " new")])
       [:a {:href "#"} name]]
      (message-item message)
      object
      ]]))

(defn content [state]
  (let [events (:events @state)]
    [:div {:class "my-badges pages"}
     [m/modal-window]
     (into [:div {:class "row"}]
           (for [event events]
             (do
               (stream-item event  [badge-message-stream-link (get-in event [:message :new_messages]) (:object event) init-data state] state))))]))


(defn handler [site-navi]
  (let [state (atom {:events []})]
    (init-data state)
    (fn []
      (layout/default site-navi (content state)))))
