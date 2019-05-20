(ns salava.badge.ui.block
 (:require [salava.core.ui.ajax-utils :as ajax]
           [salava.core.ui.helper :refer [path-for unique-values]]
           [reagent.core :refer [create-class atom cursor]]
           [salava.badge.ui.helper :as bh]
           [salava.core.ui.grid :as g]
           [clojure.string :refer [upper-case]]
           [salava.core.i18n :as i18n :refer [t]]
           [clojure.set :as set :refer [intersection]]
           [salava.core.ui.badge-grid :refer [badge-grid-element]]))

(defn init-data
  ([state]
   (ajax/GET
     (path-for "/obpv1/badge" true)
     {:handler (fn [data]
                 (swap! state assoc :badges (filter #(= "accepted" (:status %)) data)
                        :pending (filter #(= "pending" (:status %)) data)
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
