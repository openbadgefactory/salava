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
     [m/modal-window]
     [:h1 {:class "uppercase-header"} (t :admin/Statistics)]
     [:div.row
      [:div {:class "col-md-12"}
       [:h2.sectionheading (t :admin/Users)]
       [:div [:span._label.stats (t :admin/Registeredusers)]  register-users]
       [:div [:span._label.stats (t :admin/Numberofmonthlyactiveuser)] last-month-active-users]
       [:div [:span._label.stats (t :admin/Numberofmonthlyregisteredusers)]  last-month-registered-users]
       [:h2.sectionheading (t :badge/Badges)]
       [:div [:span._label.stats (t :admin/Totalbadges)]  all-badges]
       [:div [:span._label.stats (t :admin/Numberofmonthlyaddedbadges) ]  last-month-added-badges]
       [:h2.sectionheading (t :page/Pages)]
       [:div [:span._label.stats (t :admin/Totalpages)]  pages]]]]))

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
