(ns salava.badge.ui.my
  (:require
    [reagent.core :refer [atom create-class]]
    [reagent.session :as session]
    [reagent-modals.modals :as m]
    [clojure.set :as set :refer [intersection]]
    [clojure.string :refer [upper-case]]
    [salava.core.ui.ajax-utils :as ajax]
    [salava.core.ui.helper :as h :refer [unique-values navigate-to path-for  not-activated? private? js-navigate-to current-path current-route-path plugin-fun]]
    [salava.core.ui.notactivated :refer [not-activated-banner]]
    [salava.core.ui.layout :as layout]
    [salava.core.ui.grid :as g]
    [salava.badge.ui.helper :as bh]
    [salava.core.helper :refer [dump]]
    ;[salava.extra.application.ui.helper :refer [application-plugin?]]
    [salava.core.time :refer [unix-time date-from-unix-time]]
    [salava.core.i18n :as i18n :refer [t]]
    [salava.core.ui.badge-grid :refer [badge-grid-element]]
    [clojure.walk :refer [keywordize-keys]]
    [cemerick.url :as url]
    [salava.core.ui.modal :as mo]
    #_[salava.badge.ui.pending :refer [badge-pending badges-pending badge-alert]]))


(defn init-data
  ([state]
   (ajax/GET
     (path-for "/obpv1/badge" true)
     {:handler (fn [data]
                 (swap! state assoc :badges (filter #(= "accepted" (:status %)) data)
                        :pending (filter #(= "pending" (:status %)) data)
                        :initializing false))})
   #_(ajax/GET
       (path-for "/obpv1/social/pending_badges" true)
       {:handler (fn [data]
                   (swap! state assoc :spinner false :pending-badges (:pending-badges data)))})))

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


(defn import-button-description []
  (let [block (first (plugin-fun (session/get :plugins) "block" "importbadgetext"))
        user-lang (session/get-in [:user :language] "en")]
    (if block [block user-lang] [:div ""])))

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
             [:div#import-badge {:key   "new-badge" :style {:position "relative"}}
              [:a.add-element-link {:href  (path-for "/badge/import") }
               [:div {:class "media grid-container"}
                [:div.media-content
                 [:div.media-body
                  [:div {:id "add-element-icon"}
                   [:i {:class "fa fa-plus"}]]
                  [:div {:id "add-element-link"}
                   (t :badge/Import)]
                  [import-button-description]]]]]])]
          (doall
            (for [element-data badges]
              (if (badge-visible? element-data state)
                (badge-grid-element element-data state "basic" init-data)))))))


(defn no-badges-text []
  [:div
   #_(if (application-plugin?)  [:div (t :badge/Youhavenobadgesyet) (str ". ") (t :social/Getyourfirstbadge) [:a {:href (path-for "/gallery/application") } (str " ") (t :badge/Gohere)] (str ".")] [:div (t :badge/Youhavenobadgesyet) (str ".")]) ] )

(defn open-modal [id state]
  (ajax/GET
    (path-for (str "/obpv1/badge/info/" id))
    {:handler (fn [data] (mo/open-modal [:badge :info] {:badge-id id :data data} {:hidden (fn []
                                                                                            (do
                                                                                              (if (clojure.string/includes? (str js/window.location.href) (path-for (str "/badge?id=" id)))
                                                                                                (.replaceState js/history {} "Badge modal" (path-for "/badge"))
                                                                                                (navigate-to (current-route-path)))
                                                                                              (init-data state))
                                                                                            )}))}))


(defn mybadgesmodal [param]
  (let [state (atom {:initializing true
                     :badges []
                     :visibility "all"
                     :order "mtime"
                     :tags-all true
                     :tags-selected []})
        badge-type (:type param)
        block-atom (:block-atom param)
        new-field-atom (:new-field-atom param)
        func (:function param)]
    (init-data state)
    (fn []
      (let [badges (remove #(true? (bh/badge-expired? (:expires_on %))) (:badges @state))
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
        [:div.row {:id "my-badges"}
         [:div.col-md-12
          (if (:initializing @state)
            [:div.ajax-message
             [:i {:class "fa fa-cog fa-spin fa-2x "}]
             [:span (str (t :core/Loading) "...")]]
            [:div
             [badge-grid-form state]
             [:div
              (into [:div#grid {:class "row"}]
                    (doall
                      (for [element-data badges]
                        (when (badge-visible? element-data state)
                          (swap! state assoc  :new-field-atom new-field-atom :block-atom block-atom :index (:index param) :function func)
                        (badge-grid-element element-data state badge-type init-data))))
                    )]])]]))))

(defn content [state]
  (create-class {:reagent-render (fn []
                                   (let [badges-pending-func (first (plugin-fun (session/get :plugins) "pending" "badges_pending"))
                                         badge-alert-func (first (plugin-fun (session/get :plugins) "pending" "badge_alert"))]
                                     [:div {:id "my-badges"}
                                         [m/modal-window]
                                         (if (:initializing @state)
                                           [:div.ajax-message
                                            [:i {:class "fa fa-cog fa-spin fa-2x "}]
                                            [:span (str (t :core/Loading) "...")]]
                                           [:div
                                            ;[badge-alert state]
                                            [badge-alert-func state]
                                            (if (seq (:pending @state)) [badges-pending-func state init-data] ;[badges-pending state init-data]
                                              [badge-grid-form state])
                                            (cond
                                              (not-activated?) (not-activated-banner)
                                              ;(empty? (:badges @state)) [no-badges-text]
                                              :else (when-not (seq (:pending @state)) [badge-grid state]))]
                                           )]))
                 :component-did-mount (fn [] (if (:init-id @state) (open-modal (:init-id @state) state)))}))


(defn init-id "take url params" []
  (if-let [id (-> (keywordize-keys (:query (url/url (-> js/window .-location .-href)))) :id)] id nil))

(defn handler [site-navi]
  (let [id (init-id)
        state (atom {:badges []
                     :pending []
                     :visibility "all"
                     :order "mtime"
                     :tags-all true
                     :tags-selected []
                     :initializing true
                     :init-id id
                     :badge-alert nil})]
    (init-data state)
    (fn []
      (layout/default site-navi [content state]))))

