(ns salava.social.ui.connections
  (:require [salava.core.ui.layout :as layout]
            [reagent.core :refer [atom cursor]]
            [salava.core.i18n :as i18n :refer [t]]
            [salava.core.ui.ajax-utils :as ajax]
            [salava.core.helper :refer [dump]]
            [salava.core.ui.helper :as h :refer [unique-values navigate-to path-for]]))

(defn init-data [state]
  (ajax/GET
    (path-for "/obpv1/social/connections_badge" true)
    {:handler (fn [data]
                (swap! state assoc :badges data))}))

(defn badge-content [badge state]
  (let [badge-content-id (:badge_content_id badge)]
    [:div
     (str  (:name badge) " ")
     [:a {:href "#" :on-click #(ajax/POST
                                (path-for (str "/obpv1/social/delete_connection_badge/" badge-content-id))
                                {:response-format :json
                                 :keywords?       true          
                                 :handler         (fn [data]
                                                    (do
                                                      (init-data state)))
                                 :error-handler   (fn [{:keys [status status-text]}]
                                                    (.log js/console (str status " " status-text))
                                                    )})} (t :social/Unfollow)]]))

(defn content [state]
  (let [badges (:badges @state)]
    [:div {:class "my-badges pages"}
     (into [:div {:class "row"}]
           (for [badge badges]
              (badge-content badge state)))
     ]))



(defn handler [site-navi]
  (let [state (atom {:badges []})]
    (init-data state)
    
    (fn []
      (layout/default site-navi (content state)))))
