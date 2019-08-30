(ns salava.user.ui.register-complete
  (:require [reagent.session :as session]
            [reagent.core :refer [atom]]
            [salava.core.ui.layout :as layout]
            [salava.core.ui.error :as err]
            [salava.core.ui.helper :refer [js-navigate-to navigate-to path-for]]
            [salava.core.i18n :refer [t]]
            [salava.core.ui.ajax-utils :as ajax]
            [salava.core.helper :refer [dump]]))

(defn content [state]
 [:div#registration-page
  [:div.panel {:id "narrow-panel"}
   [:div.panel-heading
    [:div.media
     [:div.media-left
      [:div.logo-image-icon-url #_{:style {:vertical-align "middle"}}]]
     [:div.media-body {:style {:vertical-align "bottom"}}
      [:span {:style {:font-size "16px" :font-weight "600"}} (str (t :core/Emailactivation2) " " (session/get :site-name) (t :core/Service))]]]]
   [:div {:style {:margin-top "10px"}} [:hr.border]]
   [:div.panel-body
    [:div.col-md-12
     [:div
      [:p (t :social/Notactivatedbody1)]
      [:ul
       [:li (t :social/Notactivatedbody2)]
       [:li (t :social/Notactivatedbody3)]
       [:li (t :social/Notactivatedbody4)]
       [:li (t :social/Notactivatedbody5)]
       [:li (t :social/Notactivatedbody6)]]]
     [:div.col-md-12 {:style {:text-align "center"}}[:a.btn.btn-primary {:style {:margin-top "20px" :text-align "center"}
                                                                          :href "#" :on-click #(do
                                                                                                (.push window.dataLayer (clj->js {:event "virtualPage" :vpv "app/user/register/complete" :registration-method "form"})) 
                                                                                                (.preventDefault %)
                                                                                                (navigate-to "/social"))}
                                                      (t :core/Continue)]]]]]])

(defn init-data [state]
  (ajax/GET
    (path-for "/obpv1/user/register/complete")
    {:handler (fn [data]
                (swap! state assoc :permission (:status data)))}
    (fn [] (swap! state assoc :permission "error"))))

(defn handler [site-navi params]
  (let [state (atom {:permission "initial"})]
    (init-data state)
    (fn []
      (cond
        (= "initial" (:permission @state)) (layout/landing-page site-navi [:div])
        (= "success" (:permission @state))  (layout/landing-page site-navi (content state))
        :else  (layout/landing-page site-navi  (err/error-content))))))
