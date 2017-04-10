(ns salava.extra.socialuser.ui.connect
  (:require [reagent.core :refer [atom cursor]]
            [ajax.core :as ajax]
            [reagent.session :as session]
            [salava.core.helper :refer [dump]]
            [salava.core.ui.helper :refer [path-for]]
            [salava.core.i18n :refer [t]])
  )


(defn init-data [user-id state]
  (ajax/GET
   (path-for (str "/obpv1/socialuser/user-connection/" user-id))
   {:handler (fn [data]
               (swap! state assoc :status (:status data)))}))

(defn follow-button [user-id state]
  [:button {:class    "btn btn-primary follow"
            :on-click  #(ajax/POST
                         (path-for (str "/obpv1/socialuser/user-connection/" user-id))
                         {:response-format :json
                          :keywords?       true          
                          :handler         (fn [data]
                                            (do
                                              (init-data user-id state)))
                          :error-handler   (fn [{:keys [status status-text]}]
                                             (.log js/console (str status " " status-text))
                                             )})}
   (str " " (t :social/Follow)) ])

(defn pending-button []
  [:button {:class    "btn btn-primary follow"
            :on-click  #()}
   (str " " (t :social/Pending) "..") ])


(defn unfollow-button [user-id state]
  [:button {:class    "btn btn-primary unfollow"
            :on-click #(ajax/DELETE
                         (path-for (str "/obpv1/socialuser/user-connection/" user-id))
                         {:response-format :json
                          :keywords?       true          
                          :handler         (fn [data]
                                            (do
                                              (init-data user-id state)))
                          :error-handler   (fn [{:keys [status status-text]}]
                                             (.log js/console (str status " " status-text))
                                             )})}
    (str " " (t :social/Unfollow)) ])



(defn handler [user-id]
  (let [state (atom {:status nil
                     :user-id user-id})
        user (session/get :user)]

    (init-data user-id state)
    (fn []
      (if user
        [:div {:class "pull-right text-right"}
         (case (:status @state)
           "accepted" (unfollow-button user-id state)
           "pending"  (pending-button)
           (follow-button user-id state))
         ]))))
