(ns salava.badge.ui.my
  (:require
    [reagent.core :refer [atom]]
    [reagent.session :as session]
    [reagent-modals.modals :as m]
    [clojure.set :as set :refer [intersection]]
    [clojure.string :refer [upper-case]]
    [salava.core.ui.ajax-utils :as ajax]
    [salava.core.ui.helper :as h :refer [unique-values navigate-to path-for  not-activated? private?]]
    [salava.core.ui.notactivated :refer [not-activated-banner]]
    [salava.core.ui.layout :as layout]
    [salava.core.ui.grid :as g]
    [salava.badge.ui.helper :as bh]
    [salava.core.helper :refer [dump]]
    ;[salava.extra.application.ui.helper :refer [application-plugin?]]
    [salava.core.time :refer [unix-time date-from-unix-time]]
    [salava.core.i18n :as i18n :refer [t]]
    [salava.core.ui.badge-grid :refer [badge-grid-element]]))


(defn init-data [state]
  (ajax/GET
    (path-for "/obpv1/badge" true)
    {:handler (fn [data]
                (swap! state assoc :badges (filter #(= "accepted" (:status %)) data)
                                   :pending () ;(filter #(= "pending" (:status %)) data)
                                   :initializing false))}))

(defn visibility-select-values []
  [{:value "all" :title (t :core/All)}
   {:value "public"  :title (t :core/Public)}
   {:value "internal"  :title (t :core/Registeredusers)}
   {:value "private" :title (t :core/Onlyyou)}])

(defn order-radio-values []
  [{:value "mtime" :id "radio-date" :label (t :core/bydate)}
   {:value "name" :id "radio-name" :label (t :core/byname)}
   {:value "issuer_content_name" :id "radio-issuer" :label (t :core/byissuername)}
   {:value "expires_on" :id "radio-expiratio" :label (t :core/byexpirationdate)}])

(defn badge-grid-form [state]
  [:div {:id "grid-filter"
         :class "form-horizontal"}
   [g/grid-search-field (t :core/Search ":")  "badgesearch" (t :core/Searchbyname) :search state]
   [g/grid-select (t :core/Show ":")  "select-visibility" :visibility (visibility-select-values) state]
   [g/grid-buttons (t :core/Tags ":")  (unique-values :tags (:badges @state)) :tags-selected :tags-all state]
   [g/grid-radio-buttons (t :core/Order ":")  "order" (order-radio-values) :order state]])

(defn badge-visible? [element state]
  (if (and
        (or (= (:visibility @state) "all")
            (= (:visibility @state) (:visibility element)))
        (or (> (count
                 (intersection
                   (into #{} (:tags-selected @state))
                   (into #{} (:tags element))))
               0)
            (= (:tags-all @state)
               true))
        (or (empty? (:search @state))
            (not= (.indexOf
                    (.toLowerCase (:name element))
                    (.toLowerCase (:search @state)))
                  -1)))
    true false))


(defn badge-grid [state]
  (let [badges (:badges @state)
        order (keyword (:order @state))
        badges (case order
                 (:mtime) (sort-by order > badges)
                 (:name :issuer_content_name) (sort-by (comp clojure.string/upper-case str order) badges)
                 (:expires_on) (->> badges
                                    (sort-by order)
                                    (partition-by #(nil? (% order)))
                                    reverse
                                    flatten)
                 badges)]
    (into [:div#grid {:class "row wrap-grid"}
           (when-not (private?)
             [:div#import-badge {:key   "new-badge"}
            [:a.add-element-link {:href  "#" :on-click #(navigate-to "badge/import")}
             [:div {:class "media grid-container"}
              [:div.media-content
               [:div.media-body
                [:div {:id "add-element-icon"}
                 [:i {:class "fa fa-plus"}]]
                [:div {:id "add-element-link"}
                 (t :badge/Import)]]]]]])]
          (doall
            (for [element-data badges]
              (if (badge-visible? element-data state)
                (badge-grid-element element-data state "basic" init-data)))))))


(defn no-badges-text []
  [:div
   #_(if (application-plugin?)  [:div (t :badge/Youhavenobadgesyet) (str ". ") (t :social/Getyourfirstbadge) [:a {:href (path-for "/gallery/application") } (str " ") (t :badge/Gohere)] (str ".")] [:div (t :badge/Youhavenobadgesyet) (str ".")]) ] )



(defn content [state]
  [:div {:id "my-badges"}
   [m/modal-window]
   (if (:initializing @state)
     [:div.ajax-message
      [:i {:class "fa fa-cog fa-spin fa-2x "}]
      [:span (str (t :core/Loading) "...")]]
     [:div
      [badge-grid-form state]
      (cond
        (not-activated?) (not-activated-banner)
        (empty? (:badges @state)) [no-badges-text]
        :else [badge-grid state])]
     )])



(defn handler [site-navi]
  (let [state (atom {:badges []
                     :pending []
                     :visibility "all"
                     :order "mtime"
                     :tags-all true
                     :tags-selected []
                     :initializing true})]
    (init-data state)
    (fn []
      (layout/default site-navi (content state)))))
