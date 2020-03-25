(ns salava.admin.ui.statistics
  (:require [reagent.core :refer [atom cursor adapt-react-class]]
            [reagent.session :as session]
            [reagent-modals.modals :as m]
            [clojure.string :refer [trim]]
            [salava.core.ui.ajax-utils :as ajax]
            [salava.core.ui.layout :as layout]
            [salava.core.ui.grid :as g]
            [salava.core.ui.helper :refer [path-for]]
            [salava.core.i18n :refer [t]]
            [salava.core.helper :refer [dump]]
            [cljsjs.recharts]))


#_(defn content [state]
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

(defn make-pie-chart
  ""
  [{:keys [width height data]}]
  (let [PieChart (adapt-react-class js/window.Recharts.PieChart.)
        Pie (adapt-react-class js/window.Recharts.Pie.)
        ToolTip (adapt-react-class js/window.Recharts.Tooltip.)
        Cell (adapt-react-class js/window.Recharts.Cell.)
        Legend (adapt-react-class js/window.Recharts.Legend.)]
    [PieChart {:width width
               :height height
               :margin {:top 5}}
     (reduce (fn [r c] (conj r [Cell {:fill (:fill c)}]))
      [Pie {:fill "#82ca9d"
            :data data
            :inner-radius 40
            :padding-angle 1
            :label false
            :label-line false
            :legend-type "square"
            :dataKey :value}]

      data)
     [ToolTip]
     [Legend]]))



(defn content [state]
  (let [{:keys [users last-month-active-users last-month-registered-users all-badges last-month-added-badges pages]} @state]
    [:div {:class "admin-stats"}
     [m/modal-window]
     [:div;.panel.panel-default
      [:div.row
       [:div.col-md-12
        [:div.col-md-4
         [:div.panel-box
          [:div.panel-icon-wrapper.rounded-circle
           [:div.icon-bg.bg-users
            [:i.fa.fa-user-o.text-users]]]
          [:div.panel-numbers (:total users)]
          [:div.panel-subheading (t :admin/Registeredusers)]
          (when (pos? (:since-last-visited users))
            [:div.panel-description
             [:span.text-success [:i.fa.fa-angle-up.fa-fw] (:since-last-login users)] "since last login"])
          [:div.chart-wrapper
           [:div.recharts-responsive-container
             [:div (make-pie-chart {:width 250 :height 250 :data [{:name "activated" :value (:activated users) :fill "#73a839d9"} {:name "not-activated" :value (:not-activated users) :fill "#FF521B"}]})]]]]]
        [:div.col-md-4
         [:div.panel-box
          [:div.panel-icon-wrapper.rounded-circle
           [:div.icon-bg.bg-badge
            [:i.fa.fa-certificate.text-badges]]]
          [:div.panel-numbers all-badges]
          [:div.panel-subheading (t :admin/Totalbadges)]]]
          ;[:div.panel-description (t :admin/Numberofmonthlyaddedbadges)]]]

        [:div.col-md-4
         [:div.panel-box
          [:div.panel-icon-wrapper.rounded-circle
           [:div.icon-bg.bg-page
            [:i.fa.fa-file-text-o.text-pages]]]
          [:div.panel-numbers pages]
          [:div.panel-subheading (t :admin/Totalpages)]]]]]]]))

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
