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
            [clojure.string :refer [lower-case]]))


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
     {:width width :height height :aspect 1.3}
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

(defn content [state]
  (let [{:keys [users badges last-month-active-users last-month-registered-users all-badges last-month-added-badges pages]} @state]
    [:div {:class "admin-stats"}
     [m/modal-window]
     [:div#panel-boxes
      [:div.row
        [:div.col-md-4.col-sm-4
         [:div.panel-box.panel-chart
          [:div.panel-chart-content
           [:div.panel-icon-wrapper.rounded-circle
            [:div.icon-bg.bg-users
             [:i.fa.fa-user-o.text-users.panel-icon]]]
           [:div.panel-numbers (:total users)]
           [:div.panel-subheading (t :admin/Users)]
           [:div.panel-description
            (cond
              (pos? (:since-last-login users))
              [:div [:span.text-success [:i.fa.fa-angle-up.fa-fw] [:b (:since-last-login users)]] " " (t :admin/Increasesincelastlogin)]
              (pos? (:since-last-month users))
              [:div [:span.text-success [:i.fa.fa-angle-up.fa-fw] [:b (:since-last-month users)]] " " (t :admin/Increasesincelastmonth)]
              :else [:span])]]]]
        [:div.col-md-4.col-sm-4
         [:div.panel-box.panel-chart
          [:div.panel-chart-content
           [:div.panel-icon-wrapper.rounded-circle
            [:div.icon-bg.bg-badge
             [:i.fa.fa-certificate.text-badges.panel-icon]]]
           [:div.panel-numbers (:total badges)]
           [:div.panel-subheading (t :badge/Badges)]
           [:div.panel-description
            (cond
              (pos? (:since-last-login badges))
              [:div [:span.text-success [:i.fa.fa-angle-up.fa-fw] [:b (:since-last-login badges)]] " " (t :admin/Increasesincelastlogin)]
              (pos? (:since-last-month badges))
              [:div [:span.text-success [:i.fa.fa-angle-up.fa-fw] [:b (:since-last-month badges)]] " " (t :admin/Increasesincelastmonth)]
              :else [:span])]]]]
          ;[:div.panel-description (t :admin/Numberofmonthlyaddedbadges)]]]

        [:div.col-md-4.col-sm-4
         [:div.panel-box
          [:div.panel-icon-wrapper.rounded-circle
           [:div.icon-bg.bg-page
            [:i.fa.fa-file-text-o.text-pages.panel-icon]]]
          [:div.panel-numbers (:total pages)]
          [:div.panel-subheading (t :page/Pages)]
          [:div.panel-description
           (cond
            (pos? (:since-last-login pages))
            [:div [:span.text-success [:i.fa.fa-angle-up.fa-fw] [:b (:since-last-login pages)]] " " (t :admin/Increasesincelastlogin)]
            (pos? (:since-last-month pages))
            [:div [:span.text-success [:i.fa.fa-angle-up.fa-fw] [:b (:since-last-month pages)]] " " (t :admin/Increasesincelastmonth)]
            :else [:span])]]]]
      [:div.row
       [:div.col-md-4.col-sm-6
         [:div.panel-box.panel-chart
          [:div.panel-chart-content
           [:div.panel-icon-wrapper.rounded
            [:div.icon-bg.bg-users
             [:i.fa.fa-user-o.text-users.panel-icon]]]
           ;[:div.panel-numbers (:total users)]
           [:div.panel-subheading.pad (t :admin/Accountactivation)]]
          [:div.panel-chart-wrapper.panel-chart-wrapper-relative
            [:div (make-pie-chart {:width 200
                                   :height 150
                                   :data [{:name (t :admin/Activated) :value (:activated users) :fill (:success colors)}
                                          {:name (t :admin/Notactivated) :value (:not-activated users) :fill (:danger colors)}]})]]]]
       [:div.col-md-4.col-sm-6
            [:div.panel-box.panel-chart
             [:div.panel-chart-content
              [:div.panel-icon-wrapper.rounded
               [:div.icon-bg.bg-badge
                [:i.fa.fa-certificate.text-badges.panel-icon]]]
              ;[:div.panel-numbers (:total users)]
              [:div.panel-subheading.pad (t :admin/BadgeAcceptance)]]
             [:div.panel-chart-wrapper.panel-chart-wrapper-relative
               [:div (make-pie-chart {:width 200
                                      :height 150
                                      :data [{:name (t :social/pending) :value (:pending badges) :fill (:info colors)}
                                             {:name (t :social/accepted) :value (:accepted badges) :fill (:success colors)}
                                             {:name (t :social/declined) :value (:declined badges) :fill (:danger colors)}]})]]]]
       [:div.col-md-4.col-sm-6
         [:div.panel-box.panel-chart
          [:div.panel-chart-content
           [:div.panel-icon-wrapper.rounded
            [:div.icon-bg.bg-badge
             [:i.fa.fa-certificate.text-badges.panel-icon]]]
           ;[:div.panel-numbers (:total users)]
           [:div.panel-subheading.pad (t :badge/Badgevisibility)]]
          [:div.panel-chart-wrapper.panel-chart-wrapper-relative
            [:div (make-pie-chart {:width 200
                                   :height 150
                                   :data [{:name (session/get :site-name) :value (:internal badges) :fill (:primary colors)}
                                          {:name (str (lower-case (t :page/Public))) :value (:public badges) :fill (:success colors)}
                                          {:name (str (lower-case (t :page/Private))) :value (:private badges) :fill (:warning colors)}]})]]]]]
      [:div.row
       [:div.col-md-4.col-sm-6
        [:div.panel-box.panel-chart
         [:div.panel-chart-content
          [:div.panel-icon-wrapper.rounded
           [:div.icon-bg.bg-page
            [:i.fa.fa-file-text-o.text-pages.panel-icon]]]
          ;[:div.panel-numbers (:total users)]
          [:div.panel-subheading.pad (t :page/Pagevisibility)]]
         [:div.panel-chart-wrapper.panel-chart-wrapper-relative
           [:div (make-pie-chart {:width 200
                                  :height 150
                                  :data [{:name (session/get :site-name) :value (:internal pages) :fill (:primary colors)}
                                         {:name (str (lower-case (t :page/Public))) :value (:public pages) :fill (:success colors)}
                                         {:name (str (lower-case (t :page/Private))) :value (:private pages) :fill (:warning colors)}]})]]]]]]]))

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
