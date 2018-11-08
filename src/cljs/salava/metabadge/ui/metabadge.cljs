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

(defn badge-block [m]
  (let [{:keys [received name image criteria]} (:badge m)
        milestone-image-class (if received "" " not-received")]
    [:a {:href "#" :on-click #(mo/open-modal [:metabadge :metadata] m)}
     [:div.metabadge
      [:div.panel
       [:div.panel-heading name
        [:div.pull-right (if (:milestone? m) [:i {:class "fa fa-sitemap"}] [:i {:class "fa fa-puzzle-piece"}])]]
       [:div.panel-body
        [:table.table
         [:tbody
          [:tr
           [:td.meta {:rowSpan "2"}
            [:div [:object.image {:data image :title name :class milestone-image-class}
                   [:div.dummy [:i.fa.fa-certificate]]] #_[:img.image {:src image :title name :class milestone-image-class}]]]
           [:td.icon-container
            [:table
             (into [:tbody]
                   (for [badges (partition-all (/ (count (:required_badges m) ) 2) (take 20 (shuffle (:required_badges m))))]
                     (into [:tr]
                           (for [badge badges
                                 :let [{:keys [badge-info received current]} badge
                                       {:keys [name image criteria]} badge-info]]
                             (if received
                               [:td [:div [:object.img-circle {:data image :title name :class (if current "img-thumbnail" "")}
                                           [:div.dummy [:i.fa.fa-certificate]]]
                                     #_[:img.img-circle {:src image :alt name :title name :class (if current "img-thumbnail" "")}]]]
                               [:td [:div [:object.img-circle.not-received {:data image :alt name :title name}
                                           [:div.dummy [:i.fa.fa-certificate]]
                                           ]
                                     #_[:img.img-circle.not-received {:src image :alt name :title name} ]]])))))]]]]]]]]]))


(defn metabadge-block [state]
  (fn []
    (let [{:keys [metabadge obf-url]} @state]
      (when-not (empty? metabadge)
        (into [:div#metabadge
               (for [m metabadge
                     :let [{:keys [milestone? badge]} m]]
                 ^{:key m} [badge-block m])])))));)

(defn metabadge-link [assertion-url state]
  (init-metabadge-data assertion-url state)
  (fn []
    (let [metabadge (:metabadge @state)]
      (when-not (empty? (:metabadge @state))
        [:div.link-icon
         [:a {:href "#"
              :on-click #(mo/open-modal [:metabadge :grid] state)}
          (if (> (count metabadge) 1)
            [:div [:span [:i.link-icon {:class "fa fa-sitemap"}]] (str (count metabadge) " Metabadges")]
            (if (-> metabadge first :milestone?)
              [:div [:span [:i.link-icon {:class "fa fa-sitemap"}]] "Milestone Badge"]
              [:div [:span [:i.link-icon {:class "fa fa-puzzle-piece"}]] "Required Badge"]))]]))))

(defn metabadge [assertion-url]
  (fn []
  (let [state (atom {:assertion_url assertion-url})]
    [:div
     [metabadge-link assertion-url state]])))

