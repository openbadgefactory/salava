(ns salava.social.ui.stream
  (:require [salava.core.ui.layout :as layout]
            [reagent.session :as session]
            [salava.core.i18n :as i18n :refer [t translate-text]]
            [salava.core.ui.ajax-utils :as ajax]
            [reagent.core :refer [atom cursor]]
            [salava.user.ui.helper :refer [profile-picture]]
            [reagent-modals.modals :as m]
            [salava.core.helper :refer [dump]]
            [salava.core.time :refer [date-from-unix-time]]
            [salava.user.ui.helper :refer [profile-picture]]
            [salava.gallery.ui.badges :as b]
            [salava.badge.ui.helper :as bh]
            [salava.social.ui.helper :refer [system-image]]
            [salava.core.ui.helper :as h :refer [unique-values navigate-to path-for]]))

(defn init-data [state]
  (ajax/GET
    (path-for "/obpv1/social/events" true)
    {:handler (fn [data]
                (swap! state assoc :events (:events data)
                       :pending-badges (:pending-badges data)
                       :tips (:tips data)))}))


(defn message-item [{:keys [message first_name last_name ctime id profile_picture user_id]}]
  [:div {:class "media" :key id}
   [:span {:class "pull-left"}
    [:img {:class "message-profile-img" :src (profile-picture profile_picture)}]]
   [:div {:class "media-body"}
    [:h4 {:class "media-heading"}
    [:a {:href (path-for (str "/user/profile/" user_id)) :target "_blank"}(str first_name " "last_name)]
    ;[:span (str (t :social/Commented) ":") ]
    ]
                                        ;(date-from-unix-time (* 1000 ctime) "minutes")
    (into [:div] (for [ item (clojure.string/split-lines message)]
                   [:p item]))]
   ]
  )

(defn update-status [id new-status state]
  (ajax/POST
     (path-for (str "/obpv1/badge/set_status/" id))
     {:response-format :json
      :keywords? true
      :params {:status new-status} 
      :handler (fn []
                 (init-data state)
                 )
      :error-handler (fn [{:keys [status status-text]}]
                       )}))

(defn badge-pending [{:keys [id image_file name description meta_badge meta_badge_req issuer_content_name issuer_content_url issued_on issued_by_obf verified_by_obf obf_url]} state]
  [:div.row {:key id}
   [:div.col-md-12
    [:div.badge-container-pending
     (if (or verified_by_obf issued_by_obf)
       (bh/issued-by-obf obf_url verified_by_obf issued_by_obf))
     [:div.row
      [:div.col-md-12
       [:div.media
        [:div.pull-left
         [:img.badge-image {:src (str "/" image_file)}]]
        [:div.media-body
         [:h4.media-heading
          name]
         [:div
          [:a {:href issuer_content_url :target "_blank"} issuer_content_name]]
         [:div (date-from-unix-time (* 1000 issued_on))]

         ;METABADGE
         [:div (bh/meta-badge meta_badge meta_badge_req)]

         [:div
          description]]]]]
     [:div {:class "row button-row"}
      [:div.col-md-12
       [:button {:class "btn btn-primary"
                 :on-click #(do
                              (update-status id "accepted" state)
                              (.preventDefault %))}
        (t :badge/Acceptbadge)]
       [:button {:class "btn btn-warning"
                 :on-click #(do
                              (update-status id "declined" state)
                              (.preventDefault %))}
        (t :badge/Declinebadge)]]]]]])

(defn badges-pending [state]
  (into [:div {:id "pending-badges"}]
        (for [badge (:pending-badges @state)]
          (badge-pending badge state))))


(defn hide-event [event_id state]
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
               :dangerouslySetInnerHTML {:__html "&times;"}}]])

(defn follow-event [event state]
  (let [{:keys [subject verb image_file message ctime event_id name object]}  event
        modal-message (str "messages")]
    [:div {:class "media message-item follow"}
    (hide-event event_id state)
     [:div.media-left
      [:a {:href "#"
           :on-click #(do
                        (b/open-modal object false init-data state)
                        (.preventDefault %) )}
       [:img {:src (str "/" image_file)} ]]]
     [:div.media-body
      [:div.date (date-from-unix-time (* 1000 ctime) "days") ]
      [:h3 {:class "media-heading"}
       [:a {:href "#"
           :on-click #(do
                        (b/open-modal object false init-data state)
                        (.preventDefault %) )} (str  name)]]
       (t :social/Youstartedfollowbadge)
      ]]))

