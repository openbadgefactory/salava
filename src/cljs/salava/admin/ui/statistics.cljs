(ns salava.admin.ui.statistics
  (:require [reagent.core :refer [atom cursor adapt-react-class]]
            [reagent.session :as session]
            [reagent-modals.modals :as m]
            [clojure.string :refer [trim]]
            [salava.core.ui.ajax-utils :as ajax]
            [salava.core.ui.layout :as layout]
            [salava.core.ui.grid :as g]
            [salava.core.ui.helper :refer [path-for js-navigate-to]]
            [salava.core.i18n :refer [t]]
            [salava.core.helper :refer [dump]]
            [cljsjs.recharts]
            [clojure.string :refer [lower-case]]
            [salava.admin.ui.dashboard-helper :as dh]))


#_(defn content [state]
    (let [{:keys [register-users last-month-active-users last-month-registered-users all-badges last-month-added-badges pages]} @state]
      [:div {:class "admin-stats"}
       ;[m/modal-window]
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

(def colors
 {:default "#82ca9d"
  :primary "#0275d8"
  :success "#5cb85c"
  :info "#5bc0de"
  :warning "#f0ad4e"
  :danger "#d9534f"
  :yellow "#FFC658"
  :purple "#8884D8"})

(def sample-data
 [{:badge_count 182 :user_count 1}{:badge_count 122 :user_count 1}{:badge_count 116 :user_count 1}{:badge_count 115 :user_count 2}
  {:badge_count 110 :user_count 2}{:badge_count 107 :user_count 1}{:badge_count 103 :user_count 3}{:badge_count 101 :user_count 2}
  {:badge_count 99 :user_count 3}{:badge_count 93 :user_count 1}{:badge_count 86 :user_count 1}{:badge_count 83 :user_count 1}
  {:badge_count 82 :user_count 1}{:badge_count 81 :user_count 1}{:badge_count 80 :user_count 2}{:badge_count 79 :user_count 1}
  {:badge_count 78 :user_count 1}{:badge_count 77 :user_count 1}{:badge_count 76 :user_count 3}{:badge_count 74 :user_count 3}
  {:badge_count 73 :user_count 2}{:badge_count 72 :user_count 2}{:badge_count 71 :user_count 4}{:badge_count 70 :user_count 1}
  {:badge_count 69 :user_count 2}{:badge_count 68 :user_count 1}{:badge_count 67 :user_count 3}{:badge_count 66 :user_count 4}
  {:badge_count 65 :user_count 4}{:badge_count 64 :user_count 2}{:badge_count 63 :user_count 2}{:badge_count 62 :user_count 3}
  {:badge_count 61 :user_count 2}{:badge_count 60 :user_count 8}{:badge_count 59 :user_count 4}{:badge_count 58 :user_count 3}
  {:badge_count 55 :user_count 1}{:badge_count 54 :user_count 3}{:badge_count 53 :user_count 1}{:badge_count 52 :user_count 3}
  {:badge_count 51 :user_count 2}{:badge_count 50 :user_count 3}{:badge_count 49 :user_count 6}{:badge_count 48 :user_count 4}
  {:badge_count 47 :user_count 7}{:badge_count 46 :user_count 4}{:badge_count 45 :user_count 8}{:badge_count 44 :user_count 3}
  {:badge_count 43 :user_count 6}{:badge_count 42 :user_count 6}{:badge_count 41 :user_count 13}{:badge_count 40 :user_count 7}
  {:badge_count 39 :user_count 12}{:badge_count 38 :user_count 13}{:badge_count 37 :user_count 17}{:badge_count 36 :user_count 13}
  {:badge_count 35 :user_count 15}{:badge_count 34 :user_count 21}{:badge_count 33 :user_count 26}{:badge_count 32 :user_count 23}
  {:badge_count 31 :user_count 21}{:badge_count 30 :user_count 28}
  {:badge_count 29 :user_count 24}{:badge_count 28 :user_count 12}{:badge_count 27 :user_count 36} {:badge_count 26 :user_count 47}
  {:badge_count 25 :user_count 57}{:badge_count 24 :user_count 100} {:badge_count 23 :user_count 49} {:badge_count 22 :user_count 55}
  {:badge_count 21 :user_count 80} {:badge_count 20 :user_count 58} {:badge_count 19 :user_count 52} {:badge_count 18 :user_count 101}
  {:badge_count 17 :user_count 139} {:badge_count 16 :user_count 248} {:badge_count 15 :user_count 434} {:badge_count 14 :user_count 263}
  {:badge_count 13 :user_count 383} {:badge_count 12 :user_count 401} {:badge_count 11 :user_count 397} {:badge_count 10 :user_count 503}
  {:badge_count 9 :user_count 734} {:badge_count 8 :user_count 1102} {:badge_count 7 :user_count 1927} {:badge_count 6 :user_count 1146}
  {:badge_count 5 :user_count 1761} {:badge_count 4 :user_count 1740} {:badge_count 3 :user_count 2492}{:badge_count 2 :user_count 5459}
  {:badge_count 1 :user_count 25968}])

(defn text-content [state]
 (reduce-kv
  (fn [r k v]
    (conj r
     [:div.row
      (when v [:h2.sectionheading (t (keyword (str "admin/" (name k))))])
      (when (map? v)
        (reduce-kv
           (fn [x y z]
             (conj x
              [:div
               [:span._label.stats (t (keyword (str "admin/" (name y))))] z]))

           [:div]
           v))]))
  [:div.admin-stats]
  (-> @state (dissoc :user-badge-correlation :visible))))


(defn graphic-content [state]
  (let [{:keys [issuers users userbadges pages created issued user-badge-correlation]} @state
        {:keys [factorybadges Totalbadgesno pendingbadgescount acceptedbadgescount declinedbadgescount privatebadgescount publicbadgescount internalbadgescount badgessincelastlogin
                badgessincelastmonth badgessince3month badgessince6month badgessince1year]} userbadges
        {:keys [notactivatedusers internalusers publicusers Totalusersno userssincelastlogin userssincelastmonth
                userssince3month userssince6month userssince1year logincountsincelastlogin]} users
        {:keys [pagessincelastlogin pagessince1year pagessince6month pagessince3month pagessincelastmonth internalpagescount privatepagescount publicpagescount Totalpagesno]} pages
        {:keys [issuerssince1year issuerssince6month issuerssince3month issuerssincelastmonth issuerssincelastlogin Totalissuersno]} issuers
        {:keys [Totalissuedno issuedsincelastmonth issuedsincelastlogin]} issued
        {:keys [Totalcreatedno createdsincelastmonth createdsincelastlogin]} created]
    [:div {:class "admin-stats"}
     [:div#panel-boxes
      [:div.row
       [dh/panel-box {:heading (t :admin/Totalusersno) :icon "fa-user-o" :info {:total Totalusersno :lastlogin userssincelastlogin :lastmonth userssincelastmonth} :type "b-user"}]
       [dh/panel-box {:heading (t :admin/Totalbadgesno) :icon "fa-certificate" :info {:total Totalbadgesno :lastlogin badgessincelastlogin :lastmonth badgessincelastmonth}  :type "b-badge"}]
       [dh/panel-box {:heading (t :admin/Totalpagesno) :icon "fa-file-text-o" :info {:total Totalpagesno :lastlogin pagessincelastlogin :lastmonth pagessincelastmonth} :type "b-page"}]
       [dh/panel-box {:heading (t :admin/Totalissuersno) :icon "fa-building-o" :info {:total Totalissuersno :lastlogin issuerssincelastlogin :lastmonth issuerssincelastmonth} :type "b-page"}]
       (when created [dh/panel-box {:heading (t :badgeIssuer/Selfiebadges) :icon "fa-plus-square" :info {:total Totalcreatedno :lastlogin createdsincelastlogin :lastmonth createdsincelastmonth} :type "b-user"}])]
      [:div.row
       [dh/panel-box-chart {:size :lg
                            ;:heading (t :admin/Sharing)
                            :icon "fa-pie-chart"
                            :type "b-page"
                            :chart-type :pie ;:visibility-bar
                            :split? true
                            :chart-data [{:title (t :admin/badgeSource)
                                          :slices [{:name "Open Badge Factory " :value factorybadges :fill (:primary colors)}
                                                   {:name (session/get :site-name "Passport") :value Totalissuedno :fill (:default colors)}
                                                   {:name (t :admin/Other) :value (- Totalbadgesno (+ factorybadges Totalissuedno)) :fill (:yellow colors)}]}
                                         {:title (t :admin/badgeStatus)
                                          :slices [{:name (t :social/pending) :value pendingbadgescount :fill (:info colors)}
                                                   {:name (t :social/accepted) :value acceptedbadgescount :fill (:default colors)}
                                                   {:name (t :social/declined) :value declinedbadgescount :fill (:danger colors)}]}
                                         {:title (t :admin/userProfile)
                                          :slices [{:name (t :page/Public) :value publicusers :fill (:default colors)}
                                                   {:name (t :admin/Notactivated) :value notactivatedusers :fill (:danger colors)}
                                                   {:name (t :core/Internal) :value internalusers :fill (:yellow colors)}]}

                                         {:title (t :admin/badgeVisibility)
                                          :slices [{:name (t :page/Public) :value publicbadgescount :fill (:default colors)}
                                                   {:name (t :page/Private) :value privatebadgescount :fill (:purple colors)}
                                                   {:name (t :core/Internal) :value internalbadgescount :fill (:yellow colors)}]}
                                         {:title (t :admin/pageVisibility)
                                          :slices [{:name (t :page/Public) :value publicpagescount :fill (:default colors)}
                                                   {:name (t :page/Private) :value privatepagescount :fill (:purple colors)}
                                                   {:name (t :core/Internal) :value internalpagescount :fill (:yellow colors)}]}]}]]
      [:div.row
       [dh/panel-box-chart {:size :lg
                            :icon "fa-line-chart"
                            :type "b-user"
                            :chart-type :line
                            :chart-data [{:info [{:name 12  :total  (- Totalusersno userssince1year) :active-users (:1yearlogincount users)}
                                                 {:name 6  :total (- Totalusersno userssince6month) :active-users (:6monthlogincount users)}
                                                 {:name 3  :total (- Totalusersno userssince3month) :active-users (:3monthlogincount users)}
                                                 {:name 1  :total (- Totalusersno userssincelastmonth) :active-users (:1monthlogincount users)}
                                                 {:name 0 #_(t :admin/Lastlogin) :total Totalusersno :active-users logincountsincelastlogin}]
                                           :lines [{:name (t :admin/Totalusersno) :key "total" :stroke (:primary colors) :activeDot {:r 10} :strokeWidth 3}
                                                   {:name (t :admin/Activeusers) :key "active-users" :stroke (:danger colors)}]
                                           :title (t :admin/userGrowth)
                                           :xlabel (t :admin/noofmonths)}
                                         {:info [{:name 12  :total (- Totalbadgesno badgessince1year)}
                                                 {:name 6  :total (- Totalbadgesno badgessince6month)}
                                                 {:name 3 :total (- Totalbadgesno badgessince3month)}
                                                 {:name 1  :total (- Totalbadgesno badgessincelastmonth)}
                                                 {:name 0 #_(t :admin/now) :total Totalbadgesno}]
                                          :lines [{:name (t :admin/Totalbadgesno) :key "total" :stroke (:primary colors) :activeDot {:r 10} :strokeWidth 3}]
                                          :title (t :admin/badgeGrowth)
                                          :xlabel (t :admin/noofmonths)}
                                         {:info [{:name 12  :total (- Totalpagesno pagessince1year)}
                                                 {:name 6  :total (- Totalpagesno pagessince6month)}
                                                 {:name 3  :total (- Totalpagesno pagessince3month)}
                                                 {:name 1  :total (- Totalpagesno pagessincelastmonth)}
                                                 {:name 0 #_(t :admin/now) :total Totalpagesno}]
                                          :lines [{:name (t :admin/Totalpagesno) :key "total" :stroke (:primary colors) :activeDot {:r 10} :strokeWidth 3}]
                                          :title (t :admin/pageGrowth)
                                          :xlabel (t :admin/noofmonths)}

                                         {:info [{:name 12 :total (- Totalissuersno issuerssince1year)}
                                                 {:name 6 :total (- Totalissuersno issuerssince6month)}
                                                 {:name 3 :total (- Totalissuersno issuerssince3month)}
                                                 {:name 1 :total (- Totalissuersno issuerssincelastmonth)}
                                                 {:name 0 #_(t :admin/now) :total Totalissuersno}]
                                          :lines [{:name (t :admin/Totalissuersno) :key "total" :stroke (:primary colors) :activeDot {:r 10} :strokeWidth 3}]
                                          :title (t :admin/issuerGrowth)
                                          :xlabel (t :admin/noofmonths)}]


                             :tooltipLabel (t :admin/monthsago)}]]
      [:div.row
       [dh/panel-box-chart {:size :lg
                            :icon "fa-bar-chart"
                            :type "b-page"
                            :chart-type :mixed
                            :chart-data [{:info (sort-by :badge_count < sample-data #_(repeatedly 50 #(hash-map :badge_count (rand-int 185) :user_count (rand-int 500))) #_user-badge-correlation)
                                          :title (t :admin/userBadgeDistribution)
                                          :elements [{:legendType "none" :name (t :admin/users) :key "user_count" :fill (:warning colors) :stackId "a" :type "bar"}
                                                     {:legendType "none" :name (t :admin/users) :key "user_count" :type "line" :stroke (:primary colors) :activeDot {:r 8} :strokeWidth 3 :dot false}]
                                          :dataKeyX "badge_count"
                                          :dataKeyY "user_count"
                                          :xlabel (t :admin/noofbadges)
                                          :ylabel (t :admin/noofusers)}]
                            :tooltipLabel (t :admin/userbadges)}]]]]))

(defn export-stats [state]
  (let [url (str "/obpv1/admin/export_statistics")]
    (js-navigate-to url)))

(defn content [state]
 (let [visible-content (cursor state [:visible])]
  [:div
   [m/modal-window]
   [:div
    [:div.btn-toolbar.pull-right {:style {:margin-bottom "20px"}}
     [:a.btn.btn-primary.btn-bulky
      {:href "#"
       :on-click #(if (= "text" @visible-content) (reset! visible-content "graphic") (reset! visible-content "text"))}
      (if (= "graphic" @visible-content) (t :admin/Plaintext) (t :admin/Showgraphicalui))]
     [:a.btn.btn-primary.btn-bulky
      {:href "#"
       :on-click #(export-stats state)}
      (t :admin/ExportCSV)]]
    [:div.row
     [:div.col-md-12
      (if (= "text" @visible-content)
         [text-content state]
         [graphic-content state])]]]]))


(defn init-data [state]
  (ajax/GET
   (path-for "/obpv1/admin/stats")
   {:handler (fn [data]
               (reset! state (assoc data :visible "graphic")))}))

(defn handler [site-navi]
  (let [state (atom {:register-users nil
                     :last-month-active-users nil
                     :last-month-registered-users nil
                     :all-badges nil
                     :last-month-added-badges nil
                     :pages nil
                     :visible "graphic"})]
    (init-data state)
    (fn []
      (layout/default site-navi (content state)))))
