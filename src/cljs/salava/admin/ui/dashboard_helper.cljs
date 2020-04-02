(ns salava.admin.ui.dashboard-helper
  (:require
   [cljsjs.recharts]
   [clojure.string :refer [blank? lower-case]]
   [salava.core.i18n :refer [t]]
   [reagent.core :refer [atom cursor adapt-react-class create-class]]
   [reagent.session :as session]))

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
   ;:inner-radius 15
   ;:outer-radius 40
   :padding-angle 1
   :label false
   :label-line false
   :legend-type "wye"
   :dataKey :value}})


(defn user-growth-chart [{:keys [width height data]}]
  (let [{:keys [default-width default-height aspect bar-settings]} settings
        ToolTip (adapt-react-class js/window.Recharts.Tooltip.)
        ResponsiveContainer (adapt-react-class js/window.Recharts.ResponsiveContainer.)
        Legend (adapt-react-class js/window.Recharts.Legend.)
        Cell (adapt-react-class js/window.Recharts.Cell.)
        BarChart (adapt-react-class js/window.Recharts.BarChart.)
        Bar (adapt-react-class js/window.Recharts.Bar.)
        XAxis (adapt-react-class js/window.Recharts.XAxis.)
        YAxis (adapt-react-class js/window.Recharts.YAxis.)]
   [:div {:style {:width "100%"}}
    [ResponsiveContainer
     {:height 155}
     [BarChart
      {:data data
       :margin {:bottom 25}} ;:right 30 :left 20}}
      [XAxis {:dataKey :name}]
      [YAxis]
      [ToolTip]
      [Legend {:icon-size 8}]
      ;[Bar {:dataKey :total :fill (:default colors) :stackId "a"}]
      [Bar {:dataKey :existing-users :fill (:purple colors) :stackId "a"}]
      [Bar {:dataKey :growth :fill (:yellow colors) :stackId "a"}]
      [Bar {:dataKey :active-users :fill (:danger colors) :stackId "b"}]]]]))


(defn draw-line [{:keys [width data]}]
  (let [ToolTip (adapt-react-class js/window.Recharts.Tooltip.)
        ResponsiveContainer (adapt-react-class js/window.Recharts.ResponsiveContainer.)
        Legend (adapt-react-class js/window.Recharts.Legend.)
        CartesianGrid (adapt-react-class js/window.Recharts.CartesianGrid.)
        LineChart (adapt-react-class js/window.Recharts.LineChart.)
        XAxis (adapt-react-class js/window.Recharts.XAxis.)
        YAxis (adapt-react-class js/window.Recharts.YAxis.)
        Line (adapt-react-class js/window.Recharts.Line.)
        Label (adapt-react-class js/window.Recharts.Label.)]
     (reduce
      (fn [r d]
        (let [{:keys [info lines title xlabel ylabel]} d]
         (conj r
          [:div {:style {:width "50%" :margin-bottom "20px"}}
           (when-not (blank? title) [:div [:span [:b title]]])
           [ResponsiveContainer
            {:height 180}
            (into
             [LineChart
              {:data info}
              [XAxis {:dataKey :name}
               (when xlabel [Label {:value xlabel :offset 0 :position "insideBottom"}])]
              [YAxis
               (when ylabel [Label {:value ylabel :position "outside" :angle -90}])]
              [ToolTip]
              [Legend {:icon-size 8  :verticalAlign "top"}]]
             (for [l lines]
               [Line {:dataKey (:key l) :type "monotone" :stroke (:stroke l) :activeDot (:activeDot l) :strokeWidth (:strokeWidth l)}]))]])))
      [:div.flex-container]
      data)))

(defn make-pie [{:keys [width data]}]
  (let [PieChart (adapt-react-class js/window.Recharts.PieChart.)
        Pie (adapt-react-class js/window.Recharts.Pie.)
        ToolTip (adapt-react-class js/window.Recharts.Tooltip.)
        Cell (adapt-react-class js/window.Recharts.Cell.)
        Legend (adapt-react-class js/window.Recharts.Legend.)
        Label (adapt-react-class js/window.Recharts.Label.)
        Text (adapt-react-class js/window.Recharts.Text.)
        ResponsiveContainer (adapt-react-class js/window.Recharts.ResponsiveContainer.)
        {:keys [default-width default-height aspect pie-settings]} settings]
      (reduce
        (fn [r d]
         (let [{:keys [slices title]} d]
           (conj r
             [:div
              [ResponsiveContainer
               {:width (or width default-width) :aspect aspect}
               [PieChart
                {:margin {:top 30 :left 20}}
                (reduce (fn [r c] (conj r [Cell {:fill (:fill c)}]))
                 [Pie (assoc pie-settings :data slices)]
                 slices)
                [ToolTip]
                [Legend {:icon-size 8}]]]
              (when-not (blank? title)[:div.row [:span [:b title]]])])))
        [:div.flex-container]
        data)))


(defn panel-box [data]
 (when data
  (let [
        {:keys [type heading info icon]} data
        {:keys [since-last-login since-last-month total]} info]
   [:div.col-md-4.col-sm-4.col-xs-6
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
         :else [:div [:span {:aria-hidden "true"
                             :dangerouslySetInnerHTML {:__html "&nbsp;"}}]])]]]])))

(defn panel-box-chart [data]
 (when data
  (let [{:keys [type heading icon chart-type chart-data size split?]} data
        size-class (case size
                     :lg "col-md-12 col-sm-12 col-xs-12"
                     :md "col-md-6 col-sm-6 col-xs-12"
                    "col-md-4 col-sm-6 col-xs-12")]
    [:div {:class size-class}
     [:div.panel-box.panel-chart
      [:div.panel-chart-content
       [:div.panel-icon-wrapper.rounded {:class type}
        [:div.icon-bg.bg
         [:i.fa.panel-icon.text {:class icon}]]]
       [:div.panel-subheading.pad heading]]
      [:div.panel-chart-wrapper.panel-chart-wrapper-relative
       (case chart-type
         :pie   (make-pie {:width 210 :data chart-data}) ;(if split? (reduce (fn [r p] (conj r [make-pie {:width 200 :data [p]}])) [:div.flex-container] chart-data) (make-pie {:width 200 :data chart-data}))
         ;:visibility-bar (make-visibility-bar {:width 500 :data chart-data})
         :user-growth-chart (user-growth-chart {:width 500 :data (sort-by :order < chart-data)})
         :line (draw-line {:width 500 :data chart-data})
         [:div])]]])))
