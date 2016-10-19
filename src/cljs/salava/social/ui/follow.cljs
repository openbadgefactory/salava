(ns salava.social.ui.follow
  (:require [reagent.core :refer [atom]]
            [reagent.session :as session]
            [salava.core.ui.ajax-utils :as ajax]
            [salava.core.ui.helper :refer [path-for current-path]]
            [salava.core.i18n :refer [t]]
            [salava.core.helper :refer [dump]]))


(defn follow-button-badge [badge-content-id followed?]
  [:button {:class    "btn btn-primary text-right"
            :on-click #(ajax/POST
                        (path-for (str "/obpv1/social/create_connection_badge/" badge-content-id))
                        {:response-format :json
                         :keywords?       true          
                         :handler         (fn [data]
                                            (do
                                              (reset! followed? (:connected? data))))
                         :error-handler   (fn [{:keys [status status-text]}]
                                            (.log js/console (str status " " status-text))
                                            )})}
   [:i {:class "fa fa-plus" :aria-hidden "true"}] (str " " (t :social/Follow)) ])

(defn unfollow-button-badge [badge-content-id followed?]
  [:button {:class    "btn btn-primary text-right"
            :on-click #(ajax/POST
                        (path-for (str "/obpv1/social/delete_connection_badge/" badge-content-id))
                                  {:response-format :json
                                   :keywords?       true          
                                   :handler         (fn [data]
                                                      (do
                                                        (reset! followed? (:connected? data))))
                                   :error-handler   (fn [{:keys [status status-text]}]
                                                      (.log js/console (str status " " status-text))
                                                      )})}
    (str " " (t :social/Unfollow)) ])

(defn follow-badge [badge-content-id init-followed?]
  (let [followed? (atom init-followed?)
        user (session/get :user)]
    (fn []
      (if user
        (if @followed?
          (unfollow-button-badge badge-content-id followed?)
          (follow-button-badge badge-content-id followed?))))))

