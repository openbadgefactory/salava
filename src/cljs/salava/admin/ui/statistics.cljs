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
    [:div
     [:h1 (t :admin/Statistics)]
     [:div.row
      [:li {:class "dropdown"
            :key "target"}  
       [:a {:href "dropdown-toggle" :data-toggle "dropdown" :role "button" :aria-haspopup true :aria-expanded true} "kissa"]
       [:ul {:class "dropdown-menu"}
        [:li [:a "jee"]]
        [:li [:a "joo"]]]]
      [:div {:class "col-md-12"}
       [:div [:label (t :admin/Registeredusers)] ": " register-users]
       [:div [:label (t :admin/Totalbadges)] ": " all-badges]
       [:div [:label (t :admin/Numberofmonthlyaddedbadges) ] ": " last-month-added-badges]
       [:div [:label (t :admin/Totalpages)] ": " pages]
       [:div [:label (t :admin/Numberofmonthlyactiveuser)] ": " last-month-active-users]
       [:div [:label (t :admin/Numberofmonthlyregisteredusers)] ": " last-month-registered-users]]]]))

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
