(ns salava.admin.ui.stats-helper
  (:require
   [cljsjs.recharts]
   [clojure.string :refer [blank? lower-case split join]]
   [salava.core.i18n :refer [t]]
   [reagent.core :refer [atom cursor adapt-react-class create-class]]
   [reagent.session :as session]
   [salava.core.time :refer [date-from-unix-time iso8601-to-unix-time unix-time]]
   [salava.core.ui.helper :refer [path-for]]
   [salava.core.ui.ajax-utils :as ajax]))

(defn %percentage [v t]
  (str (Math/round (double (* (/ v t) 100))) "%"))

(def colors
 {:default "#82ca9d"
  :primary "#0275d8"
  :success "#5cb85c"
  :info "#5bc0de"
  :warning "#f0ad4e"
  :danger "#d9534f"
  :yellow "#FFC658"
  :purple "#8884D8"
  :facebook "#3b5998"
  :twitter "#00aced"
  :linkedin "#007bb6"
  :pinterest "#cb2027"})

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

#_(defn user-growth-chart [{:keys [width height data]}]
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

(defn make-bar [{:keys [width data]}]
  (let [{:keys [default-width default-height aspect bar-settings]} settings
        ToolTip (adapt-react-class js/window.Recharts.Tooltip.)
        ResponsiveContainer (adapt-react-class js/window.Recharts.ResponsiveContainer.)
        Legend (adapt-react-class js/window.Recharts.Legend.)
        Cell (adapt-react-class js/window.Recharts.Cell.)
        BarChart (adapt-react-class js/window.Recharts.BarChart.)
        Bar (adapt-react-class js/window.Recharts.Bar.)
        XAxis (adapt-react-class js/window.Recharts.XAxis.)
        YAxis (adapt-react-class js/window.Recharts.YAxis.)
        Label (adapt-react-class js/window.Recharts.Label.)
        DefaultTooltipContent (adapt-react-class js/window.Recharts.DefaultTooltipContent.)]
     (reduce
      (fn [r d]
       (let [{:keys [info bars title xlabel ylabel dataKeyY dataKeyX xlabel ylabel]} d]
         (conj r
          [:div {:style {:width (or width "50%") :margin-bottom "20px"}}
           (when-not (blank? title) [:div [:span [:b title]]])
           [ResponsiveContainer
            {:height 180}
            (into
             [BarChart
              {:data info}
              [XAxis (as-> {} $
                           (when dataKeyX (merge $ {:dataKey dataKeyX})))
                (when xlabel [Label {:value xlabel  :position "insideBottom" :dy 8}])]

              [YAxis (as-> {:interval 0} $
                           (when dataKeyY (merge $ {:dataKey dataKeyY})))
               (when ylabel [Label {:value ylabel  :position "outside" :angle -90 :dx -20}])]
              [ToolTip]]
             (for [b bars]
               [Bar {:dataKey (:key b) :fill (:fill b) :stackId (:stackId b)}]))]])))
      [:div.flex-container]
      data)))

