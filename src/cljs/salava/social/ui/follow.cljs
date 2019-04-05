(ns salava.social.ui.follow
  (:require [reagent.core :refer [atom create-class]]
            [reagent.session :as session]
            [salava.core.ui.ajax-utils :as ajax]
            [salava.core.ui.helper :refer [path-for current-path not-activated?]]
            [salava.social.ui.helper :refer [social-plugin?]]
            [salava.core.i18n :refer [t]]
            [salava.core.helper :refer [dump]]
            [salava.core.ui.popover :refer [info]]))

(def followed?
  (atom nil))


(defn follow-button-badge [badge-id followed?]
  [:div [:button {:class    "btn btn-primary follow "
            :disabled (if (not-activated?) "disabled" "")
            :on-click #(ajax/POST
                        (path-for (str "/obpv1/social/create_connection_badge/" badge-id))
                        {:response-format :json
                         :keywords?       true
                         :handler         (fn [data]
                                            (do
                                              (reset! followed? (:connected? data))))
                         :error-handler   (fn [{:keys [status status-text]}]
                                            (.log js/console (str status " " status-text))
                                            )})}
   (str " " (t :social/Follow)) ] [info {:content (t :social/Badgenotificationsinfo) :placement "left" :style {:vertical-align "super"}}]])

(defn unfollow-ajax-post [badge-id]
  (ajax/POST
   (path-for (str "/obpv1/social/delete_connection_badge/" badge-id))
   {:response-format :json
    :keywords?       true
    :handler         (fn [data]
                       (do
                         (reset! followed? (:connected? data))))
    :error-handler   (fn [{:keys [status status-text]}]
                       (.log js/console (str status " " status-text)))}))


(defn unfollow-button-badge [badge-id followed?]
 [:div [:i (str (t :social/Youfollowbadge)" ")] [:button {:class "btn btn-primary follow"
            :on-click #(unfollow-ajax-post badge-id)}
    (str " " (t :social/Unfollow)) ]])





(defn init-data [badge-id]
  (ajax/GET
   (path-for (str "/obpv1/social/connected/" badge-id))
   {:handler (fn [data]
               (reset! followed? data)
                )}))

(defn follow-badge [badge-id init-followed?]
  (let [user (session/get :user)]
    (init-data badge-id)
    (if (and user (social-plugin?))
        (create-class {:component-will-mount (fn [] (reset! followed? init-followed?))
                       :reagent-render (fn [] (if @followed?
                                                (unfollow-button-badge badge-id followed?)
                                                (follow-button-badge badge-id followed?)))
                       :component-will-unmount (fn [] (reset! followed? nil))})
        )))

