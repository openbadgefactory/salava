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

(defn milestone-badge-block [milestone_badge metabadge]
  (let [required-badges (-> metabadge first :required_badges)
        min-required (-> metabadge first :min_required)
        {:keys [received name image criteria]} milestone_badge]
    [:div.metabadge
     [:div.icons
      (conj
      (into [:div]
            (for [badge required-badges
                  :let [{:keys [badge-info received]} badge
                        {:keys [name image]} badge-info]]
              (if received
                [:img {:src image :alt name :title name}]
                [:img.not-received {:src image :alt name :title name} ])))
        [:div.milestone-badge [:img.img-thumbnail {:src (:image milestone_badge) :title (:name milestone_badge)}]]
        )]]))


(defn required-badge-block [milestone_badge metabadge]
  (let [required-badges (-> metabadge first :required_badges)
        min-required (-> metabadge first :min_required)
        {:keys [received name image criteria]} milestone_badge
        milestone-image-class (if received "" " not-received")]
    [:div.metabadge
     [:div.icons
      (conj
        (into [:div]
              (for [badge required-badges
                    :let [{:keys [badge-info received current]} badge
                          {:keys [name image criteria]} badge-info]]
                (if received
                  [:img {:src image :alt name :title name :class (if current "img-thumbnail" "")}]
                  [:a {:href criteria :target "_blank" :rel "noopener noreferrer" }[:img.not-received {:src image :alt name :title name} ]])))
        (if (not received)
          [:div.milestone-badge [:a {:href criteria :target "_blank" :rel "noopener noreferrer" } [:img.milestone-badge {:src (:image milestone_badge) :title (:name milestone_badge) :class milestone-image-class}]]]
          [:div.milestone-badge [:img {:src (:image milestone_badge) :title (:name milestone_badge) :class milestone-image-class}]]))]]))

(defn metabadge-block [assertion-url]
  (let [state (atom {})]
    (init-metabadge-data assertion-url state)
    (fn []
      (let [{:keys [metabadge milestone_badge milestone? obf-url]} @state
            required-badges (-> metabadge first :required_badges)
            min-required (-> metabadge first :min_required)
            initial-milestone-info (-> metabadge first :badge)]
        (prn @state)
        (when-not (empty? metabadge)
          [:div#metabadge
           [:div.info (if milestone? [:span [:i {:class "fa fa-sitemap"}] "Milestone badge"] [:span [:i {:class "fa fa-puzzle-piece"}] "Required badge"])]
           (if milestone?
             [milestone-badge-block (merge milestone_badge initial-milestone-info) metabadge]
             [required-badge-block (merge milestone_badge initial-milestone-info) metabadge])])))))
