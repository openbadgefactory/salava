(ns salava.metabadge.ui.modal
  (:require [reagent.core :refer [atom]]
            [salava.core.ui.ajax-utils :as ajax]
            [salava.core.ui.helper :refer [path-for]]
            [reagent.session :as session]
            [clojure.string :refer [split]]
            [salava.core.i18n :refer [t]]
            [salava.metabadge.ui.metabadge :as mb]
            [salava.core.ui.modal :as mo]))

(defn current-badge [metabadge]
  (let [{:keys [milestone? required_badges badge]} (-> metabadge first)]
    (if milestone? badge (->> required_badges (filter :current) first))))

(defn %completed [req gotten]
  (Math/round (double (* (/ gotten req) 100))))

(defn completed? [req gotten]
  (>= gotten req))

(defn dummy-badge [badge]
  (fn []
    (let [{:keys [badge-info received current]} badge
          {:keys [image name description criteria]} badge-info]
      [:div#metabadgegrid {:class "row"}
       [:div.col-md-3.badge-image
        [:div.image-container
         [:img.opaque {:src image :title "dummy badge"}]]]
       [:div.col-md-9
        [:div.row
         [:div.col-md-12
          [:h1.uppercase-header name]
          [:div.description description]
          [:div {:class "row criteria-html"}
           [:div.col-md-12
            [:h2.uppercase-header (t :badge/Criteria)]
            [:div {:dangerouslySetInnerHTML {:__html criteria}}]]]
          ]]]])))

(defn view-content [metabadge]
  (let [{:keys [badge required_badges milestone? min_required]} metabadge
        amount_received (count (filter :received required_badges))
        completed-percentage (%completed min_required amount_received)
        completed (if (> completed-percentage 100) 100 (str completed-percentage))
        is-complete? (completed? min_required amount_received)
        ;state (atom {:tab-no 1})
        ]
    (prn (map #(-> %
                   :badge-info
                   (dissoc :image)) required_badges))
    [:div.row.flip
     [:div.col-md-3.badge-image
      [:div.image-container
       [:img {:src (:image badge) :class (if-not is-complete? " opaque")}]
       #_[:span.veil {:style {:height (if is-complete? "0%" "100%")}} ]]
      [:div.progress
       [:div.progress-bar.progress-bar-success
        {:class (if  is-complete? "" " progress-bar-striped active")
         :role "progressbar"
         :aria-valuenow (str completed)
         :style {:width (str completed "%")}
         :aria-valuemin "0"
         :aria-valuemax "100"}
        (str completed "%")]]]
     [:div.col-md-9
      [:div.row
       [:div.col-md-12
        [:h1.uppercase-header (:name metabadge)]
        [:div.description (:description badge)]
        [:div {:style {:margin-top "10px"}}[:label (str (t :metabadge/Minimumrequired) ": ")] min_required]
        [:div [:label (str (t :metabadge/Amountearned)": ")] amount_received]

        [:div.panel
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
                               [:a {:href "#" :on-click #(mo/open-modal [:badge :info] {:badge-id id})} [:img.img-circle {:src image :alt name :title name :class (if current "img-thumbnail" "")}]]
                               [:a {:href "#" :on-click #(mo/open-modal [:metabadge :dummy] badge)}
                                [:img.not-received.img-circle {:src image :alt name :title name} ]]))
                       )) [:div] (sort-by :received required_badges))]]]
        [:div {:class "row criteria-html"}
         [:div.col-md-12
          [:h2.uppercase-header (t :badge/Criteria)]
          [:div {:dangerouslySetInnerHTML {:__html (:criteria badge)}}]]]
        ]]]

     ]))

(defn history [metabadge]
  (let [{:keys [badge required_badges milestone? min_required]} metabadge
        amount_received (count (filter :received required_badges))
        completed-percentage (%completed min_required amount_received)
        completed (if (> completed-percentage 100) 100 (str completed-percentage))
        is-complete? (completed? min_required amount_received)]
    [:div.row.flip
     [:div.col-md-3.badge-image
      [:div.image-container
       [:img {:src (:image badge) :class (if-not is-complete? " opaque")}]]
      [:div.progress
       [:div.progress-bar.progress-bar-success
        {:class (if  is-complete? "" " progress-bar-striped active")
         :role "progressbar"
         :aria-valuenow (str completed)
         :style {:width (str completed "%")}
         :aria-valuemin "0"
         :aria-valuemax "100"}
        (str completed "%")]]]
     [:div.col-md-9
      [:div.row
       ;[:div.col-md-12
        [:div.panel
         [:div.panel-body
          [:div.row.header
           ;[:div.col-md-12
            [:div.flip-table
             [:div.col-md-5]
             [:div.col-md-2 "issued on"]
             [:div.col-md-2 "status"]]];]
          ]

         ]

        ]]
      ;]
     ]

    #_[:table.table
       ; [:tbody
       (reduce (fn [result badge]
                 (let [{:keys [badge-info received current user_badge]} badge
                       {:keys [name image criteria]} badge-info
                       {:keys [id issued_on status]} user_badge]
                   (conj result [:tr [:div.inline [:image.img-circle {:src image}]  ]])

                   )

                 ) [:tbody] (:required_badges metabadge))];]
    ))

(defn modal-navi [metabadge state]
  [:div.row.flip
   [:div.col-md-3]
   [:div.col-md-9.badge-modal-navi
    [:ul {:class "nav nav-tabs wrap-grid"}
     [:li.nav-item {:class  (if (or (nil? (:tab-no @state))(= 1 (:tab-no @state))) "active")}
      [:a.nav-link {:href "#" :on-click #(swap! state assoc :tab [view-content metabadge] :tab-no 1 )}
       [:div  [:i.nav-icon {:class "fa fa-eye fa-lg"}] (t :page/View)  ]]]
     [:li.nav-item {:class  (if (or (nil? (:tab-no @state))(= 2 (:tab-no @state))) "active")}
      [:a.nav-link {:href "#" :on-click #(do (swap! state assoc :tab [history metabadge] :tab-no 2 )
                                           (prn (:tab-no @state )))}
       [:div  [:i.nav-icon {:class "fa fa-history fa-lg"}] "History" #_(t :page/View)  ]]]]]])

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
      [:div#metabadgegrid {:class "row"}
       [:div.col-md-3
        [:div.badge-image
         [:img {:src (or (:image current) (-> current :badge-info :image))}]]]
       [:div.col-md-9
        [:div.row
         [:div.col-md-12
          [:h1.uppercase-header (:heading metabadge) #_(or (:name current) (-> current :badge-info :name))]
          [:div.info (t :metabadge/Metabadgeinfo)]
          (reduce (fn [r m] (conj r ^{:key m}[mb/badge-block m]))  [:div#metabadge] (:metabadge metabadge))]]]])))

(defn handler [metabadge]
  (let [state (atom {:tab-no 1})]
    (fn [] (metabadge-content metabadge state))))


(def ^:export modalroutes
  {:metabadge {:metadata handler
               :multiblock multi-block
               :dummy dummy-badge}})
