(ns salava.badge.ui.block
 (:require
  [clojure.string :refer [upper-case]]
  [clojure.set :as set :refer [intersection]]
  [reagent.core :refer [create-class atom cursor]]
  [salava.badge.ui.endorsement :refer [endorsement-list request-endorsement]]
  [salava.badge.ui.evidence :refer [evidence-list-badge-view evidenceblock]]
  [salava.badge.ui.helper :as bh]
  [salava.badge.ui.settings :refer [settings-tab-content]]
  [salava.core.ui.ajax-utils :as ajax]
  [salava.core.ui.helper :refer [path-for unique-values]]
  [salava.core.ui.grid :as g]
  [salava.core.i18n :as i18n :refer [t]]
  [salava.core.ui.badge-grid :refer [badge-grid-element]]
  [salava.core.ui.popover :refer [info]]
  [salava.user.ui.helper :refer [profile-link-inline-modal]]))

(defn init-data
  ([state]
   (ajax/GET
     (path-for "/obpv1/badge" true)
     {:handler (fn [data]
                 (swap! state assoc :badges (filter #(= "accepted" (:status %)) (:badges data))
                        :pending (filter #(= "pending" (:status %)) (:badges data))
                        :initializing false))})))

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
  [:form {:id "grid-filter"
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
    (create-class {:reagent-render (fn []
                                    (let [badges (remove #(or (true? (bh/badge-expired? (:expires_on %))) (:revoked %)) (:badges @state))
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
                                           (if-not (empty? badges)
                                            [:div
                                               (into [:div#grid {:class "row"}]
                                                     (doall
                                                       (for [element-data badges]
                                                         (when (badge-visible? element-data state)
                                                           (swap! state assoc  :new-field-atom new-field-atom :block-atom block-atom :index (:index param) :function func)
                                                          (badge-grid-element element-data state badge-type init-data)))))]
                                            [:div {:style {:font-size "16px"}} [:p [:b (t :badge/Youhavenobadgesyet)]]])])]]))
                   :component-will-mount (fn [] (init-data state))})))

(defn visibilityform [vatom]
 [:div.row
  [:div.col-md-12
   [:div.panel.panel-default
    [:div.panel-heading {:style {:padding "8px"}}
     [:div.panel-title {:style {:margin-bottom "unset" :font-size "16px"}}
      (t :badge/Setbadgevisibility) [info {:style {:position "absolute" :right "0" :top "0"} :content (t :badge/Visibilityinfo) :placement "left"}]]]
    [:div.panel-body {:style {:padding "15px"}}
     [:div.visibility-opts-group
      [:div.visibility-opt
        [:input.radio-btn {:id "private"
                           :type "radio"
                           :name "private"
                           :on-change #(do
                                         (.preventDefault %)
                                         (reset! vatom "private"))
                           :checked (= "private" @vatom)}]
        [:div.radio-tile
         [:div.icon [:i.fa.fa-lock.fa-4x]]
         [:label.radio-tile-label {:for "private"} (t :badge/Private)]]]
      [:div.visibility-opt
        [:input.radio-btn {:id "internal"
                           :type "radio"
                           :name "internal"
                           :on-change #(do
                                         (.preventDefault %)
                                         (reset! vatom "internal"))
                           :checked (= "internal" @vatom)}]
        [:div.radio-tile
         [:div.icon [:i.fa.fa-group.fa-3x]]
         [:label.radio-tile-label {:for "internal"} (t :badge/Shared)]]]
      [:div.visibility-opt
        [:input.radio-btn {:id "public"
                           :type "radio"
                           :name "public"
                           :on-change #(do
                                         (.preventDefault %)
                                         (reset! vatom "public"))
                           :checked (= "public" @vatom)}]
        [:div.radio-tile
         [:div.icon [:i.fa.fa-globe.fa-3x]]
         [:label.radio-tile-label {:for "public"} (t :badge/Public)]]]]]]]
  #_[:div.col-md-12
     [:hr.border.dotted-border]]])

(defn ^:export badge_endorsements [id data]
 [endorsement-list id data])

(defn ^:export request_endorsement [state]
  [request-endorsement state])

(defn ^:export settings_tab_content [data state init-data]
  [settings-tab-content data state init-data])

(defn ^:export evidence_list_badge [id]
  [evidence-list-badge-view id])

(defn ^:export evidence_block [data state init-data]
  [evidenceblock data state init-data])

(defn ^:export badge_visibility_form [vatom]
  [visibilityform vatom])
