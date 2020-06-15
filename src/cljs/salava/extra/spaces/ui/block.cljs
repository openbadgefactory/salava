(ns salava.extra.spaces.ui.block
 (:require
  [salava.core.ui.ajax-utils :as ajax]
  [salava.core.ui.helper :refer [path-for js-navigate-to current-route-path navigate-to]]
  [salava.core.ui.layout :as layout]
  [salava.core.i18n :refer [t translate-text]]
  [reagent.session :as session]
  [reagent.core :refer [cursor atom]]
  [dommy.core :as dommy :refer-macros [sel sel1]]
  [salava.extra.spaces.ui.helper :refer [space-card]]))


(defn stylyze-buttons [btn-class color])
(defn stylyze-links [color]
  (doall
   (doseq [a (sel :a)]
      (dommy/set-style! a :color color))))

(defn stylyze []
  (let [{:keys [p-color s-color t-color]} (session/get-in [:user :current-space :css])]
   (doall
    [(dommy/set-style! (sel1 ".title-row") :background "none" :background-color p-color)
     (stylyze-buttons ".btn-default" p-color)
     (stylyze-links p-color)])))


(defn init-spaces [state]
  (ajax/POST
    (path-for (str "/obpv1/spaces/user") true)
    {:handler (fn [data]
                (swap! state assoc :spaces data
                                   :selected (session/get-in [:user :current-space :name] nil))
                (when-not (empty? (session/get-in [:user :current-space :css] nil))
                  (stylyze)))}))


(defn leave-organization [id state]
  [:a {:href "#" :on-click #(do (.preventDefault %)
                                (ajax/DELETE
                                 (path-for (str "/obpv1/spaces/user/leave/" id) true)
                                 {:handler (fn [data]
                                             (when (= "success" (:status data))
                                               (init-spaces state)))}))}
    [:i.fa.fa-user-times.fa-fw] (str " " (t :extra-spaces/Leaveorganization))])

(defn default-btn [space state]
 (let [{:keys [id default_space]} space]
   (if (pos? default_space)
     [:span [:i.fa.fa-bookmark.fa-fw] (t :extra-spaces/default)]
     [:a {:href "#" :on-click #(do
                                 (.preventDefault %)
                                 (ajax/POST
                                  (path-for (str "/obpv1/spaces/user/default/" id))
                                  {:handler (fn [data]
                                              (when (= "success" (:status data))
                                                 (init-spaces state)))}))}
       [:span [:i.fa.fa-bookmark.fa-fw] (t :extra-spaces/Setasdefault)]])))


(defn manage-spaces [state]
  (let [spaces (sort-by :name @(cursor state [:spaces]))]
   (if (seq spaces)
     [:div#badge-stats
      [:h1.uppercase-header (t :extra-spaces/Organizations)]
      [:p (t :extra-spaces/Spaceconnectioninfo)]
      [:div.expandable-block {:class "panel issuer-panel panel-default"}
       [:div.panel-heading
        [:h2 (str (t :extra-spaces/Organizations) " (" (count spaces) ")")]]
       [:div.panel-body
        [:table {:class "table"}
         [:thead
          [:tr
           [:th {:style {:display "none"}}  "Logo"]
           [:th {:style {:display "none"}} (t :badge/Name)]
           [:th {:style {:display "none"}} "role"]
           [:th {:style {:display "none"}} "Action"]]]
         (into [:tbody]
               (for [space spaces
                     :let [{:keys [space_id name logo role default_space status]} space]]
                 [:tr
                  [:td.name [:a {:href "#"
                                 :on-click #(do
                                              ;(mo/open-modal [:badge :issuer] id {:hide (fn [] (init-data state))})
                                              (.preventDefault %))}
                                (if logo [:img.badge-icon {:src (str "/" logo) :alt (str name " icon")}]
                                         [:span [:i.fa.fa-building.fa-3x {:style {:margin-right "10px"}}]]) name]]
                  [:td (if (= status "pending") [:span.label.label-info (t :extra-spaces/pendingmembership)] [:span.label {:class (if (= role "admin") "label-danger" "label-success")} (translate-text (str "extra-spaces/" role))])]
                  (when-not (= status "pending") [:td {:style {:min-width "150px"}} "" (default-btn space state)])
                  [:td.action "" (leave-organization space_id state)]]))]]]]

    [:div.well.well-sm (t :extra-spaces/Notjoinedanyorg)])))

(defn next-url [space]
  (let [current-path (current-route-path)
        admin? (= (:role space) "admin")]
    (if admin?
      current-path
      (if (clojure.string/starts-with? current-path "/space") "/social" current-path))))


(defn space-list []
 (let [state (atom {:selected  nil})
       user-id (session/get-in [:user :id])
       current-space (session/get-in [:user :current-space])]

  (init-spaces state)
  (fn []
   (let [spaces (->> @(cursor state [:spaces]) (remove #(= "pending" (:status %))))
         default-space (->> spaces (filter #(pos? (:default_space %))) first)]
     (when (seq spaces) #_(seq @(cursor state [:spaces]))
       [:div#space-list
        [:div.dropdown
         [:a.dropdown-toggle {:href "#"
                              :data-toggle "dropdown"
                              :role "button"
                              :aria-haspopup true
                              :aria-expanded false}
            [:div.selected-space
             [:img.space-img {:src (str "/" (or (:logo current-space) (:logo default-space))) :alt " "}]
             [:div.name (or @(cursor state [:selected]) (:name default-space) (t :extra-spaces/Switchorganization))] [:span.caret]]]
         (reduce
           (fn [r space]
            (let [selected? (= @(cursor state [:selected])  (:name space))]
             (conj r
               ^{:key (:space_id space)}[:li [:a {:href "#" :on-click #(do
                                                                         (reset! (cursor state [:selected]) (:name space))
                                                                         (ajax/POST
                                                                          (path-for (str "/obpv1/spaces/switch/" (:id space))true)
                                                                          {:handler (fn [data]
                                                                                      (js-navigate-to (next-url space)))}))}

                                                [:img.space-img {:src (str "/" (:logo space)) :alt " "}](if selected? [:b [:i (:name space)]] (:name space))]])))
          [:ul.dropdown-menu];.pull-left]
          (sort-by :name spaces))]])))))

(defn manage-spaces-handler [site-navi]
  (let [state (atom {})]
   (init-spaces state)
   (fn []
     (layout/default site-navi (manage-spaces state)))))
