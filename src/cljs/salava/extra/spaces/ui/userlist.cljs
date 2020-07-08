(ns salava.extra.spaces.ui.userlist
 (:require
  [reagent.session :as session]
  [reagent.core :refer [atom cursor]]
  [reagent-modals.modals :as m]
  [salava.core.ui.helper :refer [path-for unique-values plugin-fun]]
  [salava.core.ui.layout :as layout]
  [salava.core.ui.modal :as mo]
  [salava.core.ui.ajax-utils :as ajax]
  [salava.user.ui.helper :refer [profile-picture profile-link-inline-modal]]
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

(defn accept-member! [user-id state]
  (ajax/POST
   (path-for (str "/obpv1/spaces/accept/" (:id @state) "/" user-id) true)
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

(defn add-new-members [state]
 (let [status (cursor state [:populating-space])]
  (reset! status true)
  (ajax/POST
   (path-for (str "/obpv1/space/populate/"(:id @state)) true)
   {:params {:users (mapv :id (distinct @(cursor state [:new-members])))}
    :handler (fn [data]
               (when (= (:status data) "success")
                 (reset! status false)
                 (reset! (cursor state [:new-members]) [])
                 (init-data state)))})))

(defn order-radio-values []
  [{:value "mtime" :id "radio-date" :label (t :core/bydate)}
   {:value "name" :id "radio-name" :label (t :core/byname)}])

(defn custom-field-filters [field state]
  (into [:div]
    (for [f (plugin-fun (session/get :plugins) field "custom_field_filter_space")]
       (when f [f state :users nil])))) ;(:users @state)]))))

(defn grid-form [state]
 [:div#grid-filter.form-horizontal
  [g/grid-search-field (t :core/Search ":") "usersearch" (t :core/Searchbyname) :search state]
  [g/translated-grid-buttons  (t :extra-spaces/Role ":") (unique-values :role @(cursor state [:users]))  "role-selected" "role-all" state "extra-spaces"]
  [g/grid-radio-buttons (t :core/Order ":")  "order" (order-radio-values) :order state]
  [custom-field-filters "gender" state]])

(defn element-visible? [element state]
  (if (and
       (or (= (select-keys element (keys (:custom-field-filters @state))) (:custom-field-filters @state)))
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

     [:div.user-list-content [:table.table.table-responsive ;{:class "table" :summary (t :badge/Issuers)}
                              [:thead
                               [:tr
                                [:th {:style {:display "none"}}  "Logo"]
                                [:th {:style {:display "none"}} (t :badge/Name)]
                                [:th {:style {:display "none"}} "role"]
                                [:th {:style {:display "none"}} "status"]
                                [:th {:style {:display "none"}} "Action"]]]
                              (into [:tbody]
                                (for [user users
                                      :let [{:keys [id profile_picture first_name last_name role space_id status]} user
                                            name (str first_name " " last_name)
                                            member-admin? (= role "admin")
                                            pending-member? (= "pending" status)
                                            member? (= "accepted" status)]]
                                  (when (element-visible? user state)
                                    [:tr
                                      [:td  [:img {:style {:width "40px" :height "40px"} :alt "" :src (profile-picture profile_picture)}]]
                                      [:td.text-center {:style {:vertical-align "middle"}} [:a {:href "#" :on-click #(do
                                                                                                                       (.preventDefault %)
                                                                                                                       (mo/open-modal [:profile :view] {:user-id id}))}
                                                                                            name]]
                                      [:td {:style {:vertical-align "middle"}} (when member-admin? [:span.label.label-danger (t :extra-spaces/admin)])]
                                      [:td {:style {:vertical-align "middle"}} (when pending-member? [:span.label.label-info (t :extra-spaces/pendingmembership)])]
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
                                                         (if pending-member? (conj $ [:li [:a {:href "#" :on-click #(accept-member! id state)} (t :extra-spaces/Acceptrequest)]]) (conj $ nil))
                                                         (if pending-member? (conj $ [:li [:a {:href "#" :on-click #(remove-member! id state)} (t :extra-spaces/Declinerequest)]]) (conj $ nil))
                                                         (if (and member? (not= 1 (count users))) (conj $ [:li [:a {:href "#" :on-click #(remove-member! id state)} (t :extra-spaces/Removemember)]]) (conj $ nil))
                                                         (if (and member? member-admin?) (conj $ [:li [:a {:href "#" :on-click #(downgrade-member! id state)} (t :extra-spaces/Downgradetomember)]]) (conj $ nil))
                                                         (if (and member? (not member-admin?)) (conj $ [:li [:a {:href "#" :on-click #(upgrade-member! id state)} (t :extra-spaces/Upgradetoadmin)]]) (conj $ nil)))]]])))]]
     (when (= "admin" (session/get-in [:user :role] "user"))
      [:div.panel-footer
         [:a {:href "#"
              :on-click #(do
                           (.preventDefault %)
                           (mo/open-modal [:gallery :profiles]
                            {:type "pickable"
                             :selected-users-atom (cursor state [:new-members])
                             :existing-users-atom (cursor state [:users])
                             :context "space_members_modal"
                             :space-id (:id @state)}))}

          [:span [:i.fa.fa-user-plus.fa-fw.fa-lg {:style {:vertical-align "baseline"}}] " " (t :extra-spaces/Addmembers)]]
       [:div {:style {:margin "10px 0"}}
        (if @(cursor state [:populating-space])
         [:div
          [:i.fa.fa-spinner.fa-pulse.fa-2x.fa-fw] [:span " " (t :extra-spaces/Populatingspace) "..."]
          [:span.sr-only (t :extra-spaces/Populatingspace)]]

         (reduce (fn [r u]
                   (let [{:keys [id first_name last_name profile_picture]} u]
                     (conj r [:div.user-item [profile-link-inline-modal id first_name last_name profile_picture]
                              [:a {:href "#" :on-click (fn [] (reset! (cursor state [:new-members]) (->> @(cursor state [:new-members]) (remove #(= id (:id %))) vec)))}
                               [:span.close {:aria-hidden "true" :dangerouslySetInnerHTML {:__html "&times;"}}]]])))
                 [:div#social-tab.selected-users-container] (distinct @(cursor state [:new-members]))))]

       (when (seq @(cursor state [:new-members]))
        [:div.btn-toolbar
         [:div.btn-group
          [:button.btn-primary.btn.btn-bulky
           {:type "button"
            :on-click #(do
                         (.preventDefault %)
                         (add-new-members state))
            :disabled (empty? @(cursor state [:new-members]))}
           (t :core/Add)]
          [:button.btn-danger.btn.btn-bulky
           {:type "button"
            :on-click #(do
                         (.preventDefault %)
                         (reset! (cursor state [:new-members]) [])
                         (reset! (cursor state [:populating-space]) false))}
           (t :core/Cancel)]]])])]]]))




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
                    :order "mtime"
                    :custom-field-filters {}
                    :new-members []
                    :populating-space false})]

   (init-data state)
   (fn []
    (layout/default site-navi [content state]))))
