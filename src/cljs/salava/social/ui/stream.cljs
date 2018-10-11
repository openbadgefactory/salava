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
            ;[salava.extra.application.ui.helper :refer [application-plugin?]]
            [salava.social.ui.helper :refer [system-image]]
            [salava.core.ui.notactivated :refer [not-activated-banner]]
            [salava.badge.ui.pending :refer [pending-badge-content]]
            [salava.core.ui.helper :refer [path-for plugin-fun not-activated?]]
            [salava.badge.ui.modal :as bm]))


(defn init-data [state]
  (ajax/GET
    (path-for "/obpv1/social/events" true)
    {:handler (fn [data]
                (swap! state assoc :events (:events data)
                                   :initial false
                                   :tips (:tips data)))})
  (ajax/GET
    (path-for "/obpv1/social/pending_badges" true)
    {:handler (fn [data]
                (swap! state assoc :spinner false :pending-badges (:pending-badges data)))}))

(defn pending-connections [reload-fn]
  (let [connections (first (plugin-fun (session/get :plugins) "block" "pendingconnections"))]
    (if connections
      [connections reload-fn]
      [:div ""])))

(defn message-item [{:keys [message first_name last_name ctime id profile_picture user_id]}]
  [:div {:class "media" :key id}
   [:span {:class "pull-left"}
    [:img {:class "message-profile-img" :src (profile-picture profile_picture)}]]
   [:div {:class "media-body"}
    [:h4 {:class "media-heading"}
     [:a {:href "#" :on-click #(mo/open-modal [:user :profile] {:user-id user_id})} (str first_name " "last_name)]]

    (into [:div] (for [ item (clojure.string/split-lines message)]
                   [:p item]))]])

(defn update-status [id new-status state]
  (ajax/POST
     (path-for (str "/obpv1/badge/set_status/" id))
     {:response-format :json
      :keywords? true
      :params {:status new-status}
      :handler (fn []
                 (init-data state) )
      :error-handler (fn [{:keys [status status-text]}])}))

#_(defn badge-pending [{:keys [id image_file name description meta_badge meta_badge_req issuer_content_name issuer_content_url issued_on issued_by_obf verified_by_obf obf_url]} state]
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

(defn badge-alert [state]
  (if (:badge-alert @state)
    [:div {:class "alert alert-success"}
     (case (:badge-alert @state)
       "accepted"  [:div (str (t :badge/Youhaveaccepted) " \"" (:badge-name @state) "\". ") (t :badge/Youcanfind)]
       "declined" (t :badge/Badgedeclined)
       "")]))

(defn badge-pending [badge state]
  [:div.row {:key (:id badge)}
   [:div.col-md-12
    [:div.badge-container-pending
     [pending-badge-content badge]
     [:div {:class "row button-row"}
      [:div.col-md-12
       [:button {:class "btn btn-primary"
                 :on-click #(do
                              (update-status (:id badge) "accepted" state)
                              (.preventDefault %)
                              (swap! state assoc :badge-alert "accepted" :badge-name (:name badge)))}
        (t :badge/Acceptbadge)]
       [:button {:class "btn btn-warning"
                 :on-click #(do
                              (update-status (:id badge) "declined" state)
                              (.preventDefault %)
                              (swap! state assoc :badge-alert "declined" :badge-name (:name badge)))}
        (t :badge/Declinebadge)]]]]]])

(defn badges-pending [state]
  (if (:spinner @state)
    [:div.ajax-message
     [:i {:class "fa fa-cog fa-spin fa-2x "}]
     [:span (str (t :core/Loading) "...")]
     [:hr]]
    (into [:div {:id "pending-badges"}]
          (for [badge (:pending-badges @state)]
            (badge-pending badge state)))))


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
                        (mo/open-modal [:gallery :badges] {:badge-id object})
                        (.preventDefault %) )}
       [:img {:src (str "/" image_file)} ]]]
     [:div.media-body
      [:div.date (date-from-unix-time (* 1000 ctime) "days") ]
      [:i {:class "fa fa-lightbulb-o"}]
      [:div [:h3 {:class "media-heading"}
       [:a {:href "#"
            :on-click #(do
                         (mo/open-modal [:gallery :badges] {:badge-id object})
                        (.preventDefault %) )} (str  name)]]
      [:div.media-body
       (t :social/Youstartedfollowbadge)]]
      ]]))


(defn badge-advert-event [event state]
  (let [modal (first (plugin-fun (session/get :plugins) "application" "open_modal"))
         {:keys [subject verb image_file ctime event_id name object issuer_content_id issuer_content_name]} event]
    [:div#advert-event {:class "media message-item tips" :style {:margin-bottom "10px" :padding-top "5px"}}
     (hide-event event_id state)
     [:div.media-left
      [:a {:href "#"
           :on-click ""}]
      [:img {:style {:padding "4px"} :src (str "/" image_file)} ]]
     [:div.media-body
      [:div.date (date-from-unix-time (* 1000 ctime) "days") ]
      [:i {:class "fa fa-bell"}]
      [:div [:h3 {:class "media-heading" :style {:padding-bottom "5px"}}
             (t :social/Badgeadvertisement)]
       [:div.media-body
        [:div name]
        (bm/issuer-modal-link issuer_content_id issuer_content_name)
        [:a {:href "#"
             :on-click #(do
                          (.preventDefault %)
                          (modal subject state)
                          )} [:div.get-badge-link [:i {:class "fa fa-angle-double-right"}] (str " " (t :extra-application/Getthisbadge))]]]]
      ]]))

