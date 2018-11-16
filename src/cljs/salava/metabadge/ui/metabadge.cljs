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

(defn init-metabadge-icon [assertion-url data-atom]
  (ajax/GET
    (path-for (str "/obpv1/metabadge/badge/info"))
    {:params {:assertion_url assertion-url}
     :handler (fn [data] (reset! data-atom data))}))


(defn meta_icon [data-atom]
  (let [meta_badge (:meta_badge @data-atom)
        meta_badge_req (:meta_badge_req @data-atom)]
    (cond
      (and meta_badge meta_badge_req)  [:div.multi [:span {:title "This badge is part of a milestone badge"}
                                                    [:i.link-icon {:class "fa fa-sitemap"}]]
                                        [:span {:title "This badge is part of a milestone badge"} [:i.link-icon {:class "fa fa-puzzle-piece"}]]]
      meta_badge [:div {:title "This badge is a milestone badge"}[:span [:i.link-icon {:class "fa fa-sitemap"}]]]
      meta_badge_req [:div {:title "This badge is part of a milestone badge"} [:span [:i.link-icon {:class "fa fa-puzzle-piece"}]]]
      :else nil)))

(defn required-block-badges [m]
  (take 20 (shuffle (:required_badges m))))

(defn partition-count [m]
  (let [no (count (required-block-badges m) #_(:required_badges m))]
    (if (<= no 4) no (/ no 2))))

(defn badge-block [m]
  (let [{:keys [received name image criteria]} (:badge m)
        milestone-image-class (if received "" " not-received")]
    [:a {:href "#" :on-click #(mo/open-modal [:metabadge :metadata] m)}
     [:div.metabadge
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
                                            coll))) [:tbody] (partition-all (partition-count m) (required-block-badges m) #_(take 20 (shuffle (:required_badges m)))))]]]]]]]]]))


(defn metabadge-block [metabadge]
  (fn []
    (when-not (empty? metabadge)
      (into [:div#metabadge
             (for [m metabadge
                   :let [{:keys [milestone? badge]} m]]
               ^{:key m} [badge-block m])]))));)


(defn metabadge-link [assertion-url state]
  (fn []
    (let [metabadge (:metabadge @state)]
      (when-not (empty? (:metabadge @state))
        (reduce-kv
          (fn [r k v]
            (conj r (if (true? k)
                      ^{:key k}[:a {:href "#"
                                    :on-click #(mo/open-modal [:metabadge :metadata] (-> v first))}
                                [:div [:span [:i.link-icon {:class "fa fa-sitemap"}]]  (str "This badge is a milestone badge")]]
                      (if (empty? (rest v))
                        ^{:key k}  [:a {:href "#"
                                        :on-click #(mo/open-modal [:metabadge :metadata] (-> v first))}
                                    [:div [:span [:i.link-icon {:class "fa fa-puzzle-piece"}]]  "This badge is a part of a milestone badge"]]
                        ^{:key k}  [:a {:href "#"
                                        :on-click #(mo/open-modal [:metabadge :multiblock] v)}
                                    [:div [:span [:i.link-icon {:class "fa fa-puzzle-piece"}]] (str "This badge is a part of " (count v) " milestone badges")]]))))
          [:div.link-icon]
          (group-by :milestone? metabadge))))))

(defn metabadge-icon [assertion-url]
  (fn []
    (let [data-atom (atom {})]
      (init-metabadge-icon assertion-url data-atom)
      [meta_icon data-atom]
      )))

(defn metabadge [assertion-url]
  (fn []
    (let [state (atom {})]
      (init-metabadge-data assertion-url state)
      [metabadge-link assertion-url state]
      )))

