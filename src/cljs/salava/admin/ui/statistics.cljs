(ns salava.admin.ui.statistics
  (:require [reagent.core :refer [atom cursor]]
            [reagent.session :as session]
            [reagent-modals.modals :as m]
            [clojure.string :refer [trim]]
            [salava.core.ui.ajax-utils :as ajax]
            [salava.core.ui.layout :as layout]
            [salava.core.ui.grid :as g]
            [salava.core.ui.helper :refer [path-for]]
            [salava.core.i18n :refer [t]]
            [salava.core.helper :refer [dump]]))


(defn content [state]
  (let [{:keys [register-users last-month-active-users last-month-registered-users all-badges last-month-added-badges pages]} @state]
    [:div {:class "admin-stats"}
     [:h1 {:class "uppercase-header"} (t :admin/Statistics)]
     [:div.row
      [:div {:class "col-md-12"}
      [:h3 (t :admin/Users)]
       [:div [:label (t :admin/Registeredusers)]  register-users]
       [:div [:label (t :admin/Numberofmonthlyactiveuser)] last-month-active-users]
       [:div [:label (t :admin/Numberofmonthlyregisteredusers)]  last-month-registered-users]
      [:h3 (t :badge/Badges)]
       [:div [:label (t :admin/Totalbadges)]  all-badges]
       [:div [:label (t :admin/Numberofmonthlyaddedbadges) ]  last-month-added-badges]
      [:h3 (t :page/Pages)]
       [:div [:label (t :admin/Totalpages)]  pages]
       ]]]))
(defn init-data [state]
  (ajax/GET 
   (path-for "/obpv1/admin/stats")
   {:handler (fn [data]
               (reset! state data))}))

(defn handler [site-navi]
  (let [state (atom {:register-users nil
                     :last-month-active-users nil
                     :last-month-registered-users nil
                     :all-badges nil
                     :last-month-added-badges nil
                     :pages nil})]
    (init-data state)
    (fn []
      (layout/default site-navi (content state)))))
