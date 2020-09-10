(ns salava.extra.spaces.ui.stats
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
           [salava.admin.ui.stats-helper :as dh]))

(def colors
 {:default "#82ca9d"
  :primary "#0275d8"
  :success "#5cb85c"
  :info "#5bc0de"
  :warning "#f0ad4e"
  :danger "#d9534f"
  :yellow "#FFC658"
  :purple "#8884D8"})

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
  (let [{:keys [issuers users userbadges pages created issued user-badge-correlation]}  @state
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
                                          :slices [{:name "Open Badge Factory " :value factorybadges :fill (:primary colors) :percentage (dh/%percentage factorybadges Totalbadgesno)}
                                                   {:name (session/get :site-name "Passport") :value Totalissuedno :fill (:default colors) :percentage (dh/%percentage Totalissuedno Totalbadgesno)}
                                                   {:name (t :admin/Other) :value (- Totalbadgesno (+ factorybadges Totalissuedno)) :fill (:yellow colors) :percentage (dh/%percentage (- Totalbadgesno (+ factorybadges Totalissuedno)) Totalbadgesno)}]}
                                         {:title (t :admin/badgeStatus)
                                          :slices [{:name (t :social/pending) :value pendingbadgescount :fill (:info colors) :percentage (dh/%percentage pendingbadgescount Totalbadgesno)}
                                                   {:name (t :social/accepted) :value acceptedbadgescount :fill (:default colors) :percentage (dh/%percentage acceptedbadgescount Totalbadgesno)}
                                                   {:name (t :social/declined) :value declinedbadgescount :fill (:danger colors) :percentage (dh/%percentage declinedbadgescount Totalbadgesno)}]}
                                         {:title (t :admin/userProfile)
                                          :slices [{:name (t :page/Public) :value publicusers :fill (:default colors) :percentage (dh/%percentage publicusers Totalusersno)}
                                                   {:name (t :admin/Notactivated) :value notactivatedusers :fill (:danger colors) :percentage (dh/%percentage notactivatedusers Totalusersno)}
                                                   {:name (t :core/Internal) :value internalusers :fill (:yellow colors) :percentage (dh/%percentage internalusers Totalusersno)}]}

                                         {:title (t :admin/badgeVisibility)
                                          :slices [{:name (t :page/Public) :value publicbadgescount :fill (:default colors) :percentage (dh/%percentage publicbadgescount Totalbadgesno)}
                                                   {:name (t :page/Private) :value privatebadgescount :fill (:purple colors) :percentage (dh/%percentage privatebadgescount Totalbadgesno)}
                                                   {:name (t :core/Internal) :value internalbadgescount :fill (:yellow colors) :percentage (dh/%percentage internalbadgescount Totalbadgesno)}]}
                                         {:title (t :admin/pageVisibility)
                                          :slices [{:name (t :page/Public) :value publicpagescount :fill (:default colors) :percentage (dh/%percentage publicpagescount Totalpagesno)}
                                                   {:name (t :page/Private) :value privatepagescount :fill (:purple colors) :percentage (dh/%percentage privatepagescount Totalpagesno)}
                                                   {:name (t :core/Internal) :value internalpagescount :fill (:yellow colors) :percentage (dh/%percentage internalpagescount Totalpagesno)}]}]}]]
      [dh/social-media-box state]
      [:div.row
       [dh/panel-box-chart {:size :lg
                            :icon "fa-line-chart"
                            :type "b-user"
                            :chart-type :line
                            :chart-data [{:info [{:name 12  :total  (- Totalusersno userssince1year)} ;:active-users (:1yearlogincount users)}
                                                 {:name 6  :total (- Totalusersno userssince6month)} ;:active-users (:6monthlogincount users)}
                                                 {:name 3  :total (- Totalusersno userssince3month)} ;:active-users (:3monthlogincount users)}
                                                 {:name 1  :total (- Totalusersno userssincelastmonth)} ;:active-users (:1monthlogincount users)}
                                                 {:name 0  :total Totalusersno}] ;:active-users logincountsincelastlogin}]
                                           :lines [{:name (t :admin/Totalusersno) :key "total" :stroke (:primary colors) :activeDot {:r 10} :strokeWidth 3}]
                                                   ;{:name (t :admin/Activeusers) :key "active-users" :stroke (:danger colors)}]
                                           :title (t :admin/userGrowth)
                                           :xlabel (t :admin/noofmonths)}
                                         {:info [{:name 12  :total (- Totalbadgesno badgessince1year)}
                                                 {:name 6  :total (- Totalbadgesno badgessince6month)}
                                                 {:name 3 :total (- Totalbadgesno badgessince3month)}
                                                 {:name 1  :total (- Totalbadgesno badgessincelastmonth)}
                                                 {:name 0  :total Totalbadgesno}]
                                          :lines [{:name (t :admin/Totalbadgesno) :key "total" :stroke (:primary colors) :activeDot {:r 10} :strokeWidth 3}]
                                          :title (t :admin/badgeGrowth)
                                          :xlabel (t :admin/noofmonths)}
                                         {:info [{:name 12  :total (- Totalpagesno pagessince1year)}
                                                 {:name 6  :total (- Totalpagesno pagessince6month)}
                                                 {:name 3  :total (- Totalpagesno pagessince3month)}
                                                 {:name 1  :total (- Totalpagesno pagessincelastmonth)}
                                                 {:name 0  :total Totalpagesno}]
                                          :lines [{:name (t :admin/Totalpagesno) :key "total" :stroke (:primary colors) :activeDot {:r 10} :strokeWidth 3}]
                                          :title (t :admin/pageGrowth)
                                          :xlabel (t :admin/noofmonths)}

                                         {:info [{:name 12 :total (- Totalissuersno issuerssince1year)}
                                                 {:name 6 :total (- Totalissuersno issuerssince6month)}
                                                 {:name 3 :total (- Totalissuersno issuerssince3month)}
                                                 {:name 1 :total (- Totalissuersno issuerssincelastmonth)}
                                                 {:name 0 :total Totalissuersno}]
                                          :lines [{:name (t :admin/Totalissuersno) :key "total" :stroke (:primary colors) :activeDot {:r 10} :strokeWidth 3}]
                                          :title (t :admin/issuerGrowth)
                                          :xlabel (t :admin/noofmonths)}]


                             :tooltipLabel (t :admin/monthsago)}]]
      [:div.row
       [dh/panel-box-chart {:size :lg
                            :icon "fa-bar-chart"
                            :type "b-page"
                            :chart-type :mixed
                            :chart-data [{:info (sort-by :badge_count < user-badge-correlation)
                                          :title (t :admin/userBadgeDistribution)
                                          :elements [{:legendType "none" :name (t :admin/users) :key "user_count" :fill (:warning colors) :stackId "a" :type "bar"}
                                                     {:legendType "none" :name (t :admin/users) :key "user_count" :type "line" :stroke (:primary colors) :activeDot {:r 8} :strokeWidth 3 :dot false}]
                                          :dataKeyX "badge_count"
                                          :dataKeyY "user_count"
                                          :xlabel (t :admin/noofbadges)
                                          :ylabel (t :admin/noofusers)}]
                            :tooltipLabel (t :admin/userbadges)}]]]]))

