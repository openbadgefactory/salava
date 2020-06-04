(ns salava.extra.spaces.ui.block
 (:require
  [salava.core.ui.ajax-utils :as ajax]
  [salava.core.ui.helper :refer [path-for]]
  [salava.core.ui.layout :as layout]
  [salava.core.i18n :refer [t translate-text]]
  [reagent.session :as session]
  [reagent.core :refer [cursor atom]]))

(defn init-spaces [state]
  (ajax/POST
    (path-for (str "/obpv1/spaces/user") true)
    {:handler (fn [data] (swap! state assoc :spaces data
                                            :selected nil))}))

(defn leave-organization [id state]
  [:a {:href "#" :on-click #(do (.preventDefault %)
                                (ajax/DELETE
                                 (path-for (str "/obpv1/spaces/user/leave/" id) true)
                                 {:handler (fn [data]
                                             (when (= "success" (:status data))
                                               (init-spaces state)))}))}
    [:i.fa.fa-user-times.fa-fw] (str " " (t :extra-spaces/Leaveorganization))])

(defn manage-spaces [state]
  (let [spaces (sort-by :name @(cursor state [:spaces]))]
   [:div#badge-stats
    [:p (t :extra-spaces/Spaceconnectioninfo)]
    [:div.expandable-block {:class "panel issuer-panel"}
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
                   :let [{:keys [space_id name logo role]} space]]
               [:tr
                [:td.name [:a {:href "#"
                               :on-click #(do
                                            ;(mo/open-modal [:badge :issuer] id {:hide (fn [] (init-data state))})
                                            (.preventDefault %))}
                              (if logo [:img.badge-icon {:src (str "/" logo) :alt (str name " icon")}]
                                       [:span [:i.fa.fa-building.fa-3x {:style {:margin-right "10px"}}]]) name]]
                [:td [:span.label {:class (if (= role "admin") "label-danger" "label-success")} (translate-text (str "extra-spaces/" role))]]
                [:td.action "" (leave-organization space_id state)]]))]]]]))


(defn space-list []
 (let [state (atom {:selected nil})
       user-id (session/get-in [:user :id])]

  (init-spaces state)

  (fn []
   (when (seq @(cursor state [:spaces]))
     [:div#space-list
      [:div.dropdown
       [:a.dropdown-toggle {:href "#"
                            :data-toggle "dropdown"
                            :role "button"
                            :aria-haspopup true
                            :aria-expanded false}
          (or @(cursor state [:selected]) (t :extra-spaces/Switchorganization))[:span.caret]]
       (reduce
         (fn [r space]
          (let [selected? (= @(cursor state [:selected]) (:name space))]
           (conj r
             ^{:key (:space_id space)}[:li [:a {:href "#" :on-click #(do
                                                                       (reset! (cursor state [:selected]) (:name space)))}
                                              (if selected? [:b [:i (:name space)]] (:name space))]])))
        [:ul.dropdown-menu.pull-left]
        (sort-by :name @(cursor state [:spaces])))]]))))

(defn manage-spaces-handler [site-navi]
  (let [state (atom {})]
   (init-spaces state)
   (fn []
     (layout/default site-navi (manage-spaces state)))))
