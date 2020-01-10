(ns salava.metabadge.ui.metabadge
  (:require [reagent.core :refer [atom]]
            [salava.core.ui.ajax-utils :as ajax]
            [salava.core.ui.helper :refer [path-for]]
            [salava.core.i18n :refer [t]]
            [salava.core.ui.modal :as mo]
            [clojure.string :refer [blank?]]
            [cemerick.url :refer [url-encode]]))

(defn init-metabadge-data [param state]
  (let [link (if (:assertion_url param) (str "/obpv1/metabadge/assertion/info/" (url-encode (:assertion_url param))) (str "/obpv1/metabadge/info/" (:user_badge_id param)))]
    (ajax/GET
     (path-for link)
     {;:params param
      :handler (fn [data]
                 (reset! state data))})))

(defn metabadgeicon [meta_badge meta_badge_req]
  (fn []
    (let [is_milestone? (and (not= "NULL" meta_badge) (not (blank? meta_badge)))
          is_required? (not (blank? meta_badge_req))
          string (str is_milestone? is_required?)]
      (case string
        "truefalse" [:div {:title (t :metabadge/Amilestonebadge)} [:span [:i.link-icon {:class "fa fa-sitemap"}]]]
        "truetrue" [:div {:title (t :metabadge/Partofamilestonebadge)} [:span [:i.link-icon {:class "fa fa-puzzle-piece"}]]]
        "falsetrue" [:div {:title (t :metabadge/Partofamilestonebadge)} [:span [:i.link-icon {:class "fa fa-puzzle-piece"}]]]
        nil))))

(defn image-class [completion_status]
  (cond
    (<= 0 completion_status 9) "_1"
    (<= 10 completion_status 19) "_2"
    (<= 20 completion_status 29) "_3"
    (<= 30 completion_status 39) "_4"
    (<= 40 completion_status 49) "_5"
    (<= 50 completion_status 59) "_6"
    (<= 60 completion_status 69) "_7"
    (<= 70 completion_status 79) "_8"
    (<= 80 completion_status 89) "_9"
    (<= 90 completion_status 99) "_10"))

(defn current-badge [required_badges current-badge-id]
  (->> required_badges (filter #(or (= (:user_badge_id %) current-badge-id) (= (:url %) current-badge-id))) first))

(defn required-block-badges [m current-badge-id milestone?]
  (let [current-badge (current-badge (:required_badges m) current-badge-id)]
    (if milestone? (take 20 (:required_badges m)) (into [current-badge] (->> (take 19 (remove #(or (= (:user_badge_id %) current-badge-id) (= (:url %) current-badge-id)) (:required_badges m)))
                                                                             (sort-by :issued_on >))))))
(defn partition-count [m current-badge-id milestone?]
  (let [no (count (required-block-badges m current-badge-id milestone?))]
    (if (<= no 4) no (/ no 2))))

(defn badge-block [m current-badge-id milestone?]
  (let [{:keys [user_badge_id name image_file image criteria criteria_content required_badges received]} m
        milestone-image-class (if (and user_badge_id (pos? user_badge_id)) "" " not-received")]
    [:a {:href "#" :on-click #(mo/open-modal [:metabadge :metadata] m)}
     [:div.metabadge {:class (if (> (count required_badges) 8) " metabadge-large")}
      [:div.panel
       [:div.panel-heading [:span.metabadge_block_name (:name m)]
        [:div.pull-right (if milestone? [:i {:class "fa fa-sitemap"}] [:i {:class "fa fa-puzzle-piece"}])]]
       [:div.panel-body
        [:div.table ;:table.table
         [:div ;:tbody
          [:div ;:tr
           [:div.meta ;:td.meta {:rowSpan "2"}
            [:div [:img.image {:src (if image_file (str "/" image_file) image) :title name :class milestone-image-class :alt (str (t :badge/Badge) " " name)}]]]
           [:div.icon-container ;:td.icon-container
            [:table
             [:thead
              [:tr
               [:th {:style {:display "none"}} (t :metabadge/Requiredbadges)]]]
             (reduce (fn [result coll]
                       (conj result (reduce (fn [r badge]
                                              (let [;{:keys [badge-info received current]} badge
                                                    {:keys [user_badge_id name image_file image criteria criteria_content url]} badge
                                                    image (if image_file (str "/" image_file) image)
                                                    current (if (string? current-badge-id) (= url current-badge-id) (= user_badge_id current-badge-id))]

                                                (conj r (if (and user_badge_id (pos? user_badge_id))
                                                          [:td [:div [:img.img-circle {:src image :alt (str (t :badge/Badge) " " name) :title name :class (if current "img-thumbnail" "")}]]]
                                                          [:td [:div [:img.img-circle.not-received {:src image :alt (str (t :badge/Badge) " " name)  :title name}]]]))))
                                            [:tr]
                                            coll))) [:tbody] (partition-all (partition-count m current-badge-id milestone?) (required-block-badges m current-badge-id milestone?)))]]]]]]]]]))

(defn metabadge-link [id state]
  (fn []
    (when-not (empty? @state)
      (reduce-kv
       (fn [r k v]
         (conj r (case k
                   :milestones ^{:key k} (case (count v)
                                           0 nil
                                           1 [:a {:href "#"
                                                  :on-click #(mo/open-modal [:metabadge :metadata] (-> v first))}
                                              [:div [:span [:i.link-icon {:class "fa fa-sitemap"}]]  (t :metabadge/Amilestonebadge)]]

                                           [:a {:href "#"
                                                :on-click #(mo/open-modal [:metabadge :multiblock] {:metabadge v :heading (str (t :metabadge/Multiplemilestones) " (" (count v) ")") :current id :milestone? true})}
                                            [:div [:span [:i.link-icon {:class "fa fa-sitemap"}]]  (str (t :metabadge/Multiplemilestones) " (" (count v) ")")]])
                   :required-in ^{:key k} (case (count v)
                                            0 nil
                                            1 [:a {:href "#"
                                                   :on-click #(mo/open-modal [:metabadge :metadata] (-> v first))}
                                               [:div [:span [:i.link-icon {:class "fa fa-puzzle-piece"}]]  (t :metabadge/Partofamilestonebadge)]]
                                            [:a {:href "#"
                                                 :on-click #(mo/open-modal [:metabadge :multiblock] {:metabadge v :heading (str (t :metabadge/Partofmultiplemilestonebadges) " (" (count v)  ")") :current id})}
                                             [:div [:span [:i.link-icon {:class "fa fa-puzzle-piece"}]] (str (t :metabadge/Partofmultiplemilestonebadges) " (" (count v)  ")")]]))))
       [:div.link-icon] @state))))

(defn metabadge [param]
  (fn []
    (let [state (atom {})]
      (init-metabadge-data param state)
      [metabadge-link (-> param first val) state])))
