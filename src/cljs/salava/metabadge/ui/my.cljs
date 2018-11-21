(ns salava.metabadge.ui.my
  (:require [salava.core.ui.layout :as layout]
            [salava.core.ui.ajax-utils :as ajax]
            [reagent.core :refer [atom]]
            [salava.core.ui.helper :refer [path-for not-activated?]]
            [reagent-modals.modals :as m]
            [salava.core.i18n :as i18n :refer [t]]
            [salava.core.ui.notactivated :refer [not-activated-banner]]
            [salava.core.ui.modal :as mo]
            [salava.core.ui.grid :as g]
            [reagent.session :as session]
            ))

(defn init-data [state]
  (ajax/GET
    (path-for "/obpv1/metabadge" true)
    {:handler (fn [data]
                ;(session/put :metabadges data)
                (swap! state assoc :metabadges data
                       :initializing false))}))

(defn %completed [req gotten]
  (Math/round (double (* (/ gotten req) 100))))

(defn completed? [req gotten]
  (>= gotten req))

(defn metabadge-element [milestone state]
  (let [{:keys [badge name min_required required_badges completion_status]} milestone
        completed (if (> completion_status 100) 100 completion_status)]
    [:div.media.grid-container
     [:div.media-content
      [:a {:href "#" :on-click #(mo/open-modal [:metabadge :metadata] milestone)}
       (if (:image badge)
         [:div.media-left
          [:img.badge-img.opaque {:src (:image badge)}]
          ])
       [:div.media-body
        [:div.media-heading
         [:p.heading-link name]]

        [:div.progress
         [:div.progress-bar.progress-bar-success.progress-bar-striped.active
          { :role "progressbar"
            :aria-valuenow (str completion_status)
            :style {:width (str completion_status "%")}
            :aria-valuemin "0"
            :aria-valuemax "100"}
          (str completion_status "%")]]]
       ]]]))

(defn order-radio-values []
  [{:value "name" :id "radio-name" :label (t :core/byname)}
   {:value "status" :id "radio-status" :label "by status"#_(t :core/byname)}])

(defn grid-form [state]
  [:div#grid-filter {:class "form-horizontal" :style {:margin "10px 0px 10px 0px"}}
   [g/grid-radio-buttons (t :core/Order ":") "order" (order-radio-values) :order state]])

(defn metabadge-grid [state]
  (let [metabadges (:metabadges @state)
        order (keyword (:order @state))
        metabadges (case order
                     (:name) (sort-by (comp clojure.string/upper-case str order) metabadges)
                     (:status)(sort-by :completion_status > metabadges)
                     metabadges)]
    (reduce (fn [r m]
              (let [{:keys [required_badges min_required completion_status]} m
                    is-complete? (>= completion_status 100)]
                (conj r (when-not is-complete? [metabadge-element m state]))
                )) [:div#grid {:class "row wrap-grid"}] metabadges)))


(defn content [state]
  [:div {:id "my-badges"}
   [m/modal-window]
   (if (:initializing @state)
     [:div.ajax-message
      [:i {:class "fa fa-cog fa-spin fa-2x "}]
      [:span (str (t :core/Loading) "...")]]
     [:div
      [:div
       [:h1.uppercase-header (t :metabadge/Mygoals)]
       (t :metabadge/Mygoalsinfo)]
      [grid-form state]
      (cond
        (not-activated?) (not-activated-banner)
        (empty? (:metabadges@state)) [:div]
        :else [metabadge-grid state])])])

(defn handler [site-navi]
  (let [state (atom {:initializing true
                     :order "status"})]
    (init-data state)
    (fn []
      (layout/default site-navi (content state)))))
