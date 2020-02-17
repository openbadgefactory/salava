(ns salava.badgeIssuer.ui.my
  (:require
   [clojure.set :refer [intersection]]
   [clojure.string :refer [blank? upper-case]]
   [reagent.core :refer [atom create-class cursor]]
   [reagent-modals.modals :as m]
   [reagent.session :as session]
   [salava.core.ui.ajax-utils :as ajax]
   [salava.core.ui.helper :refer [path-for navigate-to private? unique-values not-activated?]]
   [salava.core.ui.grid :as g]
   [salava.core.ui.notactivated :refer [not-activated-banner]]
   [salava.core.i18n :refer [t translate-text]]
   [salava.core.ui.layout :as layout]
   [salava.core.ui.modal :as mo]))

(defn init-data [state]
  (ajax/GET
   (path-for "/obpv1/selfie" true)
   {:handler (fn [data]
               (swap! state assoc :badges (:badges data)
                      :initializing false
                      :order "mtime"))}))

(defn element-visible? [element state]
  (if (and
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

(defn order-radio-values []
  [{:value "mtime" :id "radio-date" :label (t :core/bydate)}
   {:value "name" :id "radio-name" :label (t :core/byname)}])

(defn grid-form [state]
  [:div {:id "grid-filter"
         :class "form-horizontal"}
   [g/grid-search-field (t :core/Search ":")  "badgesearch" (t :core/Searchbyname) :search state]
   [g/grid-buttons (t :core/Tags ":")  (unique-values :tags (:badges @state)) :tags-selected :tags-all state]
   [g/grid-radio-buttons (t :core/Order ":")  "order" (order-radio-values) :order state]])

(defn grid-element [element-data state]
  (let [{:keys [id name image creator_name]} element-data
        selected-users (cursor state [:selected-users])]
    [:div.media.grid-container
     [:div.media-content
      [:a {:href "#"
           :on-click #(do
                        (.preventDefault %)
                        (mo/open-modal [:selfie :view] {:badge element-data} {:hidden (fn [] (init-data state))}))}
       [:div.media-left
        [:img.badge-img {:src (str "/" image) :alt ""}]]
       [:div.media-body
        [:div.media-heading
         [:p.heading-link name]]
        [:div.media-issuer
         [:p creator_name]]]]
      #_[:div
         [:div.pull-right
          [:a {:href "#"
               :on-click #(navigate-to (str "/badge/selfie/create/" id))
               :aria-label (t :badgeIssuer/Editbadge)
               :title (t :badgeIssuer/Editbadge)}
           [:i.fa.fa-edit.fa-lg]]]
         [:div.pull-left
          [:a {:href "#"
               :on-click #(mo/open-modal [:selfie :issue] {:badge element-data})
               :aria-label (t :badge/Issuebadge)
               :title (t :badge/Issuebadge)}
           [:i.fa.fa-edit.fa-paper-plane]]]]]]))

(defn grid [state]
  (let [badges @(cursor state [:badges])
        order (keyword (:order @state))
        badges (case order
                 (:name) (sort-by (comp upper-case str order) badges)
                 (:mtime) (sort-by order > badges)
                 (sort-by order > badges))]
    (into [:div#grid {:class "row wrap-grid"}
           (when-not (private?)
             [:div#import-badge {:key   "new-badge" :style {:position "relative"}}
              [:a.add-element-link {:href (path-for (str "/badge/selfie/create"))}
               [:div {:class "media grid-container"}
                [:div.media-content
                 [:div.media-body
                  [:div {:id "add-element-icon"}
                   [:i {:class "fa fa-plus"}]]
                  [:div {:id "add-element-link"}
                   (t :badgeIssuer/New)]]]]]])]
          (doall
           (for [element-data badges]
             (if (element-visible? element-data state)
               (grid-element element-data state)))))))

(defn content [state]
  [:div#selfie-badges.my-selfie-badges
   [m/modal-window]
   (if (:initializing @state)
     [:div.ajax-message
      [:i {:class "fa fa-cog fa-spin fa-2x "}]
      [:span (str (t :core/Loading) "...")]]
     (if (not-activated?)
       [not-activated-banner]
       [:div
        [grid-form state]
        [grid state]]))])

(defn handler [site-navi]
  (let [state (atom {:badges []
                     :initializing false
                     :tags-all true
                     :tags-selected []})]
    (init-data state)
    (fn []
      (layout/default site-navi [content state]))))
