(ns salava.metabadge.ui.metabadge
  (:require [reagent.core :refer [atom]]
            [salava.core.ui.ajax-utils :as ajax]
            [salava.core.ui.helper :refer [path-for]]
            [reagent.session :as session]
            [clojure.string :refer [split]]
            [salava.core.i18n :refer [t]]))

(defn init-metabadge-data [assertion-url state]
  (ajax/GET
    (path-for (str "/obpv1/metabadge/info"))
    {:params {:assertion_url assertion-url}
     :handler (fn [data]
                (reset! state data))}))

(defn milestone-badge-block [metabadge]
  (let [required-badges (-> metabadge first :required_badges)
        min-required (-> metabadge first :min_required)]
    [:div.metabadge
     [:div.info "Milestone badge"]
     [:div.icons
      (into [:div]
            (for [badge required-badges
                  :let [{:keys [badge-info received]} badge
                        {:keys [name image]} badge-info]]
              (if received
                [:img {:src image :alt name}]
                [:img.not-received {:src image :alt name} ])))]]))


(defn required-badge-block [milestone_badge metabadge]
  (let [required-badges (-> metabadge first :required_badges)
        min-required (-> metabadge first :min_required)
        {:keys [received name image criteria]} milestone_badge
        milestone-image-class (if received "" " not-received")]
    [:div.metabadge
     [:div.info "Required badge"]
     [:div.icons
      (conj
        (into [:div]
              (for [badge required-badges
                    :let [{:keys [badge-info received]} badge
                          {:keys [name image criteria]} badge-info]]
                (if received
                  [:img {:src image :alt name :title name}]
                  [:a {:href criteria :target "_blank" :rel "noopener noreferrer" }[:img.not-received {:src image :alt name :title name} ]])))
       (if (not received)
         [:a {:href criteria :target "_blank" :rel "noopener noreferrer" } [:img.milestone-badge {:src (:image milestone_badge) :title (:name milestone_badge) :class milestone-image-class}]]
         [:img.milestone-badge {:src (:image milestone_badge) :title (:name milestone_badge) :class milestone-image-class}]))]]))

(defn metabadge-block [assertion-url]
  (let [state (atom {})]
    (init-metabadge-data assertion-url state)
    (fn []
      (let [{:keys [metabadge milestone_badge milestone? obf-url]} @state
            required-badges (-> metabadge first :required_badges)
            min-required (-> metabadge first :min_required)
            initial-milestone-info (-> metabadge first :badge)]
        (when-not (empty? metabadge)
          [:div#metabadge
           (if milestone?
             [milestone-badge-block metabadge]
             [required-badge-block (merge milestone_badge initial-milestone-info) metabadge]
             )])
        #_(if (not (empty? metabadge))
            [:div#metabadge
             ;[:label (if is-metabadge (t :badge/Milestonebadge) (t :badge/Requiredbadge))]
             [:div.metabadge
              [:div.icons
               (into [:div [:span #_(if milestone? [:i {:class "fa fa-sitemap"}]) (if milestone? "Milestone badge" "required badge")] [:br]]
                     (for [badge required-badges
                           :let [{:keys [badge-info received]} badge
                                 {:keys [name image]} badge-info]]
                       (if received
                         [:img {:src image :alt name}]
                         [:img.not-received {:src image :alt name} ])))]]])))))