(defn composed-chart [{:keys [width data tooltipLabel]}]
 (let [{:keys [default-width default-height aspect bar-settings]} settings
       ToolTip (adapt-react-class js/window.Recharts.Tooltip.)
       ResponsiveContainer (adapt-react-class js/window.Recharts.ResponsiveContainer.)
       Legend (adapt-react-class js/window.Recharts.Legend.)
       Cell (adapt-react-class js/window.Recharts.Cell.)
       BarChart (adapt-react-class js/window.Recharts.BarChart.)
       Bar (adapt-react-class js/window.Recharts.Bar.)
       XAxis (adapt-react-class js/window.Recharts.XAxis.)
       YAxis (adapt-react-class js/window.Recharts.YAxis.)
       ComposedChart (adapt-react-class js/window.Recharts.ComposedChart.)
       Label (adapt-react-class js/window.Recharts.Label.)
       Line (adapt-react-class js/window.Recharts.Line.)]

    (reduce
      (fn [r d]
        (let [{:keys [info elements title xlabel ylabel dataKeyY dataKeyX xlabel ylabel nameX nameY]} d]
         (conj r
          [:div {:style {:width (or width "50%") :margin-bottom "20px"}}
           (when-not (blank? title) [:div [:span [:b title]]])
           [ResponsiveContainer
            {:height 500}
            (into
             [ComposedChart
              {:data info}
              [XAxis (as-> {} $
                           (when dataKeyX (merge $ {:dataKey dataKeyX})))
                (when xlabel [Label {:value xlabel  :position "insideBottom" :dy 8}])]

              [YAxis (as-> {:interval "preserveStart" :scale "log" :domain ["auto" "auto"] } $
                           (when dataKeyY (merge $ {:dataKey dataKeyY})))
               (when ylabel [Label {:value ylabel  :position "outside" :angle -90 :dx -20}])]
              [ToolTip (as-> {:label :badge_count :separator " "} $
                             (when tooltipLabel (merge $  {:labelFormatter (fn [name]  (if-not (clojure.string/blank? name) (str  name " " tooltipLabel) nil))
                                                           :formatter (fn [value name props]  (if-not (clojure.string/blank? name) (str ": " value) nil))})))]]

             (for [e elements]
               (case (:type e)
                 "bar" [Bar {:unit (:unit e) :legendType (:legendType e) :name " " #_(:name e) :dataKey (:key e) :fill (:fill e) :stackId (:stackId e)}]
                 "line" [Line {:name (:name e)  :dot (:dot e) :dataKey (:key e) :type "monotone" :stroke (:stroke e) :activeDot (:activeDot e) :strokeWidth (:strokeWidth e)}]
                 nil)))]])))
      [:div.flex-container]
      data)))

(defn draw-line [{:keys [width data tooltipLabel]}]
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
        (let [{:keys [info lines title xlabel ylabel ]} d]
         (conj r
          [:div {:style {:width "50%" :margin-bottom "20px"}}
           (when-not (blank? title) [:div [:span [:b title]]])
           [ResponsiveContainer
            {:height 185}
            (into
             [LineChart
              {:data info}
              [XAxis {:dataKey :name}
               (when xlabel [Label {:value xlabel :offset 0 :position "outside" :dy 15}])]
              [YAxis
               (when ylabel [Label {:value ylabel :position "outside" :angle -90}])]
              [ToolTip (as-> {} $
                             (when tooltipLabel  (merge $ {:labelFormatter (fn [name] (str name " " tooltipLabel))})))]
              [Legend {:icon-size 8  :verticalAlign "top"}]]
             (for [l lines]
               [Line {:name (:name l) :dataKey (:key l) :type "monotone" :stroke (:stroke l) :activeDot (:activeDot l) :strokeWidth (:strokeWidth l)}]))]])))
      [:div.flex-container]
      (remove #(nil? %) data))))

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
              (when-not (blank? title)[:div.row [:span [:b title]]])
              [ResponsiveContainer
               {:width (or width default-width) :aspect aspect}
               [PieChart
                ;{:margin {:top 30 :left 20}}
                (reduce (fn [r c] (conj r [Cell {:fill (:fill c)}]))
                 [Pie (assoc pie-settings :data slices)]
                 slices)
                [ToolTip {:formatter (fn [name value props]
                                       (let [{:keys [percentage]} (-> (js->clj props :keywordize-keys true) :payload :payload)]
                                         (str name "; " percentage)))}]
                [Legend {:icon-size 8}]]]])))
              ;(when-not (blank? title)[:div.row [:span [:b title]]])])))
        [:div.flex-container]
        data)))

(defn init-social-media-stats [sm-atom ts state]
 (let [space-id @(cursor state [:space-id])
       url (if (pos? space-id) (str "/obpv1/stats/social_media/" ts "/" space-id) "/obpv1/stats/social_media")]
  (ajax/GET
   (path-for url true)
   {:handler (fn [data]
               (reset! sm-atom data))})))

