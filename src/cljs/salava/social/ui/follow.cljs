(ns salava.social.ui.follow
  (:require [reagent.core :refer [atom create-class]]
            [reagent.session :as session]
            [salava.core.ui.ajax-utils :as ajax]
            [salava.core.ui.helper :refer [path-for current-path not-activated?]]
            [salava.social.ui.helper :refer [social-plugin?]]
            [salava.core.i18n :refer [t]]
            [salava.core.helper :refer [dump]]))

(def followed?
  (atom nil))


(defn follow-button-badge [badge-content-id followed?]
  [:button {:class    "btn btn-primary follow"
            :disabled (if (not-activated?) "disabled" "")
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
   (str " " (t :social/Follow)) ])

(defn unfollow-ajax-post [badge-content-id]
  (ajax/POST
   (path-for (str "/obpv1/social/delete_connection_badge/" badge-content-id))
   {:response-format :json
    :keywords?       true          
    :handler         (fn [data]
                       (do
                         (reset! followed? (:connected? data))))
    :error-handler   (fn [{:keys [status status-text]}]
                       (.log js/console (str status " " status-text)))}))


(defn unfollow-button-badge [badge-content-id followed?]
  [:button {:class    "btn btn-primary unfollow"
            :on-click #(unfollow-ajax-post badge-content-id)}
    (str " " (t :social/Unfollow)) ])





(defn init-data [badge-content-id]
  (ajax/GET
   (path-for (str "/obpv1/social/connected/" badge-content-id))
   {:handler (fn [data]
               (reset! followed? data)
                )}))

(defn follow-badge [badge-content-id init-followed?]
  (let [user (session/get :user)]
    (init-data badge-content-id)
    (if (and user (social-plugin?))
        (create-class {:component-will-mount (fn [] (reset! followed? init-followed?))
                       :reagent-render (fn [] (if @followed?
                                                (unfollow-button-badge badge-content-id followed?)
                                                (follow-button-badge badge-content-id followed?)))
                       :component-will-unmount (fn [] (reset! followed? nil))})
        )))

