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

(def colors
 {:default "#82ca9d"
  :primary "#0275d8"
  :success "#5cb85c"
  :info "#5bc0de"
  :warning "#f0ad4e"
  :danger "#d9534f"
  :yellow "#FFC658"
  :purple "#8884D8"})

(defn chart-box-pie [data]
  (let [{:keys []} data]
    [:div.panel-box.panel-chart
     [:div.panel-chart-content]]))


(defn content [state]
  (let [{:keys [users badges last-month-active-users last-month-registered-users all-badges last-month-added-badges pages created issued]} @state]

    [:div {:class "admin-stats"}
     [m/modal-window]
     [:div#panel-boxes
      [:div.row
       [dh/panel-box {:heading (t :admin/Users) :icon "fa-user-o" :info users :type "b-user"}]
       [dh/panel-box {:heading (t :badge/Badges) :icon "fa-certificate" :info badges :type "b-badge"}]
       [dh/panel-box {:heading (t :page/Pages) :icon "fa-file-text-o" :info users :type "b-page"}]
       (when created [dh/panel-box {:heading (t :badgeIssuer/Selfiebadges) :icon "fa-user-plus" :info created :type "b-user"}])]
      [:div.row
       #_[dh/panel-box-chart {:heading (t :admin/BadgeAcceptance)
                              :icon "fa-pie-chart"
                              :type "b-badge"
                              :chart-type :pie
                              :chart-data [{:slices [{:name (t :social/pending) :value (:pending badges) :fill (:info colors)}
                                                     {:name (t :social/accepted) :value (:accepted badges) :fill (:default colors)}
                                                     {:name (t :social/declined) :value (:declined badges) :fill (:danger colors)}]}]}]
       [dh/panel-box-chart {:size :lg
                            ;:heading (t :admin/Sharing)
                            :icon "fa-pie-chart"
                            :type "b-page"
                            :chart-type :pie ;:visibility-bar
                            :split? true
                            :chart-data [{:title "Issuing"
                                          :slices [{:name "factory" :value (:factory-badges badges) :fill (:primary colors)}
                                                   {:name "Passport" :value (:total issued) :fill (:default colors)}
                                                   {:name "Other" :value (- (:total badges)(+ (:factory-badges badges) (:total issued))) :fill (:yellow colors)}]}
                                         {:title (t :admin/BadgeAcceptance)
                                          :slices [{:name (t :social/pending) :value (:pending badges) :fill (:info colors)}
                                                   {:name (t :social/accepted) :value (:accepted badges) :fill (:default colors)}
                                                   {:name (t :social/declined) :value (:declined badges) :fill (:danger colors)}]}
                                         {:title "User distribution"#_(t :admin/Users)
                                          :slices [{:name (t :page/Public) :value (:public users) :fill (:default colors)}
                                                   {:name (t :admin/Notactivated) :value (:not-activated users) :fill (:danger colors)}
                                                   {:name (t :core/Internal) :value (:internal users) :fill (:yellow colors)}]}

                                         {:title "Badge distribution" #_(t :badge/Badges)
                                          :slices [{:name (t :page/Public) :value (:public badges) :fill (:default colors)}
                                                   {:name (t :page/Private) :value (:private badges) :fill (:purple colors)}
                                                   {:name (t :core/Internal) :value (:internal badges) :fill (:yellow colors)}]}
                                         {:title "Page distribution" #_(t :page/Pages)
                                          :slices [{:name (t :page/Public) :value (:public pages) :fill (:default colors)}
                                                   {:name (t :page/Private) :value (:private pages) :fill (:purple colors)}
                                                   {:name (t :core/Internal) :value (:internal pages) :fill (:yellow colors)}]}]}]]
      [:div.row
       [dh/panel-box-chart {:size :md
                            :heading (t :admin/Usergrowth)
                            :icon "fa-bar-chart-o"
                            :type "b-user"
                            :chart-type :user-growth-chart
                            :chart-data [{:name (str "1 " (t :admin/month))
                                          :growth (:since-last-month users)
                                          :existing-users (- (:total users) (:since-last-month users))
                                          :total (:total users)
                                          :active-users (:last-month-login-count users)
                                          :order 4}
                                         {:name (str "3 " (t :admin/months))
                                          :growth (:since-3-month users)
                                          :existing-users (- (:total users) (:since-3-month users))
                                          :total (:total users)
                                          :active-users (:3-month-login-count users)
                                          :order 3}
                                         {:name (str "6 " (t :admin/months))
                                          :growth (:since-6-month users)
                                          :existing-users (- (:total users) (:since-6-month users))
                                          :total (:total users)
                                          :active-users (:6-month-login-count users)
                                          :order 2}
                                         {:name (str "1 " (t :admin/year))
                                          :growth (:since-1-year users)
                                          :existing-users (- (:total users) (:since-1-year users))
                                          :total (:total users)
                                          :active-users (:1-year-login-count users)
                                          :order 1}]}]
       [dh/panel-box-chart {:size :md
                            :heading (t :admin/Growth)
                            :icon "fa-line-chart"
                            :type "b-user"
                            :chart-type :line
                            :chart-data [{:name (str "1 " (t :admin/year))
                                          :users (:since-1-year users)
                                          :badges (:since-1-year badges)
                                          :pages (:since-1-year pages)}
                                         {:name (str "3 " (t :admin/months))
                                          :users (:since-3-month users)
                                          :badges (:since-3-month badges)
                                          :pages (:since-3-month pages)}
                                         {:name (str "6 " (t :admin/months))
                                          :users (:since-6-month users)
                                          :badges (:since-6-month badges)
                                          :pages (:since-6-month pages)}
                                         {:name (str "1 " (t :admin/month))
                                          :users (:since-last-month users)
                                          :badges (:since-last-month badges)
                                          :pages (:since-last-month pages)}]}]]]]))


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
