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
    (let [{:keys [badge required_badges milestone? min_required]} metabadge
           amount_received (count (filter :received required_badges))
           completed-percentage (%completed min_required amount_received)
           completed (if (> completed-percentage 100) 100 (str completed-percentage))
           is-complete? (completed? min_required amount_received)]
      [:div#metabadgegrid {:class "row"}
       [:div.col-md-3.badge-image
        [:div.image-container
         [:img {:src (:image badge)}]
         #_[:object {:data (:image badge)}
          [:i.fa.fa-certificate]]
         [:span.veil {:style {:height (if is-complete? "0%" "100%")}} ]]
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
         [:h1.uppercase-header (:name badge)]
         [:div.description (:description badge)]
         [:div {:style {:margin-top "10px"}}[:label "Minimum required: "] min_required]
         [:div [:label "Amount received: "] amount_received]
         [:div [:label (t :badge/Criteria) ": "] [:a {:href (:criteria badge) :target "_blank"} (t :badge/Opencriteriapage) "..."]]

         [:div.panel
          [:div.panel-body
           [:div.panel-heading [:span [:i {:class "fa fa-puzzle-piece"}] "Required badges"]]
           [:hr]
           [:div.icons
            (conj
              (into [:div]
                    (for [badge required_badges
                          :let [{:keys [badge-info received current]} badge
                                {:keys [name image criteria]} badge-info]]
                      (if received
                        #_[:object.img-circle {:class (if current "img-thumbnail" "") :data image :title name}
                         [:div.dummy [:i.fa.fa-certificate]]]
                        [:img.img-circle {:src image :alt name :title name :class (if current "img-thumbnail" "")}]
                        [:a {:href criteria :target "_blank" :rel "noopener noreferrer" }
                         #_[:object.not-received.img-circle {:data image :title name} [:div.dummy [:i.fa.fa-certificate]]]
                         [:img.not-received.img-circle {:src image :alt name :title name} ]]))))]]]]]]])))

(defn multi-block [state]
  (let [current (current-badge (:metabadge @state))]
    [:div#metabadgegrid {:class "row"}
     [:div.col-md-3
      [:div.badge-image
       [:img {:src (or (:image current) (-> current :badge-info :image))}]]]
     [:div.col-md-9
      [:div.row
       [:div.col-md-12
        [:h1.uppercase-header (or (:name current) (-> current :badge-info :name))]
        [:div "A milestone badge can be earned when a number of required badges has been earned. Each block below represents a milestone badge, click on block to see your progress....."]
        [mb/metabadge-block state]]]]]))

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
