(ns salava.badge.ui.embed
  (:require [reagent.core :refer [atom cursor]]
            [salava.core.ui.ajax-utils :as ajax]
            [salava.core.ui.layout :as layout]
            [salava.core.i18n :refer [t]]
            [reagent.session :as session]
            [salava.core.ui.helper :refer [path-for]]))


(defn error-view [state]
  [:div {:id "badge-gallery"}
       [:div {:id "grid"}
        [:div {:class "media grid-container"}
         [:div.media-content
           [:div.media-body
            [:div.media-heading
             [:div.heading-link]]
            [:div "The owner of the badge has set it private and therefore it can’t be shown."]]]]]])

(defn success-view [state]
  (let [{:keys [content permission]} @state
        {:keys [id image_file name description issuer_content_name issuer_content_url recipients]} (first (filter  #(= (:language_code %) (:default_language_code %)) content))
        url (str (session/get :site-url) (path-for (str "/badge/info/" id)))]
    [:div {:id "badge-gallery"}
     [:div {:id "grid"}
      [:div {:class "media grid-container"}
       [:a {:href url :target "_blank"}
        [:div.media-content
         (if image_file
           [:div.media-left
            [:img {:src (str "/" image_file)
                   :alt name}]
            ])
         [:div.media-body
          [:div.media-heading
           [:div.heading-link 
            name]]
          [:div.media-issuer
           issuer_content_name]
          
          
          [:div.media-description description]]]]
       [:div.media-bottom
        [:div {:class "pull-left"}
                                        ;[:a.bottom-link {:href (path-for (str "/gallery/badgeview/" badge-id))} [:i {:class "fa fa-share-alt"}] (t :badge/Share)]
         ]
        ]]]]))

(defn content [state]
  (let [{:keys [id permission]} @state
        url                     (str (session/get :site-url) (path-for (str "/badge/info/" id)))]
    
    (cond
      (= "success" permission) (success-view state)
      (= "error" permission)   (error-view state)
      :else                    [:div])))


(defn init-data [state id]
  (ajax/GET
    (path-for (str "/obpv1/badge/info-embed/" id))
    {:handler (fn [data]
                (reset! state (assoc data :id id
                                     :permission "success"
                                     :show-link-or-embed-code nil
                                     :initializing false)))}
    (fn [] (swap! state assoc :permission "error"))
    ))


(defn handler [site-navi params]
  (let [id (:badge-id params) 
        state (atom {:permission "initial"}) ]
    (init-data state id)
    (fn []
      (layout/embed-page (content state)))))
