(ns salava.oauth.ui.status
  (:require [reagent.core :refer [atom cursor]]
            [reagent.session :as session]
            [salava.oauth.ui.helper :refer [facebook-link linkedin-link]]
            [salava.core.ui.ajax-utils :as ajax]
            [salava.core.ui.helper :refer [js-navigate-to accepted-terms? base-path navigate-to path-for]]
            [salava.core.ui.layout :as layout]
            [salava.core.helper :refer [dump]]
            [salava.core.i18n :refer [t]]))

(defn unlink-linkedin [active-atom]
  (ajax/GET
    (path-for "/oauth/linkedin/deauthorize")
    {:handler (fn [data]
                (if (= (:status data) "success")
                  (reset! active-atom false)))}))

(defn content [state]
  (let [error-message-atom (cursor state [:error-message])
        active-atom (cursor state [:active])
        link-fn (if (= (:service @state) "facebook") facebook-link linkedin-link)]
    [:div {:id "login-page"}
     [:div {:class "panel"}
      [:div {:class "panel-body"}
       (if (:initializing @state)
         (ajax/loading-message)
         [:div
          (if @error-message-atom
            [:div {:class "alert alert-warning"}
             @error-message-atom])
          [:div
           (if-not @active-atom
             (link-fn nil nil)
             (if (:no-password? @state)
               [:span (t :oauth/Cannotunlink) " " (t :oauth/Setpasswordfrom)
                " " [:a {:href (path-for "/user/edit")} (t :oauth/accountsettings)] "."]
               (link-fn unlink-linkedin active-atom)))]])]]]))

(defn init-data [state service]
  (ajax/GET
    (path-for (str "/obpv1/oauth/status/" service))
    {:handler (fn [data]
                (swap! state assoc :active (:active data)
                                   :no-password? (:no-password? data)
                                   :initializing false))}))

(defn handler [site-navi params]
  (let [flash-message (t (keyword (session/get! :flash-message)))
        service (:service params)
        state (atom {:initializing  true
                     :active        false
                     :no-password?  false
                     :service service
                     :error-message (if (not-empty flash-message) flash-message)})]
    (init-data state service)
    (fn []
      (if (and (not (clojure.string/blank? (session/get-in [:user :id])))(= "false" (accepted-terms?))) (js-navigate-to (path-for (str "/user/terms/" (session/get-in [:user :id])))))
      (layout/default site-navi (content state)))))
