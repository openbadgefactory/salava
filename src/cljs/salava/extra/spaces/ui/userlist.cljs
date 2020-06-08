(ns salava.extra.spaces.ui.userlist
 (:require
  [reagent.session :as session]
  [reagent.core :refer [atom cursor]]
  [reagent-modals.modals :as m]
  [salava.core.ui.helper :refer [path-for unique-values]]
  [salava.core.ui.layout :as layout]
  [salava.core.ui.modal :as mo]
  [salava.core.ui.ajax-utils :as ajax]
  [salava.user.ui.helper :refer [profile-picture]]
  [salava.core.i18n :refer [t]]
  [salava.core.ui.grid :as g]))

(defn init-data [state]
 (let [id @(cursor state [:id])]
   (ajax/POST
    (path-for (str "/obpv1/spaces/userlist/" id) true)
    {:handler (fn [data]
                (swap! state assoc :users data))})))

(defn downgrade-member! [admin-id state]
  (ajax/POST
    (path-for (str "/obpv1/spaces/downgrade/" (:id @state) "/" admin-id) true)
    {:handler (fn [data]
                (when (= (:status data) "success")
                  (init-data state)))}))

(defn remove-member! [user-id state]
  (ajax/POST
    (path-for (str "/obpv1/spaces/remove_user/" (:id @state) "/" user-id) true)
    {:handler (fn [data]
                (when (= (:status data) "success")
                  (init-data state)))}))

(defn upgrade-member! [user-id state]
  (ajax/POST
    (path-for (str "/obpv1/spaces/upgrade/" (:id @state) "/" user-id) true)
    {:handler (fn [data]
                (when (= (:status data) "success")
                  (init-data state)))}))

(defn order-radio-values []
  [{:value "mtime" :id "radio-date" :label (t :core/bydate)}
   {:value "name" :id "radio-name" :label (t :core/byname)}])

(defn grid-form [state]
 [:div#grid-filter.form-horizontal
  [g/grid-search-field (t :core/Search ":") "usersearch" (t :core/Searchbyname) :search state]
  [g/translated-grid-buttons  (t :extra-spaces/Role ":") (unique-values :role @(cursor state [:users]))  "role-selected" "role-all" state "extra-spaces"]
  [g/grid-radio-buttons (t :core/Order ":")  "order" (order-radio-values) :order state]])

(defn element-visible? [element state]
  (if (and
       (or (> (count
               (clojure.set/intersection
                (into #{} (:role-selected @state))
                #{(:role element)}))
                ;(into #{} (:status element))))
              0)
           (= (:role-all @state) true))
       (or (empty? (:search @state))
           (not= (.indexOf
                  (.toLowerCase (str (:first_name element) " " (:last_name element)))
                  (.toLowerCase (:search @state)))
                 -1)))
    true false))

(defn user-list [state]
 (let [{:keys [name member_count role id]} @(cursor state [:space])
       users  @(cursor state [:users])
       admins (filter #(= (:role %) "admin") users)
       order (keyword  @(cursor state [:order]))
       users (case order
               (:name) (sort-by #(str (:first_name %) " " (:last_name %)) users);(sort-by (comp upper-case str order) users)
               (:mtime) (sort-by order > users)
               (sort-by order > users))]
  [:div
   [:div.form-group
    [:div.panel.panel-default
     [:div.panel-heading.weighted]

     [:table {:class "table" :summary (t :badge/Issuers)}
      [:thead
       [:tr
        [:th {:style {:display "none"}}  "Logo"]
        [:th {:style {:display "none"}} (t :badge/Name)]
        [:th {:style {:display "none"}} "Action"]]]
      (into [:tbody]
        (for [user users
              :let [{:keys [id profile_picture first_name last_name role space_id]} user
                    name (str first_name " " last_name)
                    member-admin? (= role "admin")]]
          (when (element-visible? user state)
            [:tr
              [:td  [:img {:style {:width "40px" :height "40px"} :alt "" :src (profile-picture profile_picture)}]]
              [:td.text-center {:style {:vertical-align "middle"}} [:a {:href "#" :on-click #(do
                                                                                               (.preventDefault %)
                                                                                               (mo/open-modal [:profile :view] {:user-id id}))}
                                                                    name]]
              [:td {:style {:vertical-align "middle"}} (when member-admin? [:span.label.label-info (t :extra-spaces/admin)])]
              [:td {:style {:text-align "end"}}
               [:div.btn-group
                 [:button.btn-primary.btn.btn-bulky.dropdown-toggle
                  {:type "button"
                   :data-toggle "dropdown"
                   :aria-haspopup true
                   :aria-expanded false
                   :disabled (and (= 1 (count users)) (= id (session/get-in [:user :id])))}
                  (t :extra-spaces/Manage) " " [:span.caret]]
                ^{:key id} (as-> [:ul.dropdown-menu] $
                                 (if-not (= 1 (count users)) (conj $ [:li [:a {:href "#" :on-click #(remove-member! id state)} (t :extra-spaces/Removemember)]]) (conj $ nil))
                                 (if member-admin? (conj $ [:li [:a {:href "#" :on-click #(downgrade-member! id state)} (t :extra-spaces/Downgradetomember)]]) (conj $ nil))
                                 (if-not member-admin? (conj $ [:li [:a {:href "#" :on-click #(upgrade-member! id state)} (t :extra-spaces/Upgradetoadmin)]]) (conj $ nil)))]]])))]]]]))

(defn content [state]
 [:div#space.user-list
  [m/modal-window]
  [grid-form state]
  [user-list state]])

(defn handler [site-navi]
 (let [current-space (session/get-in [:user :current-space])
       state (atom {:space current-space
                    :id (:id current-space)
                    :role-selected []
                    :role-all true
                    :order "mtime"})]

   (init-data state)
   (fn []
    (layout/default site-navi [content state]))))
