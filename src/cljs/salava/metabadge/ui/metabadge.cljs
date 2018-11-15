(ns salava.metabadge.ui.metabadge
  (:require [reagent.core :refer [atom]]
            [salava.core.ui.ajax-utils :as ajax]
            [salava.core.ui.helper :refer [path-for]]
            [salava.core.i18n :refer [t]]
            [salava.core.ui.modal :as mo]))

(defn init-metabadge-data [assertion-url state]
  (ajax/GET
    (path-for (str "/obpv1/metabadge/info"))
    {:params {:assertion_url assertion-url}
     :handler (fn [data]
                (reset! state data))}))

(defn required-block-badges [m]
  (take 20 (shuffle (:required_badges m))))

(defn partition-count [m]
  (let [no (count (required-block-badges m) #_(:required_badges m))]
    (if (< no 4) no (/ no 2))))

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
            [:div [:img.image {:src image :title name :class milestone-image-class}]]]
           [:td.icon-container
            [:table
             (reduce (fn [result coll]
                       (conj result (reduce (fn [r badge]
                                              (let [{:keys [badge-info received current]} badge
                                                    {:keys [name image criteria]} badge-info]
                                                (conj r (if received
                                                          [:td [:div [:img.img-circle {:src image :alt name :title name :class (if current "img-thumbnail" "")}]]]
                                                          [:td [:div [:img.img-circle.not-received {:src image :alt name :title name} ]]]))))
                                            [:tr]
                                            coll))) [:tbody] (partition-all (partition-count m) (required-block-badges m) #_(take 20 (shuffle (:required_badges m)))))]]]]]]]]]))


(defn metabadge-block [state]
  (fn []
    (let [{:keys [metabadge obf-url]} @state]
      (when-not (empty? metabadge)
        (into [:div#metabadge
               (for [m metabadge
                     :let [{:keys [milestone? badge]} m]]
                 ^{:key m} [badge-block m])])))))

(defn process-links [metabadge]
  (reduce-kv (fn [r k v]
               (assoc r (keyword (str k)) {:content v :text (if (true? k) (str "This badge is a milestone badge") (if (empty? (rest v))
                                                                                                                    "This badge is a part of a milestone badge"
                                                                                                                    (str "This badge is a part of " (count v) " milestone badges")))})
               ) {} (group-by :milestone? metabadge)))


(defn metabadge-link [assertion-url state]
  (init-metabadge-data assertion-url state)
  (fn []
    (let [metabadge (:metabadge @state)]
      (prn (->> (process-links metabadge)
               vals
               (map :text)))
      (when-not (empty? (:metabadge @state))
        [:div.link-icon
         [:label (str (t :metabadge/Metabadge) ": ")]
         [:a {:href "#"
              :on-click #(mo/open-modal [:metabadge :grid] state)}
          (if (> (count metabadge) 1)
            [:div [:span [:i.link-icon {:class "fa fa-sitemap"}]] (str (count metabadge) " " (t :metabadge/Milestones))]
            (if (-> metabadge first :milestone?)
              [:div [:span [:i.link-icon {:class "fa fa-sitemap"}]] (t :metabadge/Milestonebadge)]
              [:div.inline [:span [:i.link-icon {:class "fa fa-puzzle-piece"}]] (t :metabadge/Requiredinmilestone)]))]]))))

(defn metabadge [assertion-url]
  (fn []
    (let [state (atom {:assertion_url assertion-url})]
      [:div
       [metabadge-link assertion-url state]])))

