(ns salava.social.ui.dashboard
  (:require [salava.core.ui.layout :as layout]))

(defn welcome-block [state]
  [:div#welcome-block {:class "block"}
   [:div.welcome-block.block-content
    "Welcome"
    ]]
  )
(defn notifications-block [state]
  [:div#notifications-block {:class "block"}
   [:div.notifications-block.block-content
    [:i.fa.fa-rss.icon]
    [:span.title "notifications"]
    ]

   ]
  )

(defn badges-block [state]
  [:div#badge-block {:class "block"}
   [:div.badge-block.block-content
    [:i.fa.fa-certificate.icon]
    [:span.title "Badges"]
    ]
   ]
  )
(defn explore-block [state]
  [:div#explore-block {:class "block"}
   [:div.explore-block.block-content
    [:i.fa.fa-search.icon]
    [:span.title "Explore"]
    ]
   ]
  )
(defn connections-block [state]
  [:div#connections-block {:class "block"}
   [:div.connections-block.block-content
    [:i.fa.fa-group.icon]
    [:span.title
    "Connections"]
    ]
   ]
  )
(defn profile-block [state]
  [:div#profile-block {:class "block"}
   [:div.profile-block.block-content
    [:i.fa.fa-user.icon]
    [:span.title
    "Profile"]]])

(defn help-block [state]
  [:div#help-block {:class "block"}
   [:div.help-block.block-content
    [:i.fa.fa-info-circle.icon]
    [:span.title
    "Help"]
    ]
   ]
  )

(defn content [state]
  [:div.grid-container
   [:header.row [welcome-block state]]
   [:main [notifications-block state] [badges-block state]
    ]

   ]
  #_[:div.row
   [:div;#dashboard-container
   [:div;.container
    [welcome-block state]
    [notifications-block state]
    [explore-block state]
    [badges-block state]
    [profile-block state]
    [connections-block state]
    [help-block]
    ]]])


(defn handler [site-navi]
  (let [state {}]
    (fn [] (layout/default-no-sidebar  site-navi [content state]))))

