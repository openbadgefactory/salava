(ns salava.metabadge.ui.modal
  (:require [reagent.core :refer [atom]]
            [salava.core.ui.ajax-utils :as ajax]
            [salava.core.ui.helper :refer [path-for]]
            [reagent.session :as session]
            [clojure.string :refer [split]]
            [salava.core.i18n :refer [t]]
            [salava.metabadge.ui.metabadge :as mb]))

(defn current-badge [metabadge]
  (let [{:keys [milestone? required_badges badge]} (-> metabadge first)]
    (if milestone? badge (->> required_badges (filter :current) first))))

(defn %completed [req gotten]
  (Math/round (double (* (/ gotten req) 100))))

(defn completed? [req gotten]
  (>= gotten req))

(defn metabadge-content [metabadge]
  (fn []
    (let [ {:keys [badge required_badges milestone? min_required]} metabadge
           amount_received (count (filter :received required_badges))
           completed (str (%completed min_required amount_received))
           is-complete? (completed? min_required amount_received)]
      [:div#metabadgegrid {:class "row"}
       [:div.col-md-3.badge-image
        [:img {:src (:image badge)}]
        [:div.progress
         [:div.progress-bar.progress-bar-success
          {:class (if  is-complete? "" " progress-bar-striped active")
           :role "progressbar"
           :aria-valuenow completed
           :style {:width (str completed "%")}
           :aria-valuemin "0"
           :aria-valuemax "100"}
          (str completed "%")]]]
       [:div.col-md-9
        [:div.row
         [:h1.uppercase-header (:name badge)]
         [:div.description (:description badge)]
         [:div {:style {:margin-top "10px"}}[:label "Minimum required: "] min_required]
         [:div [:label "Amount received: "] amount_received]
         [:div [:label (t :badge/Criteria) ": "] [:a {:href (:criteria badge) :target "_blank"} (t :badge/Opencriteriapage) "..."]]
         ;[:div [:label "% completed: "] (str (%completed min_required amount_received) " %" )]

         [:div.panel
          [:div.panel-body
           [:div.panel-heading [:span [:i {:class "fa fa-puzzle-piece"}] "Required badges"]]
           [:hr]
           [:div.icons
            (conj
              (into [:div.col-xs-12]
                    (for [badge required_badges
                          :let [{:keys [badge-info received current]} badge
                                {:keys [name image criteria]} badge-info]]
                      (if received
                        [:img {:src image :alt name :title name :class (if current "img-thumbnail" "")}]
                        [:a {:href criteria :target "_blank" :rel "noopener noreferrer" }
                         [:img.not-received {:src image :alt name :title name} ]]))))]]]]]])))

(defn multi-block [state]
  (let [current (current-badge (:metabadge @state))]
    [:div#metabadgegrid {:class "row"}
     [:div.col-md-3
      [:div.badge-image
       [:img {:src (or (:image current) (-> current :badge-info :image))}]]]
     [:div.col-md-9
      [:div.row
       [:h1.uppercase-header (or (:name current) (-> current :badge-info :name))]
       [mb/metabadge-block state]]]]))

(defn content [state]
  (fn []
    (let [metabadge (:metabadge @state)]
      (if (empty? (rest metabadge))
        [metabadge-content (-> metabadge first)]
        [multi-block state]))))


(def ^:export modalroutes
  {:metabadge {:grid content
               :metadata metabadge-content}}
  )