(defn publish-event-badge [event state]
  (let [{:keys [subject verb image_file message ctime event_id name object first_name last_name]}  event]
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
                         (.preventDefault %) )} (str (t :social/User) " " first_name " " last_name " " (t :social/Publishedbadge) " " name)]]
      [:div.media-body
       (t :social/Publishedbadgetext)]]
      ]])
  )

(defn publish-event-page [event state]
  (let [{:keys [subject verb profile_picture message ctime event_id name object first_name last_name]}  event]
    [:div {:class "media message-item tips"}
    (hide-event event_id state)
     [:div.media-left
      [:a {:href "#"
           :on-click #(do
                        (mo/open-modal [:page :view] {:page-id object})

                        (.preventDefault %) )}
       [:img {:src (profile-picture profile_picture) } ]]]
     [:div.media-body
      [:div.date (date-from-unix-time (* 1000 ctime) "days") ]
      [:i {:class "fa fa-lightbulb-o"}]
      [:div [:h3 {:class "media-heading"}
       [:a {:href "#"
            :on-click #(do
                         (mo/open-modal [:page :view] {:page-id object})
                         (.preventDefault %) )} (str (t :social/User) " " first_name " " last_name " " (t :social/Publishedpage) " " name)]]
       [:div.media-body
        (t :social/Publishedpagetext)]]
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
                        (.preventDefault %) )} (if (= owner s_id)
                                                 (str (t :social/Youstartedfollowing) " " o_first_name " " o_last_name)
                                                 (str  s_first_name " " s_last_name " " (t :social/Followsyou)  ))]]
       [:div.media-body
        (if (= owner s_id)(t :social/Youstartedfollowingtext)
           (t :social/Followsyoutext) )
       ]]
      ]]))

(defn message-event [event state]
  (let [{:keys [subject verb image_file message ctime event_id name object]}  event
        new-messages  (get-in event [:message :new_messages])
        modal-message (if (pos? new-messages)
                              [:span (t :social/Newmessages) [:span.badge new-messages]]
                              (t :social/Readmore))
        reload-fn (fn [] (init-data state))
        ;(str  (t :social/Readmore) (if (pos? new-messages) (str " (" new-messages " " (if (= 1 new-messages) (t :social/Newmessage) (t :social/Newmessages)) ")")))
        ]
    [:div {:class (if (pos? new-messages) "media message-item new " "media message-item" )}
    (hide-event event_id state)
     [:div.media-left
      [:a {:href "#"
           :on-click #(do
                                        ;(b/open-modal object true init-data state)
                        (init-data state)
                        (mo/open-modal [:gallery :badges] {:badge-id object
                                                           :show-messages true
                                                           :reload-fn reload-fn})
                        (.preventDefault %) )}
       [:img {:src (str "/" image_file)} ]]]
     [:div.media-body
      [:div.date (date-from-unix-time (* 1000 ctime) "days") ]
      [:h3 {:class "media-heading"}
      (if (pos? new-messages) [:span.new  new-messages])
       [:a {:href "#"
            :on-click #(do
                         (mo/open-modal [:gallery :badges] {:badge-id object
                                                            :show-messages true
                                                            :reload-fn reload-fn})
                        ;(b/open-modal object true init-data state)
                        (.preventDefault %) )} name]]
      (message-item message)
      [:a {:href     "#"
           :on-click #(do
                        (init-data state)
                        (mo/open-modal [:gallery :badges] {:badge-id object
                                                           :show-messages true
                                                           :reload-fn reload-fn})
                        ;(b/open-modal (:object event) true init-data state)
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
       ;(translate-text body)
       body]
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
     :button nil ;(if (application-plugin?) (t :social/Getyourfirstbadge) nil)
     :link  nil;(if (application-plugin?) "/gallery/application" nil)
     }))


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

(defn not-activated-account []
  {:header (t :social/Notactivatedaccount)
   :body  [:div
           [:p (t :social/Notactivatedbody1)]
           [:ul
             [:li (t :social/Notactivatedbody2)]
             [:li (t :social/Notactivatedbody3)]
             [:li (t :social/Notactivatedbody4)]
             [:li (t :social/Notactivatedbody5)]
             [:li (t :social/Notactivatedbody6)]]]
   :button (t :social/Readmore)
   :link   "/user/edit/email-addresses"})

(defn tips-container [tips state]
  [:div.row
   (cond
       (not-activated?) (tip-event (not-activated-account) state)
       (:welcome-tip tips) (tip-event (get-your-first-badge-tip) state)
       (:profile-picture-tip tips) (tip-event (profile-picture-tip) state)
       :else [:div])
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
        admin-events (or (:admin-events @state) nil)
        reload-fn (fn [] (init-data state))]
    [:div {:class "my-badges pages"}

     [m/modal-window]
     [badge-alert state]
     [pending-connections reload-fn]
     [badges-pending state]
     (if (not-activated?)
       (not-activated-banner))
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
               (and (= "page" (:type event)) (= "publish" (:verb event))) (publish-event-page event state)
               (and (= "advert" (:type event)) (= "advertise" (:verb event))) (badge-advert-event event state)
               (= "message" (:verb event)) [message-event event state]
               :else "")))]))



(defn handler [site-navi]
  (let [state (atom {:initial true
                     :spinner true
                     :events []
                     :pending-badges []
                     :tips {:profile-picture-tip false
                            :welcome-tip false
                            :not-verified-emails []}
                     :badge-alert nil})]

    (init-data state)
    (fn []
      (layout/default site-navi [content state]))))




