(ns salava.social.ui.connections
  (:require [salava.core.ui.layout :as layout]
            [reagent.core :refer [atom cursor]]
            [salava.core.i18n :as i18n :refer [t]]
            [salava.core.ui.ajax-utils :as ajax]
            [salava.core.helper :refer [dump]]
            [salava.gallery.ui.badges :as b]
            [reagent-modals.modals :as m]
            [salava.core.ui.helper :as h :refer [unique-values navigate-to path-for]]))

(defn init-data [state]
  (ajax/GET
    (path-for "/obpv1/social/connections_badge" true)
    {:handler (fn [data]
                (swap! state assoc :badges data))}))


(defn unfollow [badge-content-id state]
  [:a {:href "#" :on-click #(ajax/POST
                                (path-for (str "/obpv1/social/delete_connection_badge/" badge-content-id))
                                {:response-format :json
                                 :keywords?       true          
                                 :handler         (fn [data]
                                                    (do
                                                      (init-data state)))
                                 :error-handler   (fn [{:keys [status status-text]}]
                                                    (.log js/console (str status " " status-text))
                                                    )})} (t :social/Unfollow)])

(defn badge-connections [badges state]
  [:div.panel
     [:div.panel-heading
      [:h3
       (str (t :badge/Badges) " (" (count badges) ")")]]
     [:div.panel-body
        [:table {:class "table" :summary (t :badge/Badgeviews)}
         [:thead
          [:tr
           [:th (t :badge/Badge)]
           [:th (t :badge/Name)]
           [:th ""
            ]]]
         (into [:tbody]
               (for [badge-views badges
                     :let [{:keys [id name image_file reg_count anon_count latest_view]} badge-views]]
                 [:tr
                  [:td [:img.badge-icon {:src (str "/" image_file)
                                         :alt name}]]
                  [:td.name [:a {:href "#"
                                 :on-click #(do
                                              (b/open-modal id false init-data state)
                                              (.preventDefault %)) } name]]
                   [:td (unfollow id state)]
                  ]))]]
     ])


(defn content [state]
  (let [badges (:badges @state)]
    [:div {:id "badge-stats"}
     [m/modal-window]
     (badge-connections badges state)
     
     ]))



(defn handler [site-navi]
  (let [state (atom {:badges []})]
    (init-data state)
    
    (fn []
      (layout/default site-navi (content state)))))
