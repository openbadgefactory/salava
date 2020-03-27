(ns salava.admin.ui.dashboard-helper
  (:require
   [cljsjs.recharts]
   [clojure.string :refer [blank? lower-case]]
   [salava.core.i18n :refer [t]]
   [reagent.core :refer [atom cursor adapt-react-class create-class]]
   [reagent.session :as session]))

;(def PieChart (adapt-react-class js/window.Recharts.PieChart.))
;(def Pie (adapt-react-class js/window.Recharts.Pie.))
;(def ToolTip (adapt-react-class js/window.Recharts.Tooltip.))
;(def Cell (adapt-react-class js/window.Recharts.Cell.))
;(def Legend (adapt-react-class js/window.Recharts.Legend.))
;(def ResponsiveContainer (adapt-react-class js/window.Recharts.ResponsiveContainer.))
(def BarChart (adapt-react-class js/window.Recharts.BarChart.))
(def Bar (adapt-react-class js/window.Recharts.Bar.))
;(def CartesianGrid (adapt-react-class js/window.Recharts.CartesianGrid.))
(def XAxis (adapt-react-class js/window.Recharts.XAxis.))
(def YAxis (adapt-react-class js/window.Recharts.YAxis.))


(def colors
 {:default "#82ca9d"
  :primary "#0275d8"
  :success "#5cb85c"
  :info "#5bc0de"
  :warning "#f0ad4e"
  :danger "#d9534f"
  :yellow "#FFC658"
  :purple "#8884D8"})

(def settings
 {:default-width 250
  :default-height 150
  :aspect 1.3
  :pie-settings
  {:fill (:default colors)
   :inner-radius 20
   :padding-angle 1
   :label false
   :label-line false
   :legend-type "circle"
   :dataKey :value}})

(defn make-pie [{:keys [width height data]}]
  (let [PieChart (adapt-react-class js/window.Recharts.PieChart.)
        Pie (adapt-react-class js/window.Recharts.Pie.)
        ToolTip (adapt-react-class js/window.Recharts.Tooltip.)
        Cell (adapt-react-class js/window.Recharts.Cell.)
        Legend (adapt-react-class js/window.Recharts.Legend.)
        ResponsiveContainer (adapt-react-class js/window.Recharts.ResponsiveContainer.)
        {:keys [default-width default-height aspect pie-settings]} settings]
    [ResponsiveContainer
     {:width (or width default-width)  :aspect aspect}
     [PieChart
      (reduce (fn [r c] (conj r [Cell {:fill (:fill c)}]))
       [Pie (assoc pie-settings :data data)]
       data)
      [ToolTip]
      [Legend {:icon-size 8}]]]))

(defn make-visibility-bar [{:keys [width height data]}]
  (let [{:keys [default-width default-height aspect bar-settings]} settings
        ToolTip (adapt-react-class js/window.Recharts.Tooltip.)
        ResponsiveContainer (adapt-react-class js/window.Recharts.ResponsiveContainer.)
        Legend (adapt-react-class js/window.Recharts.Legend.)
        Cell (adapt-react-class js/window.Recharts.Cell.)]
   [:div {:style {:width "100%"}}
    [ResponsiveContainer
     {:height 155}
     [BarChart
      {:data data}
       ;:margin {:top 20 :right 30 :left 20}}
      [XAxis {:dataKey :name}]
      [YAxis]
      [ToolTip]
      [Legend {:icon-size 8}]
      [Bar {:dataKey (keyword (t :page/Public)) :fill (:default colors) :stackId "a"}]
      [Bar {:dataKey (session/get :site-name) :fill (:yellow colors) :stackId "a"}]
      [Bar {:dataKey (keyword (t :page/Private)) :fill (:purple colors) :stackId "a"}]
      [Bar {:dataKey (keyword (t :admin/Notactivated)) :fill (:danger colors) :stackId "a"}]]]]))


(defn panel-box [data]
 (when data
  (let [
        {:keys [type heading info icon]} data
        {:keys [since-last-login since-last-month total]} info]
   [:div.col-md-3.col-sm-4.col-xs-6
    [:div.panel-box.panel-chart
     [:div.panel-chart-content
      [:div.panel-icon-wrapper.rounded-circle {:class type}
       [:div.icon-bg.bg
        [:i.fa.panel-icon.text {:class icon}]]]
      [:div.panel-numbers total]
      [:div.panel-subheading heading]
      [:div.panel-description
       (cond
         (pos? since-last-login)
         [:div [:span.text-success [:i.fa.fa-angle-up.fa-fw] [:b since-last-login]] " " (t :admin/Increasesincelastlogin)]
         (pos? since-last-month)
         [:div [:span.text-success [:i.fa.fa-angle-up.fa-fw] [:b since-last-month]] " " (t :admin/Increasesincelastmonth)]
         :else [:span])]]]])))

(defn panel-box-chart [data]
 (when data
  (let [{:keys [type heading icon chart-type chart-data size]} data
        size-class (case size
                     :md "col-md-6 col-sm-6 col-xs-12"
                    "col-md-3 col-sm-6 col-xs-12")]
    [:div {:class size-class}
     [:div.panel-box.panel-chart
      [:div.panel-chart-content
       [:div.panel-icon-wrapper.rounded {:class type}
        [:div.icon-bg.bg
         [:i.fa.panel-icon.text {:class icon}]]]
       [:div.panel-subheading.pad heading]]
      [:div.panel-chart-wrapper.panel-chart-wrapper-relative
       (case chart-type
         :pie (make-pie {:width 200 :data chart-data})
         :visibility-bar (make-visibility-bar {:width 500 :data chart-data})
         [:div])]]])))
