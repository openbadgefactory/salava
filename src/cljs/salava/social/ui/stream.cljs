(ns salava.social.ui.stream
  (:require [salava.core.ui.layout :as layout]
            [reagent.session :as session]
            [salava.core.i18n :as i18n :refer [t translate-text]]
            [salava.core.ui.ajax-utils :as ajax]
            [reagent.core :refer [atom cursor create-class]]
            [salava.core.ui.modal :as mo]
            [salava.user.ui.helper :refer [profile-picture]]
            [reagent-modals.modals :as m]
            [salava.core.helper :refer [dump]]
            [salava.core.time :refer [date-from-unix-time]]
            [salava.user.ui.helper :refer [profile-picture]]
            [salava.gallery.ui.badges :as b]
            [salava.badge.ui.helper :as bh]
            [salava.extra.application.ui.helper :refer [application-plugin?]]
            [salava.social.ui.helper :refer [system-image]]
            [salava.core.ui.helper :as h :refer [unique-values navigate-to path-for plugin-fun]]))




(defn init-data [state]
  (ajax/GET
    (path-for "/obpv1/social/events" true)
    {:handler (fn [data]
                (swap! state assoc :events (:new-events data)
                       :initial false
                       :pending-badges (:pending-badges data)
                       :tips (:tips data))
                (if (:admin-events data)
                  (swap! state assoc :admin-events (:admin-events data))))}))


(defn pending-connections [state]
  (let [connections (first (plugin-fun (session/get :plugins) "block" "pendingconnections"))]
    (if connections
      [connections {:state state
                    :init-data init-data}]
      [:div ""])))

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

(defn follow-event-badge [event state]
  (let [{:keys [subject verb image_file message ctime event_id name object]}  event
        modal-message (str "messages")]
    [:div {:class "media message-item tips"}
    (hide-event event_id state)
     [:div.media-left
      [:a {:href "#"
           :on-click #(do
                        (b/open-modal object false init-data state)
                        (.preventDefault %) )}
       [:img {:src (str "/" image_file)} ]]]
     [:div.media-body
      [:div.date (date-from-unix-time (* 1000 ctime) "days") ]
      [:i {:class "fa fa-lightbulb-o"}]
      [:div [:h3 {:class "media-heading"}
       [:a {:href "#"
           :on-click #(do
                        (b/open-modal object false init-data state)
                        (.preventDefault %) )} (str  name)]]
      [:div.media-body
       (t :social/Youstartedfollowbadge)]]
      ]]))

(defn publish-event-badge [event state]
  (let [{:keys [subject verb image_file message ctime event_id name object first_name last_name]}  event
        modal-message (str "messages")]
    [:div {:class "media message-item tips"}
    (hide-event event_id state)
     [:div.media-left
      [:a {:href "#"
           :on-click #(do
                        (mo/open-modal [:badge :info] {:badge-id object})
                        
                        (.preventDefault %) )}
       [:img {:src (str "/" image_file)} ]]]
     [:div.media-body
      [:div.date (date-from-unix-time (* 1000 ctime) "days") ]
      [:i {:class "fa fa-lightbulb-o"}]
      [:div [:h3 {:class "media-heading"}
       [:a {:href "#"
            :on-click #(do
                         (mo/open-modal [:badge :info] {:badge-id object})
                        (.preventDefault %) )} (str first_name " " last_name " " verb " "  name)]]
      [:div.media-body
       "Käyttäjä julkaisi merkin! eikö ole hienoa?"]]
      ]])
  )


