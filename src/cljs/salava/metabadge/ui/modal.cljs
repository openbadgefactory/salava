(ns salava.metabadge.ui.modal
  (:require [reagent.core :refer [atom]]
            [salava.core.ui.ajax-utils :as ajax]
            [salava.core.ui.helper :refer [path-for]]
            [reagent.session :as session]
            [clojure.string :refer [split]]
            [salava.core.i18n :refer [t]]
            [salava.metabadge.ui.metabadge :as mb]
            [salava.core.ui.modal :as mo]
            [salava.core.time :refer [date-from-unix-time]]
            [salava.core.ui.error :as err]
            [salava.badge.ui.modal :refer []]))

(defn current-badge [metabadge]
  (let [{:keys [milestone? required_badges badge]} (-> metabadge first)]
    (if milestone? badge (->> required_badges (filter :current) first))))

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

(defn dummy-badge [badge]
  (fn []
    (let [{:keys [badge-info received current]} badge
          {:keys [image name description criteria]} badge-info]
      [:div#metabadgegrid {:class "row flip-table"}
       [:div.col-md-3.badge-image
        [:div.image-container
         [:img.opaque {:src image :title name}]]]
       [:div.col-md-9
        [:div.col-md-12
         [:h1.uppercase-header name]
         [:div.description description]
         [:div {:class "row criteria-html"}
          [:div.col-md-12
           [:h2.uppercase-header (t :badge/Criteria)]
           [:div {:dangerouslySetInnerHTML {:__html criteria}}]]]]]])))

