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
            [cljsjs.recharts]
            [clojure.string :refer [lower-case]]
            [salava.admin.ui.dashboard-helper :as dh]))


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
        Legend (adapt-react-class js/window.Recharts.Legend.)
        ResponsiveContainer (adapt-react-class js/window.Recharts.ResponsiveContainer.)]
    [ResponsiveContainer
     {:width width  :aspect 1.3}
     [PieChart {:margin {:top 5}}
      (reduce (fn [r c] (conj r [Cell {:fill (:fill c)}]))
       [Pie {:fill "#82ca9d"
             :data data
             :inner-radius 20
             :padding-angle 1
             :label false
             :label-line false
             :legend-type "circle"
             :dataKey :value}]

       data)
      [ToolTip]
      [Legend {:icon-size 8}]]]))

(def colors {:primary "#0275d8"
             :success "#5cb85c"
             :info "#5bc0de"
             :warning "#f0ad4e"
             :danger "#d9534f"})

(defn chart-box-pie [data]
  (let [{:keys []} data]
    [:div.panel-box.panel-chart
     [:div.panel-chart-content]]))


(defn content [state]
  (let [{:keys [users badges last-month-active-users last-month-registered-users all-badges last-month-added-badges pages]} @state]

    [:div {:class "admin-stats"}
     [m/modal-window]
     [:div#panel-boxes
      [:div.row
       [dh/panel-box {:heading (t :admin/Users) :icon "fa-user-o" :info users :type "b-user"}]
       [dh/panel-box {:heading (t :badge/Badges) :icon "fa-certificate" :info badges :type "b-badge"}]
       [dh/panel-box {:heading (t :page/Pages) :icon "fa-file-text-o" :info users :type "b-page"}]]
      [:div.row
       [dh/panel-box-chart {:heading (t :admin/BadgeAcceptance)
                            :icon "fa-certificate"
                            :type "b-badge"
                            :chart-type :pie
                            :chart-data [{:name (t :social/pending) :value (:pending badges) :fill (:info colors)}
                                         {:name (t :social/accepted) :value (:accepted badges) :fill (:success colors)}
                                         {:name (t :social/declined) :value (:declined badges) :fill (:danger colors)}]}]
       [dh/panel-box-chart {:size :md
                            :heading (t :badge/Visibility)
                            :icon "fa-eye"
                            :type "b-page"
                            :chart-type :visibility-bar
                            :chart-data [{:name (t :admin/Users)
                                          (keyword (t :page/Public)) (:public users)
                                          (keyword (t :admin/Notactivated)) (:not-activated users)
                                          (session/get :site-name) (:internal users)
                                          :amt (:total users)}
                                         {:name (t :badge/Badges)
                                          (keyword (t :page/Public)) (:public badges)
                                          (keyword (t :page/Private)) (:private badges)
                                          (session/get :site-name) (:internal badges)
                                          :amt (:total badges)}
                                         {:name (t :page/Pages)
                                          (keyword (t :page/Public)) (:public pages)
                                          (keyword (t :page/Private)) (:private pages)
                                          (session/get :site-name) (:internal pages)
                                          :amt (:total pages)}]}]]]]))





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
    #_(fn [] (layout/dashboard site-navi [content state]))
    (fn []
      (layout/default site-navi (content state)))))