(defn- process-time [time]
 (if (string? time)
   time
   (let [t  (as-> (date-from-unix-time (* time 1000)) $
                  (split $ ".")
                  (reverse $))]
       (->> t (map #(if (= 1 (count %)) (str "0"%) %)) (join "-")))))

(defn make-pie-social [{:keys [width data]}]
  (let [PieChart (adapt-react-class js/window.Recharts.PieChart.)
        Pie (adapt-react-class js/window.Recharts.Pie.)
        ToolTip (adapt-react-class js/window.Recharts.Tooltip.)
        Cell (adapt-react-class js/window.Recharts.Cell.)
        Legend (adapt-react-class js/window.Recharts.Legend.)
        Label (adapt-react-class js/window.Recharts.Label.)
        Text (adapt-react-class js/window.Recharts.Text.)
        ResponsiveContainer (adapt-react-class js/window.Recharts.ResponsiveContainer.)
        {:keys [default-width default-height aspect pie-settings]} settings
        time-atom (atom (:ctime (first data)))]
        ;data-atom (atom data)]
      (conj
        (reduce
          (fn [r d]
           (let [{:keys [slices title]} d]
             (conj r
               [:div
                (when-not (blank? title)[:div.row [:span [:b title]]])
                [ResponsiveContainer
                 {:width (or width default-width) :aspect aspect}
                 [PieChart
                  ;{:margin {:top 30 :left 20}}
                  (reduce (fn [r c] (conj r [Cell {:fill (:fill c)}]))
                   [Pie (assoc pie-settings :data slices)]
                   slices)
                  [ToolTip {:formatter (fn [name value props]
                                         (let [{:keys [percentage]} (-> (js->clj props :keywordize-keys true) :payload :payload)]
                                           (str name "; " percentage)))}]
                  [Legend {:icon-size 8}]]]])))
                ;(when-not (blank? title)[:div.row [:span [:b title]]])])))
          [:div.flex-container]
          data)
        [:div
         [ResponsiveContainer
          {:width (or width default-width) :aspect aspect}
          (reduce
           (fn [r d]
             (let [icon (case (:name d)
                          "Facebook" "fa-facebook-square"
                          "Twitter" "fa-twitter-square"
                          "Pinterest" "fa-pinterest-square"
                          "Linkedin" "fa-linkedin-square")]
               (conj r
                [:div {:style {:margin "5px"}}
                 [:p
                  [:i.fa {:class icon :style {:font-size "30px" :color (:fill d)}}]
                  [:span {:style {:margin "0 10px" :font-size "14px" :font-weight "600"} } (:percentage d) "  (" (str (:value d) ") ")]]])))
           [:div {:style {:width (str width "px") :height "100%" :padding "10px" :text-align "start"}}
            [:p
             [:span {:style {:font-size "14px" :font-weight "600"}} (t :admin/Totalbadgessharedtosocialmedia) ": " (:total (first data))]]]
           (:slices (first data)))]])))


(defn panel-box [data]
 (when data
  (let [
        {:keys [type heading info icon]} data
        {:keys [lastlogin lastmonth total]} info]
   [:div.col-md-4.col-sm-6.col-xs-12
    [:div.panel-box.panel-chart
     [:div.panel-chart-content
      [:div.panel-icon-wrapper.rounded-circle {:class type}
       [:div.icon-bg.bg
        [:i.fa.panel-icon.text {:class icon}]]]
      [:div.panel-numbers total]
      [:div.panel-subheading heading]
      [:div.panel-description
       (cond
         (pos? lastlogin)
         [:div [:span.text-success [:i.fa.fa-angle-up.fa-fw] [:b lastlogin]] " " (t :admin/Increasesincelastlogin)]
         (pos? lastmonth)
         [:div [:span.text-success [:i.fa.fa-angle-up.fa-fw] [:b lastmonth]] " " (t :admin/Increasesincelastmonth)]
         :else [:div [:span {:aria-hidden "true"
                             :dangerouslySetInnerHTML {:__html "&nbsp;"}}]])]]]])))