(defn declined-badge [badge]
  (let [data-atom (atom {:id (-> badge :user_badge :id)})]
    (init-user-badge data-atom)
    (fn []
      (let [{:keys [badge-info user_badge]} badge
            {:keys [image name]} badge-info
            {:keys [owner? user-logged-in?]} @data-atom]
        (if (and user-logged-in? owner?)
          [:div#metabadgegrid {:class "row flip-table"}
           [:div.col-md-3.badge-image
            [:div.image-container
             [:img {:src image :title name}]]]
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
  (let [{:keys [badge required_badges milestone? min_required]} metabadge
        amount_received (count (filter :received required_badges))
        completed-percentage (%completed min_required amount_received)
        completed (if (> completed-percentage 100) 100 (str completed-percentage))
        is-complete? (completed? min_required amount_received)]

    [:div.row.flip-table {:id "metabadgegrid"}
     [:div.col-md-3.badge-image
      [:div.image-container
       [:img {:src (:image badge) :class (if-not is-complete? " opaque")}]]
      [:div.progress
       [:div.progress-bar.progress-bar-success
        {;:class (if  is-complete? "" " progress-bar-striped")
         :role "progressbar"
         :aria-valuenow (str completed)
         :style {:width (str completed "%")}
         :aria-valuemin "0"
         :aria-valuemax "100"}
        (str completed "%")]]]
     [:div.col-md-9
      [:div.col-md-12
       [:h1.uppercase-header (:name metabadge)]
       [:div {:style {:margin-top "10px"}}[:label (str (t :metabadge/Minimumrequired) ": ")] min_required]
       [:div [:label (str (t :metabadge/Amountearned)": ")] amount_received]
       [:div.description (:description badge)]
       #_[:div.panel
        [:div.panel-body
         [:div.panel-heading [:span [:i {:class "fa fa-puzzle-piece"}] (if (empty? (rest required_badges)) (t :metabadge/Requiredbadge) (t :metabadge/Requiredbadges))]]
         [:hr]
         [:div.icons
          (reduce (fn [result badge]
                    (let [{:keys [badge-info received current user_badge]} badge
                          {:keys [name image criteria]} badge-info
                          {:keys [id issued_on status]} user_badge]
                      (conj result
                            (if received
                              (if (= "declined" status)
                                [:a {:href "#" :on-click #(mo/open-modal [:metabadge :declined] badge)} [:img.img-circle {:src image :alt name :title name :class (if current "img-thumbnail" "")}]]
                                [:a {:href "#" :on-click #(mo/open-modal [:badge :info] {:badge-id id})} [:img.img-circle {:src image :alt name :title name :class (if current "img-thumbnail" "")}]])
                              [:a {:href "#" :on-click #(mo/open-modal [:metabadge :dummy] badge)}
                               [:img.not-received.img-circle {:src image :alt name :title name} ]]))
                      )) [:div] (sort-by :received required_badges))]]]
       [:div {:class "row criteria-html"}
        [:div.col-md-12
         [:h2.uppercase-header (t :badge/Criteria)]
         [:div {:dangerouslySetInnerHTML {:__html (:criteria badge)}}]]]
       ]]]))

(defn history [metabadge]
  (let [{:keys [badge required_badges milestone? min_required]} metabadge
        amount_received (count (filter :received required_badges))
        completed-percentage (%completed min_required amount_received)
        completed (if (> completed-percentage 100) 100 (str completed-percentage))
        is-complete? (completed? min_required amount_received)]
    [:div.row.flip-table
     [:div.col-md-3.badge-image
      [:div.image-container
       [:img {:src (:image badge) :class (if-not is-complete? " opaque")}]]
      [:div.progress
       [:div.progress-bar.progress-bar-success
        {;:class (if  is-complete? "" " progress-bar-striped")
         :role "progressbar"
         :aria-valuenow (str completed)
         :style {:width (str completed "%")}
         :aria-valuemin "0"
         :aria-valuemax "100"}
        (str completed "%")]]]
     [:div.col-md-9 {:id "badge-stats"}
      [:div.panel
       [:div.panel-body
        [:div.row.header {:style {:margin-bottom "10px"}}
         [:div.flip-table
          [:div.col-md-6]
          [:div.col-md-2 (t :metabadge/Dateissued)]
          [:div.col-md-4 #_(t :metabadge/Badgestatus)]]]
        (reduce (fn [r badge]
                  (let [{:keys [badge-info received current user_badge]} badge
                        {:keys [name image criteria]} badge-info
                        {:keys [id issued_on status]} user_badge
                        image-class (if-not received " opaque")]
                    (conj r [:div.col-md-12
                             [:div.flip-table
                              [:div.col-md-1 [:img.badge-icon {:src image :class image-class}]]
                              [:div.col-md-5 (if received
                                               (if (= status "declined")
                                                 [:a {:href "#" :on-click #(mo/open-modal [:metabadge :declined] badge)} name]
                                                 [:a {:href "#" :on-click #(mo/open-modal [:badge :info] {:badge-id id})} name])
                                               [:a {:href "#" :on-click #(mo/open-modal [:metabadge :dummy] badge)} name]
                                               )]
                              [:div.col-md-2 [:label (t :metabadge/Dateissued)] (if issued_on (date-from-unix-time (* 1000 issued_on)) "-")]
                              [:div.col-md-4 #_[:label (t :metabadge/Badgestatus)] #_(case status
                                                                                   "accepted" (t :social/accepted)
                                                                                   "declined" (t :social/declined)
                                                                                   "pending" (t :social/pending)
                                                                                   "-")]]]))) [:div.row.body] (->> required_badges
                                                                                                                   (sort-by #(-> % :user_badge :issued_on) >)
                                                                                                                   (sort-by :received >)))]]]]))

(defn modal-navi [metabadge state]
  [:div.row.flip-table
   [:div.col-md-3]
   [:div.col-md-9.badge-modal-navi
    [:ul {:class "nav nav-tabs wrap-grid"}
     [:li.nav-item {:class  (if (or (nil? (:tab-no @state))(= 1 (:tab-no @state))) "active")}
      [:a.nav-link {:href "#" :on-click #(swap! state assoc :tab [view-content metabadge] :tab-no 1 )}
       [:div  [:i.nav-icon {:class "fa fa-eye fa-lg"}] (t :page/View)  ]]]
     [:li.nav-item {:class  (if (or (nil? (:tab-no @state))(= 2 (:tab-no @state))) "active")}
      [:a.nav-link {:href "#" :on-click #(swap! state assoc :tab [history metabadge] :tab-no 2 )}
       [:div  [:i.nav-icon {:class "fa fa-history fa-lg"}] (t :metabadge/History)  ]]]]]])

(defn metabadge-content [metabadge state]
  (let [{:keys [badge required_badges milestone? min_required]} metabadge
        amount_received (count (filter :received required_badges))
        completed-percentage (%completed min_required amount_received)
        completed (if (> completed-percentage 100) 100 (str completed-percentage))
        is-complete? (completed? min_required amount_received)]

    [:div#metabadgegrid {:class "row"}
     [modal-navi metabadge state]
     (if (:tab @state) (:tab @state) [view-content metabadge])]))


(defn multi-block [metabadge]
  (fn []
    (let [current (current-badge (:metabadge metabadge) #_(:metabadge @state))]
      [:div#metabadgegrid {:class "row flip-table"}
       [:div.col-md-3
        [:div.badge-image
         [:img {:src (or (:image current) (-> current :badge-info :image))}]]]
       [:div.col-md-9
        [:div.row
         [:div.col-md-12
          [:h1.uppercase-header (:heading metabadge) #_(or (:name current) (-> current :badge-info :name))]
          [:div.info (t :metabadge/Metabadgeinfo)]
          (reduce (fn [r m] (conj r ^{:key m}[mb/badge-block m]))  [:div#metabadge {:class "row wrap-grid"}] (:metabadge metabadge))]]]])))

(defn handler [metabadge]
  (let [state (atom {:tab-no 1})]
    (fn [] (metabadge-content metabadge state))))


(def ^:export modalroutes
  {:metabadge {:metadata handler
               :multiblock multi-block
               :dummy dummy-badge
               :declined declined-badge}})
