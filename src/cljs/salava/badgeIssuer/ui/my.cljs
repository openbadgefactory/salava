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
                      :order "mtime"
                      :alert (session/get! :issue-event)))}))

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
  (let [{:keys [id name image creator_name issuable_from_gallery]} element-data
        selected-users (cursor state [:selected-users])]
    [:div.media.grid-container
     [:div.media-content
      #_(when issuable_from_gallery
          [:span.inline-block.pull-right {:title (t :badgeIssuer/Issuableselfiebadge) :aria-label (t :badgeIssuer/Issuableselfiebadge)} [:i.fa.fa-paper-plane]])
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
         [:p creator_name]]]]]]))

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

(defn issue-alert [state]
  (let [alert @(cursor state [:alert])
        {:keys [badge recipient_count]} alert]
   (.scrollTo js/window 0 0)
   [:div#badge-creator.alert.alert-success.alert-dismissable
    [:button.close
     {:type "button"
      :data-dismiss "alert"
      :aria-label (t :core/Close)}
     [:span {:aria-hidden "true" :dangerouslySetInnerHTML {:__html "&times;"}}]]
    [:div
     [:p (str (t :badgeIssuer/Badgesuccessfullyissuedto) " " recipient_count)]]]))

(defn content [state]
  [:div#selfie-badges.my-selfie-badges
   [m/modal-window]
   (if (:initializing @state)
     [:div.ajax-message
      [:i {:class "fa fa-cog fa-spin fa-2x "}]
      [:span (str (t :core/Loading) "...")]]
     [:div
      (when @(cursor state [:alert]) (issue-alert state))
      [grid-form state]
      (if (not-activated?)
        [not-activated-banner]
        [grid state])])])

(defn handler [site-navi]
  (let [state (atom {:badges []
                     :initializing false
                     :tags-all true
                     :tags-selected []
                     :alert nil})]
    (init-data state)
    (fn []
      (layout/default site-navi [content state]))))
