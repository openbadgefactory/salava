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

(defn init-metabadge-icon [id data-atom]
  (ajax/GET
    (path-for (str "/obpv1/metabadge/badge/info"))
    {:params {:user_badge_id id}
     :handler (fn [data] (reset! data-atom data))}))


(defn meta_icon [data-atom]
  (let [required_badge (:required_badge @data-atom)
        milestone (:milestone @data-atom)]
    (cond
      milestone [:div {:title (t :metabadge/Amilestonebadge)}[:span [:i.link-icon {:class "fa fa-sitemap"}]]]
      required_badge [:div {:title (t :metabadge/Partofamilestonebadge)} [:span [:i.link-icon {:class "fa fa-puzzle-piece"}]]]
      :else nil)))

(defn current-badge [required_badges]
  (->> required_badges (filter :current) first))

(defn required-block-badges [m]
  (let [current-badge (current-badge (:required_badges m))]
    (if (:milestone? m) (take 20 (:required_badges m)) (into [current-badge] (take 19 (remove :current (:required_badges m)))))))

(defn partition-count [m]
  (let [no (count (required-block-badges m))]
    (if (<= no 4) no (/ no 2))))


(defn badge-block [m]
  (let [{:keys [received name image criteria]} (:badge m)
        milestone-image-class (if received "" " not-received")]
    [:a {:href "#" :on-click #(mo/open-modal [:metabadge :metadata] m)}
     [:div.metabadge {:class (if (> (count (:required_badges m)) 8) " metabadge-large")}
      [:div.panel
       [:div.panel-heading (:name m)
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
                                            coll))) [:tbody] (partition-all (partition-count m) (required-block-badges m)))]]]]]]]]]))


(defn metabadge-link [assertion-url state]
  (fn []
    (let [metabadge (:metabadge @state)]
      (when-not (empty? (:metabadge @state))
        (reduce-kv
          (fn [r k v]
            (conj r (if (true? k)
                      ^{:key k}(if (empty? (rest v))
                                 [:a {:href "#"
                                      :on-click #(mo/open-modal [:metabadge :metadata] (-> v first))}
                                  [:div [:span [:i.link-icon {:class "fa fa-sitemap"}]]  (t :metabadge/Amilestonebadge)]]

                                 [:a {:href "#"
                                      :on-click #(mo/open-modal [:metabadge :multiblock] {:metabadge v :heading (str (t :metabadge/Multiplemilestones) " (" (count v)")")})}
                                  [:div [:span [:i.link-icon {:class "fa fa-sitemap"}]]  (str (t :metabadge/Multiplemilestones) " (" (count v)")")]])
                      ^{:key k} (if (empty? (rest v))
                                  [:a {:href "#"
                                       :on-click #(mo/open-modal [:metabadge :metadata] (-> v first))}
                                   [:div [:span [:i.link-icon {:class "fa fa-puzzle-piece"}]]  (t :metabadge/Partofamilestonebadge)]]
                                  [:a {:href "#"
                                       :on-click #(mo/open-modal [:metabadge :multiblock] {:metabadge v :heading (str (t :metabadge/Partofmultiplemilestonebadges) " ("(count v)  ")")})}
                                   [:div [:span [:i.link-icon {:class "fa fa-puzzle-piece"}]] (str (t :metabadge/Partofmultiplemilestonebadges) " ("(count v)  ")")]]))))
          [:div.link-icon]
          (group-by :milestone? metabadge))))))

(defn metabadge-icon [id]
  (fn []
    (let [data-atom (atom {})]
      (init-metabadge-icon id data-atom)
      [meta_icon data-atom]
      )))

(defn metabadge [assertion-url]
  (fn []
    (let [state (atom {})]
      (init-metabadge-data assertion-url state)
      [metabadge-link assertion-url state]
      )))

