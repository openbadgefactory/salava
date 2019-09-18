(ns salava.metabadge.ui.my
  (:require [salava.core.ui.layout :as layout]
            [salava.core.ui.ajax-utils :as ajax]
            [reagent.core :refer [atom cursor]]
            [salava.core.ui.helper :refer [path-for not-activated?]]
            [reagent-modals.modals :as m]
            [salava.core.i18n :as i18n :refer [t]]
            [salava.core.ui.notactivated :refer [not-activated-banner]]
            [salava.core.ui.modal :as mo]
            [salava.core.ui.grid :as g]
            [reagent.session :as session]
            [salava.metabadge.ui.metabadge :as mb]))

(defn init-data [state]
 (ajax/GET
  (path-for "/obpv1/metabadge" true)
  {:handler (fn [data]
              (swap! state assoc :metabadges data
                     :initializing false))}))

(defn %completed [req gotten]
 (Math/round (double (* (/ gotten req) 100))))

(defn completed? [req gotten]
 (>= gotten req))

(defn metabadge-element [milestone state]
 (let [{:keys [name min_required image_file criteria completion_status user_badge_id]} milestone
       image-class (if (or user_badge_id (= 100 completion_status)) "" " opaque")
       is-completed? (or user_badge_id (= completion_status 100))]
  [:div.media.grid-container
   [:div.media-content
    [:a {:href "#" :on-click #(mo/open-modal [:metabadge :metadata] milestone)}
     (if image_file
       [:div.media-left {:class image-class}
        [:img.badge-img {:src (str "/" image_file) :class (mb/image-class completion_status)}]])
     [:div.media-body
      [:div.media-heading
       [:p.heading-link name]]
      (when-not is-completed?
       [:div.progress
        [:div.progress-bar.progress-bar-success
         { :role "progressbar"
           :aria-valuenow (str completion_status)
           :style {:width (str completion_status "%")}
           :aria-valuemin "0"
           :aria-valuemax "100"}
         (str completion_status "%")]])]]]]))

(defn order-radio-values []
 [{:value "name" :id "radio-name" :label (t :core/byname)}
  {:value "status" :id "radio-status" :label (t :metabadge/bystatus)}])

(defn grid-form [state]
 (let [show-atom (cursor state [:show])]
  [:div#grid-filter {:class "form-horizontal" :style {:margin "10px 0px 10px 0px"}}
   [:label.checkbox-inline
    [:input.form-check-input {:type "checkbox"
                              :id "checkbox1"
                              :on-change #(do (if (= :in-progress @show-atom)
                                                (reset! show-atom :all)
                                                (reset! show-atom :in-progress)))}]
    (t :metabadge/Showcompletedmilestones)]]))

(defn metabadge-grid [state]
 (let [order (keyword (:order @state))
       in-progress (-> @state :metabadges :in_progress)
       completed (-> @state :metabadges :completed)
       show (cursor state [:show])
       metabadges (case @show
                    :in-progress in-progress
                    :all (flatten (conj in-progress completed))
                    (flatten (conj in-progress completed)))]
   (if (= 0 (count metabadges))
    [:div {:style {:margin-top "50px"}} (t :metabadge/Nonewgoals)]
    (reduce (fn [r m]
              (let [{:keys [required_badges min_required completion_status]} m
                    is-complete? (>= completion_status 100)]
                (conj r [metabadge-element m state])))
            [:div#grid {:class "row wrap-grid"}] (sort-by :completion_status > metabadges)))))

(defn content [state]
 (let [show-atom (cursor state [:show])]
  [:div {:id "my-badges"}
   [m/modal-window]
   (if (:initializing @state)
    [:div.ajax-message
     [:i {:class "fa fa-cog fa-spin fa-2x "}]
     [:span (str (t :core/Loading) "...")]]
    [:div
     [:div
      [:h1.uppercase-header (t :metabadge/Milestonebadges)]
      (str (t :metabadge/Aboutmilestonebadge) " " (t :metabadge/Milestonebadgespageinfo))]
     (if-not (empty? (:metabadges @state)) [grid-form state])
     (cond
       (not-activated?) (not-activated-banner)
       (empty? (-> @state :metabadges)) [:div {:style {:margin-top "50px"}} (t :metabadge/Nonewgoals)]
       :else [metabadge-grid state])])]))

(defn handler [site-navi]
 (let [state (atom {:initializing true
                    :show :in-progress})]
  (init-data state)
  (fn []
    (layout/default site-navi (content state)))))