(defn follow-event-user [event state]
  (let [{:keys [subject verb ctime event_id object s_first_name s_last_name o_first_name o_last_name o_id s_id owner o_profile_picture s_profile_picture]}  event
        modal-message (str "messages")]
    [:div {:class "media message-item tips"}
    (hide-event event_id state)
     [:div.media-left
      [:a {:href "#"
           :on-click #(do
                        (mo/open-modal [:user :profile] {:user-id (if (= owner s_id)
                                                                                              o_id
                                                                                              s_id)})                        
                        ;(b/open-modal object false init-data state)
                        (.preventDefault %) )}
       [:img {:src (profile-picture (if (= owner s_id)
                                      o_profile_picture
                                      s_profile_picture)) } ]]]
     [:div.media-body
      [:div.date (date-from-unix-time (* 1000 ctime) "days") ]
      [:i {:class "fa fa-lightbulb-o"}]
      [:div [:h3 {:class "media-heading"}
       [:a {:href "#"
           :on-click #(do
                        (mo/open-modal [:user :profile] {:user-id (if (= owner s_id)
                                                                                              o_id
                                                                                              s_id)})
                        (.preventDefault %) )} (if (= owner s_id) (str  "You started  " verb " " o_first_name " " o_last_name)
                                                   (str  s_first_name " " s_last_name " " verb " you"  ))]]
       [:div.media-body
        (if (= owner s_id) "aloitit henkilön seuraamisen"
            "Henkilö aloitti sinun seuraamisen")
       ]]
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
      [:i {:class "fa fa-lightbulb-o"}]
        [:div
      [:h3 {:class "media-heading"}
       [:a {:href (path-for "/user/edit/profile")} (t :social/Profiletipheader)]]
      [:div.media-body
       (t :badge/Add) " "  (t :badge/Profilepicture)
       "."  ]]
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
      [:i {:class "fa fa-lightbulb-o"}]
      [:div [:h3 {:class "media-heading"}
       [:a {:href (if link (path-for link) "#")} (translate-text header)]]
      [:div.media-body
       (translate-text body)]
      (if button
        [:a {:href (if link (path-for link) "#")} (translate-text button) ])
      ]]]))

(defn profile-picture-tip []
  {:header (t :social/Profilepictureheader)  
   :body  (str (t :social/Profilepicturebody) ".")
   :button (t :social/Profiletipbutton)
   :link "/user/edit/profile"} )

(defn profile-description-tip []
  {:header (t :social/Profiledescriptiontipheader)  
   :body  (str (t :social/Profiledescriptionbody) ".")
   :button (t :social/Profiletipbutton)
   :link "/user/edit/profile"} )


(defn get-your-first-badge-tip []
  (let [site-name (session/get :site-name)]
    {:header (str (t :core/Welcometo) " " site-name (t :core/Service))
     :body (str (t :social/Youdonthaveanyanybadgesyet) ".")
     :button (if (application-plugin?) (t :social/Getyourfirstbadge) nil)
     :link  (if (application-plugin?) "/gallery/application" nil) }))


(defn report-ticket-tip [events]
  (let [count (count events)]
    {:header (t :social/Emailadmintickets)
     :body  (str (t :social/Openissues) ": " count) 
     :button (t :social/Clickhere)
     :link   "/admin/tickets"}))

(defn not-verified-email [email]
  {:header (t :social/Confirmyouremailheader)
   :body  (str (t :user/Confirmyouremailbody1) " " email ". "(t :user/Confirmyouremailbody2) "." )
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

(defn empty-stream []
  [:div {:class "media message-item tips"}
    [:div.media-left
      (system-image)]
    [:div.media-body
        [:i {:class "fa fa-lightbulb-o"}]
        [:div
        [:h3.media-heading (str (t :social/Emptystreamheader) " " (t :social/Sometips))]
        [:div.media-body
        [:ul
         [:li (t :social/Pagetip) " "  [:a {:href (path-for "/page") } (t :page/Mypages)]]
         [:li (t :social/Badgetip) " " [:a {:href (path-for "/badge") } (t :badge/Mybadges) ]]
         [:li (t :social/Profiletip) " " [:a {:href (path-for "/gallery/profiles") }(t :gallery/Sharedprofiles)  ]]]]]]])


;[usermodel/handler 11]

(defn content [state]
  (let [events (:events @state)
        tips (:tips @state)
        initial (:initial @state)
        admin-events (or (:admin-events @state) nil)]
    [:div {:class "my-badges pages"}
     [m/modal-window]
     (pending-connections state)
     [badges-pending state]
     (if admin-events
       [:div.row
        (tip-event (report-ticket-tip admin-events) state)]
       )
     (tips-container tips state)
     (if (and (empty? events) (not initial) (not (:profile-picture-tip tips)) (not (:welcome-tip tips)) (empty? (:not-verified-emails tips)))(empty-stream))
     (into [:div {:class "row"}]
           (for [event events]
             (cond
               (and (= "badge" (:type event)) (= "follow" (:verb event))) (follow-event-badge event state)
               (and (= "user" (:type event)) (= "follow" (:verb event))) (follow-event-user event state)
               (and (= "badge" (:type event)) (= "publish" (:verb event))) (publish-event-badge event state) 
               (= "message" (:verb event)) (message-event event state)
               :else "")))]))



(defn handler [site-navi]
  (let [state (atom {:initial true
                     :events []
                     :tips {:profile-picture-tip false
                            :welcome-tip false
                            :not-verified-emails []}})]

    (init-data state)
    (fn []
      (layout/default site-navi (content state)))))