(defn message-event [event state]
  (let [{:keys [subject verb image_file message ctime event_id name object]}  event
        new-messages  (get-in event [:message :new_messages])
        modal-message (if (pos? new-messages)
                              [:span (t :social/Newmessages) [:span.badge new-messages]]
                              (t :social/Readmore))
        ;(str  (t :social/Readmore) (if (pos? new-messages) (str " (" new-messages " " (if (= 1 new-messages) (t :social/Newmessage) (t :social/Newmessages)) ")")))
        ]
    [:div {:class (if (pos? new-messages) "media message-item new " "media message-item" )}
    (hide-event event_id state)
     [:div.media-left
      [:a {:href "#"
           :on-click #(do
                        (b/open-modal object true init-data state)
                        (.preventDefault %) )} 
       [:img {:src (str "/" image_file)} ]]]
     [:div.media-body
      [:div.date (date-from-unix-time (* 1000 ctime) "days") ]
      [:h3 {:class "media-heading"}
      (if (pos? new-messages) [:span.new  new-messages])
       [:a {:href "#"
           :on-click #(do
                        (b/open-modal object true init-data state)
                        (.preventDefault %) )} name]]
      (message-item message)
      [:a {:href     "#"
       :on-click #(do
                    (b/open-modal (:object event) true init-data state)
                    (.preventDefault %) )}
       modal-message]]]))


(defn edit-profile-event [{:keys [header body button link]} state]
  (let [site-name (session/get :site-name)
        ]
    [:div {:class "media message-item tips"}
     [:div.media-left
      (system-image)]
     [:div.media-body
      ;[:div.date (date-from-unix-time (* 1000 ctime) "days") ]
      [:h3 {:class "media-heading"}
       [:a {:href (path-for "/user/edit/profile")} (t :social/Profiletipheader)]]
      [:div.media-body
       (t :badge/Add) " "  (t :badge/Profilepicture)
       "."  ]
      [:a {:href (path-for "/user/edit/profile")} (t :social/Profiletipbutton)]
      ]]))

(defn tip-event [{:keys [header body button link]} state]
  (let [site-name (session/get :site-name)
        ]
    [:div {:class "media message-item tips"}
     [:div.media-left
      (system-image)]
     [:div.media-body
      ;[:div.date (date-from-unix-time (* 1000 ctime) "days") ]
      [:h3 {:class "media-heading"}
       [:a {:href (if link (path-for link) "#")} (translate-text header)]]
      [:div.media-body
       (translate-text body)"."]
      (if button
        [:a {:href (if link (path-for link) "#")} (translate-text button) ])
      ]]))

(defn get-first-badge-event [state]
  (let [site-name (session/get :site-name)]
    [:div {:class "media message-item tips"}
     [:div.media-left
      (system-image)]
     [:div.media-body
      ;[:div.date (date-from-unix-time (* 1000 ctime) "days") ]
      [:h3 {:class "media-heading"}
       [:a {:href (path-for "/gallery/application")}  (str (t :core/Welcometo) " " site-name (t :core/Service))]]
      [:div.media-body
       (str (t :social/Youdonthaveanyanybadgesyet) ".")]
      [:a {:href (path-for "/gallery/application")}
       (t :social/Getyourfirstbadge)]]]))

(defn profile-picture-tip []
  {:header (t :social/Profilepictureheader)  
   :body  (t :social/Profilepicturebody)
   :button (t :social/Profiletipbutton)
   :link "/user/edit/profile"} )

(defn profile-description-tip []
  {:header (t :social/Profiledescriptiontipheader)  
   :body  (t :social/Profiledescriptionbody)
   :button (t :social/Profiletipbutton)
   :link "/user/edit/profile"} )


(defn get-your-first-badge-tip []
  (let [site-name (session/get :site-name)]
    {:header (str (t :core/Welcometo) " " site-name (t :core/Service))
     :body  (t :social/Youdonthaveanyanybadgesyet)
     :button (t :social/Getyourfirstbadge)
     :link   "/gallery/application"}))

(defn not-verified-email [email]
  {:header (t :social/Confirmyouremailheader)
   :body  (str (t :user/Confirmyouremailbody1) " " email (t :user/Confirmyouremailbody2) )
   :button (t :social/Readmore)
   :link   "/user/edit/email-addresses"})

(defn tips-container [tips state]
  [:div.row
   (if (:welcome-tip tips)
     (tip-event (get-your-first-badge-tip) state))
   (if (:profile-picture-tip tips)
     (tip-event (profile-picture-tip) state))
   (into [:div ]
         (for [email (:not-verified-emails tips)]
           (tip-event (not-verified-email (:email email)) state) 
           ))])

(defn content [state]
  (let [events (:events @state)
        tips (:tips @state)]
    [:div {:class "my-badges pages"}
     [m/modal-window]
     [badges-pending state]
     (tips-container tips state)
     (into [:div {:class "row"}]
           (for [event events]
             (cond
               (= "follow" (:verb event)) (follow-event event state)
               (= "message" (:verb event)) (message-event event state)
               :else "")))]))



(defn handler [site-navi]
  (let [state (atom {:events []})]
    (init-data state)
    (fn []
      (layout/default site-navi (content state)))))
