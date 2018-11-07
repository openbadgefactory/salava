(ns salava.metabadge.ui.metabadge
  (:require [reagent.core :refer [atom]]
            [salava.core.ui.ajax-utils :as ajax]
            [salava.core.ui.helper :refer [path-for]]
            [reagent.session :as session]
            [clojure.string :refer [split]]
            [salava.core.i18n :refer [t]]
            [salava.core.ui.modal :as mo]))

(defn init-metabadge-data [assertion-url state]
  (ajax/GET
    (path-for (str "/obpv1/metabadge/info"))
    {:params {:assertion_url assertion-url}
     :handler (fn [data]
                (reset! state data))}))

(defn milestone-badge-block [m]
  (let [{:keys [received name image criteria]} (:badge m)]
    [:div
     [:div.info [:span [:i {:class "fa fa-sitemap"}] "Milestone badge"]]
     [:div.metabadge
      [:div.icons
       (conj
         (into [:div]
               (for [badge (:required_badges m)
                     :let [{:keys [badge-info received]} badge
                           {:keys [name image]} badge-info]]
                 (if received
                   [:img {:src image :alt name :title name}]
                   [:img.not-received {:src image :alt name :title name} ])))
         [:div.milestone-badge [:img.img-thumbnail {:src image :title name }]])]]]))

(defn required-badge-block [m]
  (let [{:keys [received name image criteria]} (:badge m)
        milestone-image-class (if received "" " not-received")]
     [:a {:href "#" :on-click #(mo/open-modal [:metabadge :metadata] m)}
    [:div.metabadge
     [:div.icons


        [:div.milestone-badge
         [:img {:src image :title name :class milestone-image-class}]]
        [:div.required-badges
                  (into [:div]
               (for [badge (:required_badges m)
                     :let [{:keys [badge-info received current]} badge
                           {:keys [name image criteria]} badge-info]]
                 (if received
                   [:img {:src image :alt name :title name :class (if current "img-thumbnail" "")}]
                   ;[:a {:href criteria :target "_blank" :rel "noopener noreferrer" }
                    [:img.not-received {:src image :alt name :title name} ];]
                   )))]
        ]
      ]

     ]
    #_[:div
     [:a {:href "#" :on-click #(mo/open-modal [:metabadge :metadata] m)}
     [:div.info [:span [:i {:class "fa fa-puzzle-piece"}] "Required badge"]]
     [:div.metabadge
      [:div.icons
       (conj
         (into [:div]
               (for [badge (:required_badges m)
                     :let [{:keys [badge-info received current]} badge
                           {:keys [name image criteria]} badge-info]]
                 (if received
                   [:img {:src image :alt name :title name :class (if current "img-thumbnail" "")}]
                   ;[:a {:href criteria :target "_blank" :rel "noopener noreferrer" }
                    [:img.not-received {:src image :alt name :title name} ];]
                   )))
         (if (not received)
           [:div.milestone-badge [:a {:href criteria :target "_blank" :rel "noopener noreferrer" } [:img.milestone-badge {:src image :title name :class milestone-image-class}]]]
           [:div.milestone-badge [:img {:src image :title name :class milestone-image-class}]]))]]]]))


#_(defn metabadge-block [assertion-url]
  (let [state (atom {})]
    (init-metabadge-data assertion-url state)
    (fn []
      (let [{:keys [metabadge obf-url]} @state]
        (when-not (empty? metabadge)
          (into [:div#metabadge
                 (for [m metabadge
                       :let [{:keys [milestone? badge]} m]]
                   (if milestone? ^{:key m} [milestone-badge-block m] ^{:key m} [required-badge-block m]))]))))))

(defn metabadge-block [state]
  ;(let [state (atom {})]
    ;(init-metabadge-data assertion-url state)
    (fn []
      (let [{:keys [metabadge obf-url]} @state]
        (when-not (empty? metabadge)
          (into [:div#metabadge
                 (for [m metabadge
                       :let [{:keys [milestone? badge]} m]]
                   (if milestone? ^{:key m} [milestone-badge-block m] ^{:key m} [required-badge-block m]))])))));)

(defn metabadge-link [assertion-url state]
  (init-metabadge-data assertion-url state)
  (fn []
    (let [metabadge (:metabadge @state)]
      (when-not (empty? (:metabadge @state))
        [:div#metabadge
         [:a {:href "#"
              :on-click #(mo/open-modal [:metabadge :grid] state)}
         (if (> (count metabadge) 1)
          [:div [:i.link-icon {:class "fa fa-sitemap"}] (str (count metabadge) " Metabadges")]
           (if (-> metabadge first :milestone?)
             [:div [:i.link-icon {:class "fa fa-sitemap"}] "Milestone Badge"]
              [:div [:i.link-icon {:class "fa fa-puzzle-piece"}] "Required Badge"]))]]))))

(defn metabadge [assertion-url]
  (let [state (atom {:assertion_url assertion-url})]
    [:div
     [metabadge-link assertion-url state]]))

