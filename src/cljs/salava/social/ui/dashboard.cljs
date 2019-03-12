(ns salava.social.ui.dashboard
  (:require [salava.core.ui.layout :as layout]
            [salava.core.ui.ajax-utils :as ajax]
            [salava.social.ui.stream :as stream]
            [reagent.core :refer [atom]]
            [salava.core.ui.modal :as mo]
            [salava.core.i18n :as i18n :refer [t translate-text]]
            [salava.core.time :refer [date-from-unix-time]]
            [reagent-modals.modals :as m]
            [salava.core.ui.helper :refer [path-for]]
            [salava.badge.ui.my :as my]))

(defn init-pending-badges [state]
  (ajax/GET
    (path-for "/obpv1/social/pending_badges" true)
    {:handler (fn [data]
                (swap! state assoc :spinner false :pending-badges (:pending-badges data)))}))

(defn init-badge-stats [state]
  (ajax/GET
    (path-for "/obpv1/badge/stats" true)
    {:handler (fn [data]
                (swap! state assoc :stats data)
                )}))


(defn init-data [state]
  (stream/init-data state)
  ;(init-pending-badges state)
  (my/init-data state)
  (init-badge-stats state))

(defn follow-event-badge [event state]
  (let [{:keys [subject verb image_file message ctime event_id name object]}  event
        modal-message (str "messages")]
    [:div {:class "media"}
     [:a {:href "#"
          :on-click #(do
                       (mo/open-modal [:gallery :badges] {:badge-id object})
                       (.preventDefault %))
          :style {:text-decoration "none"}}
      [:div.media-left
       [:img {:src (str "/" image_file)} ]]
      [:div.media-body
       [:div.content-text
        [:p.content-heading
         (t :social/Youarefollowingthisbadge)]
        [:span.date (date-from-unix-time (* 1000 ctime) "days")]
        ]]]]))

(defn message-event [event state]
  (let [{:keys [subject verb image_file message ctime event_id name object]}  event
        reload-fn (fn [] (init-data state))]
    [:div {:class "media"}
     [:a {:href "#"
          :on-click #(do
                       ;(b/open-modal object true init-data state)
                       (init-data state)
                       (mo/open-modal [:gallery :badges] {:badge-id object
                                                          :show-messages true
                                                          :reload-fn reload-fn})
                       (.preventDefault %) )
          :style {:text-decoration "none"}}
      [:div.media-left
       [:img {:src (str "/" image_file)} ]]
      [:div.media-body
       [:div.content-text
        [:p.content-heading (:message message)]
        [:span.date (date-from-unix-time (* 1000 ctime) "days") ]]]]]))


(defn welcome-block [state]
  [:div#welcome-block {:class "block"}
   [:div.welcome-block.block-content.row
    [:div.content "Welcome back to open badge passport"]
    ]])

#_(defn notifications-block [state]
    [:div#notifications-block {:class "block"}
     [:div.notifications-block.block-content
      [:div.heading_1 [:i.fa.fa-rss.icon]
       [:span.title "notifications"]]
      ]])

(defn notifications-block [state]
  (let [events (:events @state)]
    [:div#notifications-block {:class "block col-md-3" }
     ;(prn @state)
     [:div.notifications-block.block-content
      [:div.heading_1 [:i.fa.fa-rss.icon]
       [:a {:href "social/stream"} [:span.title "notifications"]] [:span.badge (count events)]]
      (reduce (fn [r event]
                (conj r [:div.notification-div.ax_default
                         (cond
                           (and (= "badge" (:type event)) (= "follow" (:verb event))) (follow-event-badge event state)
                           (and (= "user" (:type event)) (= "follow" (:verb event))) (stream/follow-event-user event state)
                           (and (= "badge" (:type event)) (= "publish" (:verb event))) (stream/publish-event-badge event state)
                           (and (= "page" (:type event)) (= "publish" (:verb event))) (stream/publish-event-page event state)
                           (= "advert" (:type event)) (stream/badge-advert-event event state)
                           (= "message" (:verb event)) [message-event event state]
                           :else "")
                         ])
                )[:div.content] (->> (take 5 events) (remove #(= (:verb %) "ticket"))))
      ]]))

(defn badges-block [state]
  [:div#badge-block {:class "block col-md-6"}
   [:div.badge-block.block-content
    [:div.heading_1
     [:i.fa.fa-certificate.icon]
      [:a {:href (path-for "/badge")} [:span.title "Badges"]]]
     [:div.content
      [:div.stats
         [:div.total-badges
          [:p.num (get-in @state [:stats :badge_count] 0)]
          [:p.desc (t :badge/Badges)]]
         [:div
          [:p.num (->> (get-in @state [:stats :badge_views])
                      (reduce #(+ %1 (:reg_count %2) (:anon_count %2)) 0))]
          [:p.desc (t :badge/Badgeviews)]]]

      [:div.pending
       [:p.header (t :badge/Pendingbadges)]
       (if (seq (:pending-badges @state))
         (reduce (fn [r badge]
                   (conj r [:a {:href (path-for "/badge")} [:img {:src (str "/" (:image_file badge)) :alt (:name badge) :title (:name badge)}]])
                   ) [:div] (take 5 (:pending-badges @state)))
         )]
      [:div.badges
       [:p.header (t :badge/Lastestearnedbadges)]
       (reduce (fn [r badge]
                   (conj r [:a {:href "#" :on-click #(do
                                                       (.preventDefault %)
                                                       (mo/open-modal [:badge :info] {:badge-id (:id badge )}))} [:img {:src (str "/" (:image_file badge)) :alt (:name badge) :title (:name badge)}]])
                   ) [:div] (take 5 (:badges @state)))
       ]
      [:div.badges
       [:p.header (t :badge/Latestearnablebadges)]
       ;[earnable-badges state]
       ]
      ]
     ]])

(defn explore-block [state]
  [:div#explore-block {:class "block col-sm-3"}
   [:div.explore-block.block-content
    [:div.heading_1
     [:i.fa.fa-search.icon]
     [:span.title "Explore"]]
    ]])

(defn connections-block [state]
  [:div#connections-block {:class "block col-sm-6"}
   [:div.connections-block.block-content
    [:div.heading_1 [:i.fa.fa-group.icon]
     [:span.title
      "Connections"]]
    ]])

(defn profile-block [state]
  [:div#profile-block {:class "block col-sm-3"}
   [:div.profile-block.block-content
    [:div.heading_1
     [:i.fa.fa-user.icon]
     [:span.title
      "Profile"]]]])

(defn help-block [state]
  [:div#getting-started {:class "block col-md-3"}
   [:div.getting-started.block-content
    [:div.heading_1
     [:i.fa.fa-info-circle.icon]
     [:span.title.help
      "Help"]]
    ]
   ]
  )

(defn content [state]
  #_[:div.grid-container
     [:header.row [welcome-block state]]
     [:main [notifications-block state] [badges-block state]
      ]

     ]

  [:div#dashboard-container
   [m/modal-window]
   [welcome-block state]
   [:div.row
    [notifications-block state]
    [badges-block state]
    [help-block]

    ]
   [:div.row
    [profile-block state]
    [connections-block state]
    [explore-block state]


    ]])


(defn handler [site-navi]
  (let [state (atom {})]
    (init-data state)
    (fn [] (layout/dashboard site-navi [content state]))))