(defn panel-box-chart [data]
 (when data
  (let [{:keys [type heading icon chart-type chart-data size split? tooltipLabel data-atom]} data
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
         :pie   (make-pie {:width 190 :data chart-data})
         :line (draw-line {:width 500 :data chart-data :tooltipLabel tooltipLabel})
         :bar (make-bar {:width "100%" :data chart-data})
         :mixed (composed-chart {:width "100%" :data chart-data :tooltipLabel tooltipLabel})
         [:div])]]])))

(defn social-media-box [state]
  (let [data-atom (atom {:value {} :ctime nil})
        {:keys [id value ctime]} @data-atom
        {:keys [facebook linkedin pinterest twitter]} value
        id-atom @(cursor state [:space-id])
        time-atom (cursor data-atom [:ctime])]
   (create-class
    {:reagent-render
     (fn []
       (let [value @(cursor data-atom [:value])
             linkedin @(cursor data-atom [:value :linkedin])
             facebook @(cursor data-atom [:value :facebook])
             pinterest @(cursor data-atom [:value :pinterest])
             twitter @(cursor data-atom [:value :twitter])
             time-atom (cursor data-atom [:ctime])]
         ^{:key @(cursor state [:space-id])}
         [:div.row
          [:div.col-md-12.col-sm-12.col-xs-12
           [:div.panel-box.panel-chart
            [:div.panel-chart-content
             [:div.panel-icon-wrapper.rounded {:class "b-user"}
               [:div.icon-bg.bg
                [:i.fa.panel-icon.text {:class "fa-share-square"}]]]
             [:div.panel-subheading.pad (t :admin/Socialmedia)]]
            [:div.panel-chart-wrapper.panel-chart-wrapper-relative
              [:div.row
               [:div.col-md-6
                [:div.form-group
                  [:label {:for "date"} (t :admin/Showstatssince) ": "]
                  [:input.form-control
                   {:style {:max-width "unset"}
                    :type "date"
                    :id "date"
                    :max (process-time (unix-time))
                    :value (process-time @time-atom)
                    :on-change #(do
                                  (reset! time-atom (.-target.value %)))}]]]]

              (make-pie-social {:width 300 :data [{:ctime @time-atom
                                                   :total (+ facebook linkedin pinterest twitter)
                                                   :slices [{:name "Facebook" :value facebook :fill (:facebook colors) :percentage (%percentage facebook (+ facebook linkedin pinterest twitter))}
                                                            {:name "Twitter" :value twitter :fill (:twitter colors) :percentage (%percentage twitter (+ facebook linkedin pinterest twitter))}
                                                            {:name "Pinterest" :value pinterest :fill (:pinterest colors) :percentage (%percentage pinterest (+ facebook linkedin pinterest twitter))}
                                                            {:name "Linkedin" :value linkedin :fill (:linkedin colors) :percentage (%percentage linkedin (+ facebook linkedin pinterest twitter))}]}]})]]]]))
     :component-did-mount
     (fn [] (init-social-media-stats data-atom nil state))

     :component-did-update
     (fn []
      (js/window.setTimeout
       #(let [space-id @(cursor state [:space-id])
              ts (if (string? @(cursor data-atom [:ctime])) (iso8601-to-unix-time @(cursor data-atom [:ctime])) @(cursor data-atom [:ctime]))
              url (if ts (str "/obpv1/stats/social_media/" ts "/" space-id) "/obpv1/stats/social_media")]
             (js/setTimeout
              (fn []
                (ajax/GET
                 (path-for url true)
                 {:handler (fn [data]
                              (reset! data-atom data))}))))
       500))})))
