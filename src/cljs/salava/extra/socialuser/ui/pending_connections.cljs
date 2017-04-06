(ns salava.extra.socialuser.ui.pending-connections
  (:require [reagent.core :refer [atom cursor]]
            [ajax.core :as ajax]
            [salava.core.helper :refer [dump]]
            [salava.core.ui.helper :refer [path-for]]
            [salava.core.i18n :refer [t]])
  )


(defn init-data [state]
  (ajax/GET
   (path-for (str "/obpv1/socialuser/user-pending-requests"))
   {:handler (fn [data]
               (swap! state assoc :pending data))}))


(defn accept [owner-id state]
  [:button {:class "btn btn-primary"
            :on-click #(do
                         (ajax/POST
                          (path-for (str "/obpv1/socialuser/user-pending-requests/" owner-id "/accepted"))
                             {:response-format :json
                              :keywords?       true          
                              :handler         (fn [data]
                                                 (do
                                                   (init-data state)))
                              :error-handler   (fn [{:keys [status status-text]}]
                                                 (.log js/console (str status " " status-text))
                                                 )})
                              (.preventDefault %))}
       (t :social/Accept) ]
  )

(defn decline [owner-id state]
  [:button {:class "btn btn-primary"
                 :on-click #(do
                              (ajax/POST
                               (path-for (str "/obpv1/socialuser/user-pending-requests/" owner-id "/declined"))
                               {:response-format :json
                                :keywords?       true          
                                :handler         (fn [data]
                                                   (do
                                                     (init-data state)))
                                :error-handler   (fn [{:keys [status status-text]}]
                                                   (.log js/console (str status " " status-text))
                                                 )})
                              (.preventDefault %))}
        (t :social/Decline)]
  )

(defn request [{:keys [owner_id profile_picture first_name last_name]} state]
  [:div.row {:key owner_id}
   [:div.col-md-12
    [:div.badge-container-pending
     
     [:div.row
      [:div.col-md-12
       [:div.media
        [:div.pull-left
         [:img.badge-image {:src (str "/" profile_picture)}]]
        [:div.media-body
         [:h4.media-heading
          (str  first_name " " last_name " haluaa seurata sinua" )]
         
         

         
         

         ]]]]
     [:div {:class "row button-row"}
      [:div.col-md-12
       (accept owner_id state)
       (decline owner_id state)
       ]]]]])

(defn pending-requests [state]
  (into [:div {:id "pending-badges"}]
        (for [item (:pending @state)]
          (do
            (request item state)))))


(defn handler []
  (let [state (atom {:pending []})]

    (init-data state)
    (fn []
      (pending-requests state)
      )))