(defn export-stats [state]
  (let [url (str "/obpv1/space/export_statistics?id=" (session/get-in [:user :current-space :id]))]
    (js-navigate-to url)))

(defn content [state]
 (let [visible-content (cursor state [:visible])]
   [:div
    [m/modal-window]
    [:div
     [:div.row
      [:div.col-md-12
       [:div.btn-toolbar.pull-right {:style {:margin-bottom "20px"}}
        [:a.btn.btn-primary.btn-bulky
         {:href "#"
          :on-click #(if (= "text" @visible-content) (reset! visible-content "graphic") (reset! visible-content "text"))}
         (if (= "graphic" @visible-content) (t :admin/Plaintext) (t :admin/Showgraphicalui))]
        [:a.btn.btn-primary.btn-bulky
         {:href "#"
          :on-click #(export-stats state)}
         (t :admin/ExportCSV)]]]]
     [:div.row
      [:div.col-md-12
       (if (= "text" @visible-content)
          [text-content state]
          [graphic-content state])]]]]))

(defn handler [site-navi]
  (let [id (session/get-in [:user :current-space :id])
         t (atom {})]
   (ajax/POST
    (path-for (str "/obpv1/space/stats/" id))
    {:handler (fn [data]
                (reset! t (assoc data :visible "graphic")))})

   (fn []
     (layout/default site-navi [content t]))))
