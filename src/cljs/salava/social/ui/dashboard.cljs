(ns salava.social.ui.dashboard
  (:require [salava.core.ui.layout :as layout]))

(defn welcome-block [state]
  [:div#welcome-block {:class "block"}
   [:div.welcome-block.block-content
    "Welcome"
    ]]
  )
#_(defn notifications-block [state]
  [:div#notifications-block {:class "block"}
   [:div.notifications-block.block-content
    [:i.fa.fa-rss.icon]
    [:span.title "notifications"]
    ]

   ]
  )

(defn notifications-block [state]
  [:div#notifications-block {:class "block col-sm-4"}
   [:div.notifications-block.block-content
    [:i.fa.fa-rss.icon]
    [:span.title "notifications"]
    ]

   ]
  )

(defn badges-block [state]
  [:div#badge-block {:class "block col-sm-4"}
   [:div.badge-block.block-content
    [:i.fa.fa-certificate.icon]
    [:span.title "Badges"]
    ]
   ]
  )
(defn explore-block [state]
  [:div#explore-block {:class "block col-sm-4"}
   [:div.explore-block.block-content
    [:i.fa.fa-search.icon]
    [:span.title "Explore"]
    ]
   ]
  )
(defn connections-block [state]
  [:div#connections-block {:class "block col-sm-4"}
   [:div.connections-block.block-content
    [:i.fa.fa-group.icon]
    [:span.title
    "Connections"]
    ]
   ]
  )
(defn profile-block [state]
  [:div#profile-block {:class "block col-sm-4"}
   [:div.profile-block.block-content
    [:i.fa.fa-user.icon]
    [:span.title
    "Profile"]]])

(defn help-block [state]
  [:div#help-block {:class "block col-sm-4"}
   [:div.help-block.block-content
    [:i.fa.fa-info-circle.icon]
    [:span.title
    "Help"]
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
    [welcome-block state]
    [:div.row
     [notifications-block state]
     [badges-block state]
     [profile-block state]
     ]
    [:div.row

    [explore-block state]


    [connections-block state]
    [help-block]
    ]])


(defn handler [site-navi]
  (let [state {}]
    (fn [] (layout/default-no-sidebar  site-navi [content state]))))

