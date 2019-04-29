(ns salava.gallery.ui.block
  (:require [salava.core.ui.ajax-utils :as ajax]
            [salava.core.ui.helper :refer [path-for]]
            [salava.core.ui.badge-grid :refer [badge-grid-element]]))

(defn init-data [element state]
  (ajax/GET
    (path-for (str "/obpv1/gallery/recent") true)
    {:params {:visibility (:visibility @state)
              :user-id (:user-id @state)
              :type element}
     :handler (fn [data] (swap! state assoc element data))}))

(defn badge-grid [badges badge-small-view]
  (into [:div {:class "row wrap-grid" :id "grid"}]
        (for [element-data (if badge-small-view (sort-by :mtime > badges) (take 6 (sort-by :mtime > badges)))]
          (badge-grid-element element-data nil "profile" nil))))

(defn ^:export recentbadges [data]
  (let [{:keys [edit-mode? user-id badge-small-view user visibility]} data
         state (atom {:user_id user-id :visibility visibility})]
    (init-data "badges" data)
    (fn []
      [:div
       (badge-grid (:badges @state) true)
       ]
      )
    ))
(defn ^:export recentpages [user-id visibility]
  (let [state (atom {})]))

