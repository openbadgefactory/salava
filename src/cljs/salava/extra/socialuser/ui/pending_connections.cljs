(ns salava.extra.socialuser.ui.pending-connections
  (:require [reagent.core :refer [atom cursor]]
            [salava.core.ui.ajax-utils :as ajax]
            [salava.core.helper :refer [dump]]
            [salava.core.ui.modal :as mo]
            [salava.core.ui.helper :refer [path-for]]
            [salava.core.i18n :refer [t]])
  )


(defn init-data [state]
  (ajax/GET
   (path-for (str "/obpv1/socialuser/user-pending-requests"))
   {:handler (fn [data]
               (swap! state assoc :pending data))}))


(defn accept [owner-id state parent-data]
  [:button {:class    "btn btn-primary"
            :on-click #(do
                         (ajax/POST
                          (path-for (str "/obpv1/socialuser/user-pending-requests/" owner-id "/accepted"))
                          {:response-format :json
                           :keywords?       true          
                           :handler         (fn [data]
                                              (do
                                                
                                                (init-data state)
                                                
                                                ;((:init-data parent-data) (:state parent-data))
                                                ))
                           :error-handler   (fn [{:keys [status status-text]}]
                                              (.log js/console (str status " " status-text))
                                              )})
                         (.preventDefault %))}
       (t :social/Accept) ]
  )

(defn decline [owner-id state parent-data]
  [:button {:class         "btn btn-primary"
            :on-click #(do
                         (ajax/POST
                          (path-for (str "/obpv1/socialuser/user-pending-requests/" owner-id "/declined"))
                          {:response-format :json
                           :keywords?       true          
                           :handler         (fn [data]
                                              (do
                                                (init-data state)
                                                ;((:init-data parent-data) (:state parent-data))
                                                ))
                           :error-handler   (fn [{:keys [status status-text]}]
                                              (.log js/console (str status " " status-text))
                                              )})
                         (.preventDefault %))}
        (t :social/Decline)]
  )

(defn request [{:keys [owner_id profile_picture first_name last_name]} state parent-data]
  [:div.row {:key owner_id}
   [:div.col-md-12
    [:div.badge-container-pending
     
     [:div.row
      [:div.col-md-12
       [:div.media
        [:div.pull-left
         [:img.badge-image {:src (str "/" profile_picture)
                            :on-click #(mo/open-modal [:user :profile] {:user-id owner_id})}]]
        [:div.media-body
         [:h5.media-heading
          [:a {:on-click #(mo/open-modal [:user :profile] {:user-id owner_id})}
           (str  first_name " " last_name " haluaa seurata sinua" )]]]]]]
     [:div {:class "row button-row"}
      [:div.col-md-12
       (accept owner_id state parent-data)
       (decline owner_id state parent-data)
       ]]]]])

(defn pending-requests [state parent-data]
  (into [:div {:id "pending-badges"}]
        (for [item (:pending @state)]
          (do
            (request item state parent-data)))))


(defn handler [parent-data]
  (let [state (atom {:pending []})]

    (init-data state)
    (fn []
      (pending-requests state parent-data)
      )))
