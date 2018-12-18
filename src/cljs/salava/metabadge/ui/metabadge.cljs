(ns salava.metabadge.ui.metabadge
  (:require [reagent.core :refer [atom]]
            [salava.core.ui.ajax-utils :as ajax]
            [salava.core.ui.helper :refer [path-for]]
            [salava.core.i18n :refer [t]]
            [salava.core.ui.modal :as mo]))

(defn init-metabadge-data [id state]
  (ajax/GET
    (path-for (str "/obpv1/metabadge/info"))
    {:params {:user_badge_id id}
     :handler (fn [data]
                (reset! state data))}))

(defn init-metabadge-icon [id data-atom]
  (ajax/GET
    (path-for (str "/obpv1/metabadge/badge/info"))
    {:params {:user_badge_id id}
     :handler (fn [data] (reset! data-atom data))}))


#_(defn meta_icon [data-atom]
    (let [required_badge (:required_badge @data-atom)
          milestone (:milestone @data-atom)]
      (cond
        milestone [:div {:title (t :metabadge/Amilestonebadge)}[:span [:i.link-icon {:class "fa fa-sitemap"}]]]
        required_badge [:div {:title (t :metabadge/Partofamilestonebadge)} [:span [:i.link-icon {:class "fa fa-puzzle-piece"}]]]
        :else nil)))

(defn meta_icon [data-atom]
  (let [{:keys [meta_badge meta_badge_req]} @data-atom
        m (str meta_badge meta_badge_req)]
    (case m
      "truefalse" [:div {:title (t :metabadge/Amilestonebadge)}[:span [:i.link-icon {:class "fa fa-sitemap"}]]]
      "truetrue" [:div {:title (t :metabadge/Partofamilestonebadge)} [:span [:i.link-icon {:class "fa fa-puzzle-piece"}]]]
      "falsetrue" [:div {:title (t :metabadge/Partofamilestonebadge)} [:span [:i.link-icon {:class "fa fa-puzzle-piece"}]]]
      nil)))

(defn current-badge [required_badges current-badge-id]
  (->> required_badges (filter #(= (:id %) current-badge-id)) first))

(defn required-block-badges [m current-badge-id milestone?]
  (let [current-badge (current-badge (:required_badges m) current-badge-id ) ]
    (if milestone? (take 20 (:required_badges m)) (into [current-badge] (take 19 (remove #(= (:id %) current-badge-id) (:required_badges m)))))))

(defn partition-count [m current-badge-id milestone?]
  (let [no (count (required-block-badges m current-badge-id milestone?))]
    (if (<= no 4) no (/ no 2))))


(defn badge-block [m current-badge-id milestone?]
  (let [{:keys [user_badge_id name image_file criteria criteria_content required_badges]} m
        milestone-image-class (if user_badge_id "" " not-received")]
    [:a {:href "#" :on-click #(mo/open-modal [:metabadge :metadata] m)}
     [:div.metabadge {:class (if (> (count required_badges) 8) " metabadge-large")}
      [:div.panel
       [:div.panel-heading (:name m)
        [:div.pull-right (if milestone? #_(:milestone? m) [:i {:class "fa fa-sitemap"}] [:i {:class "fa fa-puzzle-piece"}])]]
       [:div.panel-body
        [:table.table
         [:tbody
          [:tr
           [:td.meta {:rowSpan "2"}
            [:div [:img.image {:src (str "/" image_file) :title name :class milestone-image-class}]]]
           [:td.icon-container
            [:table
             (reduce (fn [result coll]
                       (conj result (reduce (fn [r badge]
                                              (let [;{:keys [badge-info received current]} badge
                                                    {:keys [id name image_file criteria criteria_content]} badge
                                                     current (= id current-badge-id)]
                                                (conj r (if id
                                                          [:td [:div [:img.img-circle {:src (str "/" image_file) :alt name :title name :class (if current "img-thumbnail" "")
                                                                                       }]]]
                                                          [:td [:div [:img.img-circle.not-received {:src (str "/" image_file) :alt name :title name} ]]]))))
                                            [:tr]
                                            coll))) [:tbody] (partition-all (partition-count m current-badge-id milestone?) (required-block-badges m current-badge-id milestone?)))]]]]]]]]]))


(defn metabadge-link [id state]
  (fn []
    (when-not (empty? @state)
      (reduce-kv
        (fn [r k v]
          (conj r (case k
                    :milestones ^{:key k}(case (count v)
                                           0 nil
                                           1 [:a {:href "#"
                                                  :on-click #(mo/open-modal [:metabadge :metadata] (-> v first))}
                                              [:div [:span [:i.link-icon {:class "fa fa-sitemap"}]]  (t :metabadge/Amilestonebadge)]]

                                           [:a {:href "#"
                                                :on-click #(mo/open-modal [:metabadge :multiblock] {:metabadge v :heading (str (t :metabadge/Multiplemilestones) " (" (count v)")") :current id :milestone? true})}
                                            [:div [:span [:i.link-icon {:class "fa fa-sitemap"}]]  (str (t :metabadge/Multiplemilestones) " (" (count v)")")]])
                    :required-in ^{:key k} (case (count v)
                                             0 nil
                                             1 [:a {:href "#"
                                                    :on-click #(mo/open-modal [:metabadge :metadata] (-> v first))}
                                                [:div [:span [:i.link-icon {:class "fa fa-puzzle-piece"}]]  (t :metabadge/Partofamilestonebadge)]]
                                             [:a {:href "#"
                                                  :on-click #(mo/open-modal [:metabadge :multiblock] {:metabadge v :heading (str (t :metabadge/Partofmultiplemilestonebadges) " ("(count v)  ")") :current id})}
                                              [:div [:span [:i.link-icon {:class "fa fa-puzzle-piece"}]] (str (t :metabadge/Partofmultiplemilestonebadges) " ("(count v)  ")")]]))))
        [:div.link-icon] @state))))

(defn metabadge-icon [id]
  (fn []
    (let [data-atom (atom {})]
      (init-metabadge-icon id data-atom)
      [meta_icon data-atom]
      )))

(defn metabadge [id]
  (fn []
    (let [state (atom {})]
      (init-metabadge-data id state)
      [metabadge-link id state]
      )))

