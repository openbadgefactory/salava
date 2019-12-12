(ns salava.metabadge.ui.modal
  (:require [reagent.core :refer [atom]]
            [salava.core.ui.ajax-utils :as ajax]
            [salava.core.ui.helper :refer [path-for url?]]
            [reagent.session :as session]
            [clojure.string :refer [split]]
            [salava.core.i18n :refer [t]]
            [salava.metabadge.ui.metabadge :as mb]
            [salava.core.ui.modal :as mo]
            [salava.core.time :refer [date-from-unix-time unix-time]]
            [salava.core.ui.error :as err]
            [salava.badge.ui.modal :refer []]
            [reagent.session :as session]))

(defn current-badge [metabadge milestone? current-badge-id]
 (if milestone? (->> metabadge first) (->> metabadge first :required_badges (filter #(or (= current-badge-id (:user_badge_id %)) (= current-badge-id (:url %)))) first)))

(defn %completed [req gotten]
 (Math/round (double (* (/ gotten req) 100))))

(defn completed? [req gotten]
 (>= gotten req))

(defn init-user-badge [data-atom]
 (ajax/GET
   (path-for (str "/obpv1/badge/info/" (:id @data-atom)))
   {:handler (fn [data]
               (reset! data-atom data))}))

(defn change-declined-badge-status [id]
 (ajax/POST
   (path-for (str "/obpv1/metabadge/update_status/" id))
   {:handler (fn []
               (mo/open-modal [:badge :info] {:badge-id id}))
    :error-handler (fn [{:keys [status status-text]}])}))

(defn application-link [url not_before not_after]
 (when (and
         (not (clojure.string/blank? url))
         (url? url)
         (or (= 0 not_before) (< not_before (unix-time)))
         (or (= 0 not_after) (> not_after (unix-time))))
  [:div [:a {:href url :target "_blank"} [:i.apply-now-icon {:class "fa fa-angle-double-right"}]  (t :extra-application/Getthisbadge)]]))

(defn dummy-badge [badge]
 (fn []
  (let [{:keys [image_file image name description criteria criteria_content application_url not_after not_before]} badge]
   [:div#metabadgegrid {:class "row flip-table"}
    [:div.col-md-3.badge-image
     [:div.image-container
      [:div.opaque
       [:img {:src (if image_file (str "/" image_file) image) :title name}]]
      [application-link application_url not_before not_after]]]
    [:div.col-md-9
     [:div.col-md-12
      [:h1.uppercase-header name]
      [:div.description description]
      [:div {:class "row criteria-html"}
       [:div.col-md-12
        [:h2.uppercase-header (t :badge/Criteria)]
        [:div {:dangerouslySetInnerHTML {:__html (or criteria criteria_content)}}]]]]]])))

(defn declined-badge [badge]
 (let [data-atom (atom {:id (-> badge :user_badge_id)})]
  (init-user-badge data-atom)
  (fn []
   (let [{:keys [image_file name]} badge
         {:keys [owner? user-logged-in?]} @data-atom]
    (if (and user-logged-in? owner?)
     [:div#metabadgegrid {:class "row flip-table"}
      [:div.col-md-3.badge-image
       [:div.image-container
        [:img {:src (str "/" image_file) :title name}]]]
      [:div.col-md-9
       [:div.col-md-12
        [:h1.uppercase-header name]
        [:div.footer
         (t :metabadge/Declinedbadgeinfo)
         [:hr]
         [:div
          [:button {:class "btn btn-primary"
                    :on-click #(change-declined-badge-status (:id @data-atom))}
           (t :core/Yes)]
          [:button {:class "btn btn-warning"
                    :data-dismiss "modal"}
           (t :core/No)]]]]]]
     (err/error-content))))))

(defn view-content [metabadge]
 (let [{:keys [image_file image description required_badges milestone? min_required completion_status criteria criteria_content user_badge_id name]} metabadge
       amount_received (count (filter #(pos? (:user_badge_id %)) required_badges))
       is-complete? (or user_badge_id (= 100 completion_status))]
  [:div.row.flip-table ;{:id "metabadgegrid"}
   [:div.col-md-3.badge-image
    [:div.image-container
     [:div {:class (when-not is-complete? "opaque")}
      [:img {:src (if image_file (str "/" image_file) image) :class (mb/image-class completion_status) :alt (str "Metabadge " name)}]]]
    [:div.progress {:alt (str completion_status "%") :title (str completion_status "%")}
     [:div.progress-bar.progress-bar-success
      {:role "progressbar"
       :aria-valuenow (str completion_status)
       :style {:width (str completion_status "%")}
       :aria-valuemin "0"
       :aria-valuemax "100"
       :alt (str completion_status "%")
       :title (str completion_status "%")}
      (str completion_status "%")]]]
   [:div.col-md-9
    [:div.col-md-12
     [:h1.uppercase-header (:name metabadge)]
     [:div {:style {:margin-top "10px"}}[:label (str (t :metabadge/Minimumrequired) ": ")] min_required]
     [:div [:label (str (t :metabadge/Amountearned)": ")] amount_received]
     [:div.description description]
     [:div {:class "row criteria-html"}
      [:div.col-md-12
       [:h2.uppercase-header (t :badge/Criteria)]
       [:div {:dangerouslySetInnerHTML {:__html (or criteria criteria_content)}}]]]]]]))

(defn required-badge-tab [metabadge]
 (let [{:keys [image_file image required_badges milestone? min_required completion_status user_badge_id name]} metabadge
       amount_received (count (filter #(pos? (:user_badge_id %)) required_badges))
       is-complete? (or (pos? user_badge_id) (= 100 completion_status))]
  [:div.row.flip-table
   [:div.col-md-3.badge-image
    [:div.image-container
     [:div {:class (if-not is-complete? "opaque")}
      [:img {:src (if image_file (str "/" image_file) image) :class (mb/image-class completion_status) :alt (str "Metabadge " name)}]]]
    [:div.progress {:alt (str completion_status "%") :title (str completion_status "%")}
     [:div.progress-bar.progress-bar-success
      {:role "progressbar"
        :aria-valuenow (str completion_status)
        :style {:width (str completion_status "%")}
        :aria-valuemin "0"
        :aria-valuemax "100"
        :alt (str completion_status "%")
        :title (str completion_status "%")}
      (str completion_status "%")]]]
   [:div.col-md-9 {:id "badge-stats"}
    [:div.col-md-12
     [:div.description (if-not is-complete? (t :metabadge/Inprogressmilestoneinfo) (t :metabadge/Completedmilestoneinfo))]

     [:div.panel
      [:hr.line {:style {:margin-top "50px"}}] ;{:style {:margin-top "50px" :border-style "dotted"}}]
      [:div.panel-body
       [:div.row.header {:style {:margin-bottom "10px" :margin-top "10px"}}
        [:div.row.flip-table
         [:div.col-md-6]
         [:div.col-md-2 (t :metabadge/Earnedon)]
         [:div.col-md-4]]]
       (reduce (fn [r badge]
                (let [{:keys [name image_file image issued_on user_badge_id status deleted application_url not_after not_before]} badge
                      image-class (if-not (pos? user_badge_id) " opaque")]
                  (conj r [:div
                           [:div.row.flip-table {:style {:margin-bottom "10px"}}
                            [:div.col-md-1 [:img.badge-icon {:src (if image_file (str "/" image_file) image) :class image-class :alt (str (t :badge/Badge) " " name)}]]
                            [:div.col-md-5 (if (pos? user_badge_id)
                                             (if  (empty? (-> (session/get :user) (dissoc :pending))) ;; disable received badge link when user is not logged in
                                               name
                                               (if (= status "declined")
                                                 [:a {:href "#" :on-click #(mo/open-modal [:metabadge :declined] badge)} name]
                                                 (if (pos? deleted) name [:a {:href "#" :on-click #(mo/open-modal [:badge :info] {:badge-id user_badge_id})} name])))
                                             [:a {:href "#" :on-click #(mo/open-modal [:metabadge :dummy] badge)} name])]

                            [:div.col-md-2 [:label.hidden-label (t :metabadge/Earnedon)] (if issued_on (date-from-unix-time (* 1000 issued_on)) "-")]
                            [:div.col-md-4 ]]]))) [:div.row.body] (->> required_badges
                                                                       (sort-by #(-> % :issued_on) >)
                                                                       #_(sort-by :received >)))]]]]]))

(defn modal-navi [metabadge state]
 [:div.row.flip-table
  [:div.col-md-3]
  [:div.col-md-9.badge-modal-navi
   [:ul {:class "nav nav-tabs wrap-grid"}
    [:li.nav-item {:class  (if (or (nil? (:tab-no @state))(= 1 (:tab-no @state))) "active")}
     [:a.nav-link {:href "#" :on-click #(swap! state assoc :tab [required-badge-tab metabadge] :tab-no 1)}
      [:div  [:i.nav-icon {:class "fa fa-puzzle-piece fa-lg"}] (t :metabadge/Requiredbadges)]]]
    [:li.nav-item {:class  (if (or (nil? (:tab-no @state))(= 2 (:tab-no @state))) "active")}
     [:a.nav-link {:href "#" :on-click #(swap! state assoc :tab [view-content metabadge] :tab-no 2)}
      [:div  [:i.nav-icon {:class "fa fa-info-circle fa-lg"}] (t :metabadge/Info)]]]]]])

(defn metabadge-content [metabadge state]
 (let [{:keys [badge required_badges milestonefirst? min_required]} metabadge
       amount_received (count (filter :received required_badges))
       completed-percentage (%completed min_required amount_received)
       completed (if (> completed-percentage 100) 100 (str completed-percentage))
       is-complete? (completed? min_required amount_received)]
   [:div#metabadgegrid {:class "row"}
    [modal-navi metabadge state]
    (if (:tab @state) (:tab @state) [required-badge-tab metabadge])]))

(defn multi-block [metabadge]
 (fn []
  (let [current (current-badge (:metabadge metabadge) (:milestone? metabadge) (:current metabadge))]
   [:div#metabadgegrid {:class "row flip-table"}
    [:div.col-md-3
     [:div.badge-image
      [:img {:src (str "/" (:image_file current)) :alt (str (t :badge/Badge) " " (:name current))}]]]
    [:div.col-md-9
     [:div.row
      [:div.col-md-12
       [:h1.uppercase-header (:heading metabadge)]
       [:div.info (str (t :metabadge/Aboutmilestonebadge)" " (t :metabadge/Metabadgeblocksinfo))]
       (reduce (fn [r m] (conj r ^{:key m}[mb/badge-block m (:current metabadge) (:milestone? metabadge)]))  [:div#metabadge {:class "row wrap-grid"}] (:metabadge metabadge))]]]])))

(defn handler [metabadge]
  (let [state (atom {:tab-no 1})]
    (fn [] (metabadge-content metabadge state))))

(def ^:export modalroutes
  {:metabadge {:metadata handler
               :multiblock multi-block
               :dummy dummy-badge
               :declined declined-badge}})
