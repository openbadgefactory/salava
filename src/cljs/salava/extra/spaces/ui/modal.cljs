(ns salava.extra.spaces.ui.modal
  (:require
    [clojure.string :refer [blank?]]
    [reagent.core :refer [cursor atom]]
    [reagent-modals.modals :as m]
    [salava.core.ui.ajax-utils :as ajax]
    [salava.core.ui.modal :as mo]
    [salava.core.ui.helper :refer [path-for]]
    [salava.core.i18n :refer [t]]
    [salava.core.time :refer [date-from-unix-time]]
    [salava.extra.spaces.ui.creator :as creator]
    [salava.user.ui.helper :refer [profile-picture profile-link-inline-modal]]
    [reagent.session :as session]))

(defn check-membership [id state]
 (ajax/POST
  (path-for (str "/obpv1/spaces/check_membership/" id) true)
  {:handler (fn [data]
              (prn data "sadsadasdasdada")
              (swap! state assoc :member_info data))}))

(defn init-data [id state]
  (ajax/GET
    (path-for (str "/obpv1/spaces/"id))
    {:handler (fn [data]
                (swap! state assoc :space data :member_info nil)
                (check-membership id state))}))

(defn join-space [id state]
  (ajax/POST
   (path-for (str "/obpv1/spaces/user/join/" id) true)
   {:handler (fn [data]
               (when (= (:status data) "success")
                 (init-data id state)))}))

(defn leave-space [id state]
  (ajax/POST
   (path-for (str "/obpv1/spaces/user/leave/" id) true)
   {:handler (fn [data]
               (when (= (:status data) "success")
                 ;(check-membership id state)
                 (init-data id state)))}))

(defn delete-space! [state]
  (ajax/DELETE
    (path-for (str "/obpv1/spaces/delete/" (:id @state)) true)
    {:handler (fn [data]
                (when (= (:status data) "success")
                  (m/close-modal!)))}))

(defn downgrade-member! [admin-id state]
  (ajax/POST
    (path-for (str "/obpv1/spaces/downgrade/" (:id @state) "/" admin-id) true)
    {:handler (fn [data]
                (when (= (:status data) "success")
                  (init-data (:id @state) state)))}))

(defn add-admin [state]
 (ajax/POST
  (path-for (str "/obpv1/spaces/add_admin/" (:id @state)) true)
  {:params {:admins (map :id @(cursor state [:new-admins]))}
   :handler (fn [data]
              (when (= "success" (:status data))
                (init-data (:id @state) state)
                (reset! (cursor state [:new-admins]) [])))}))

(defn update-status [state status]
  (ajax/POST
   (path-for (str "/obpv1/spaces/update_status/" (:id @state)) true)
   {:params {:status status}
    :handler (fn [data]
               (when (= (:status data) "success")
                 (init-data (:id @state) state)))}))

(defn set-visibility [v state init-fn]
  (ajax/POST
   (path-for (str "/obpv1/spaces/update_visibility/" (:id @state)) true)
   {:params {:visibility v}
    :handler (fn [data]
               (when (= (:status data) "success")
                (if init-fn
                  (init-fn)
                  (init-data (:id @state) state))))}))

(defn space-logo [state]
  (let [{:keys [logo name]} @(cursor state [:space])
        {:keys [role status]} @(cursor state [:member_info])]
    [:div.text-center {:class "col-md-3" :style {:margin-bottom "20px"}}
     [:div
      (if-not (blank? logo)
        [:img.space-img {:src (if (re-find #"^data:image" logo)
                                  logo
                                 (str "/" logo))
                         :alt name}]
        [:i.fa.fa-building-o.fa-5x {:style {:margin-top "10px"}}])]
     (when role
       [:div {:style {:margin "5px auto"}}
        (if (and status (= status "accepted"))
         (if (= "admin" role)
           [:span.label.label-danger (t :extra-spaces/admin)]
           [:span.label.label-success (t :extra-spaces/member)])
         [:span.label.label-info (t :extra-spaces/pendingmembership)])])]))

(defn space-banner [state]
  (let [{:keys [banner]} @(cursor state [:space])]
    (when banner
      [:div.space-banner-container {:style {:max-width "640px" :max-height "120px"}}
        [:img {:src (if (re-find #"^data:image" banner)
                        banner
                        (str "/" banner))}]])))

(defn view-space [state]
  (let [{:keys [name description ctime status alias css]} @(cursor state [:space])
        {:keys [p-color s-color t-color]} css]
    [:div {:style {:line-height "2.5"}}
      [space-banner state]
      [:h1.uppercase-header {:style {:word-break "break-word"}} name]
      [:p [:b description]]
      [:div [:span._label (str (t :extra-spaces/Alias) ":  ")] alias]
      [:div [:span._label (str (t :extra-spaces/createdon) ":  ")] (date-from-unix-time (* 1000 ctime))]
      [:div [:span._label (str (t :extra-spaces/Status) ": ")] status]
      (when (and css (= "admin" (session/get-in [:user :role])))
        [:div
          [:div [:span._label (str (t :extra-spaces/Primarycolor) ":  ")] [:span.color-span {:style {:background-color p-color}}]]
          (when s-color [:div [:span._label (str (t :extra-spaces/Secondarycolor) ":  ")] [:span.color-span {:style {:background-color s-color}}]])
          (when t-color [:div [:span._label (str (t :extra-spaces/Tertiarycolor) ":  ")] [:span.color-span {:style {:background-color t-color}}]])])]))

(defn edit-space [state]
   [creator/modal-content state])

(defn delete-space-content [state]
  [:div.row
    (when (> (:member_count @state) 1) [:p [:b (t :extra-spaces/Aboutdelete)]])
    [:div.alert.alert-danger
     (t :badge/Confirmdelete)]
    [:hr.line]
    [:div.btn-toolbar
     [:div.btn-group
      [:button.btn.btn-primary
       {:type "button"
        :aria-label (t :core/Cancel)
        :on-click #(do
                     (.preventDefault %)
                     (swap! state assoc :tab nil :tab-no 1))}
       (t :core/Cancel)]
      [:button.btn.btn-danger
       {:type "button"
        :on-click #(do
                     (.preventDefault %)
                     (delete-space! state))
        :data-dismiss "modal"}
       (t :core/Delete)]]]])

(defn manage-admins [state]
  [:div.form-group
   [:div.panel.panel-default
    [:div.panel-heading.weighted
     (t :extra-spaces/Admins)]

    [:table {:class "table" :summary (t :badge/Issuers)}
     [:thead
      [:tr
       [:th {:style {:display "none"}}  "Logo"]
       [:th {:style {:display "none"}} (t :badge/Name)]
       [:th {:style {:display "none"}} "Action"]]]
     (into [:tbody]
       (for [admin @(cursor state [:space :admins])
             :let [{:keys [id profile_picture first_name last_name]} admin
                   name (str first_name " " last_name)]]
         [:tr
           [:td  [:img {:style {:width "40px" :height "40px"} :alt "" :src (profile-picture profile_picture)}]]
           [:td.text-center {:style {:vertical-align "middle"}} [:a {:href "#" :on-click #(do
                                                                                            (.preventDefault %)
                                                                                            (mo/open-modal [:profile :view] {:user-id id}))}
                                                                 name]]
           [:td {:style {:vertical-align "middle"}} (when (= id @(cursor state [:space :last_modified_by])) [:span.label.label-info (t :extra-spaces/lastmodifier)])]
           [:td {:style {:text-align "end"}} [:button.btn.btn-primary.btn-bulky
                                               {:on-click #(do
                                                             (.preventDefault %)
                                                             (downgrade-member! id state))
                                                :disabled (= 1 (count @(cursor state [:space :admins])))}
                                               (t :extra-spaces/Downgradetomember)]]]))]
    [:hr.line]
    [:div#social-tab {:style {:background-color "ghostwhite" :padding "8px"}}
     #_[:span._label (t :extra-spaces/Admins)]
     #_[:p (t :extra-space/Aboutadmins)]
     [:div
       [:a {:href "#"
            :on-click #(do
                         (.preventDefault %)
                         (mo/open-modal [:gallery :profiles]
                          {:type "pickable"
                           :selected-users-atom (cursor state [:new-admins])
                           :existing-users-atom (cursor state [:space :admins])
                           :context "space_admins_modal"} {}))}

        [:span [:i.fa.fa-user-plus.fa-fw.fa-lg {:style {:vertical-align "baseline"}}] " " (t :extra-spaces/Addadmins)]]]
     (reduce (fn [r u]
               (let [{:keys [id first_name last_name profile_picture]} u]
                 (conj r [:div.user-item [profile-link-inline-modal id first_name last_name profile_picture]
                          [:a {:href "#" :on-click (fn [] (reset! (cursor state [:new-admins]) (->> @(cursor state [:new-admins]) (remove #(= id (:id %))) vec)))}
                           [:span.close {:aria-hidden "true" :dangerouslySetInnerHTML {:__html "&times;"}}]]])))
             [:div.selected-users-container] @(cursor state [:new-admins]))
     (when (seq @(cursor state [:new-admins]))
      [:div.btn-toolbar
       [:div.btn-group
        [:button.btn-primary.btn.btn-bulky
         {:type "button"
          :on-click #(do
                       (.preventDefault %)
                       (add-admin state))
          :disabled (empty? @(cursor state [:new-admins]))}
         (t :core/Add)]
        [:button.btn-danger.btn.btn-bulky
         {:type "button"
          :on-click #(do
                       (.preventDefault %)
                       (reset! (cursor state [:new-admins]) []))}
         (t :core/Delete)]]])]]])

(defn manage-status [state]
  [:div#space-gallery.form-group
   [:div.panel.panel-default
    [:div.panel-heading.weighted
     (t :extra-spaces/Status)]
    [:div.panel-body
     [:div.row
      [:div.col-md-12 {:style {:line-height "3"}}
       [:div.blob {:class @(cursor state [:space :status])}] [:span.weighted @(cursor state [:space :status])]
       [:div.pull-right
        (case @(cursor state [:space :status])
          "active" [:div.btn-toolbar
                    [:div.btn-group
                     [:button.btn.btn-warning.btn-bulky
                      {:on-click #(do
                                    (.preventDefault %)
                                    (update-status state "suspended"))
                       :role "button"
                       :aria-label (t :extra-spaces/Suspend)}
                      (t :extra-spaces/Suspend)]
                     [:button.btn.btn-danger.btn-bulky
                      {:on-click #(do
                                    (.preventDefault %)
                                    (reset! (cursor state [:delete]) true))
                                    ;(delete-space! state))
                       :type "button"
                       :aria-label (t :core/Delete)}
                      (t :core/Delete)]]]
           "suspended" [:div.btn-toolbar
                        [:div.btn-group
                         [:button.btn.btn-primary.btn-bulky
                          {:on-click #(do
                                        (.preventDefault %)
                                        (update-status state "active"))
                           :type "button"
                           :aria-label (t :extra-spaces/Activate)}
                          (t :extra-spaces/Activate)]
                         [:button.btn.btn-danger.btn-bulky
                          {:on-click #(do
                                        (.preventDefault %)
                                        (reset! (cursor state [:delete]) true))
                                        ;(delete-space! state))
                           :type "button"
                           :aria-label (t :core/Delete)}
                          (t :core/Delete)]]]
           "deleted"    [:button.btn.btn-primary.btn-bulky
                          {:on-click #(do
                                        (.preventDefault %)
                                        (update-status state "active"))
                           :type "button"
                           :aria-label (t :extra-spaces/Undodelete)}
                         (t :extra-spaces/Undodelete)]
           [:div])]]]
     (when @(cursor state [:delete])
       [:div;.row
         (when (> (:member_count @state) 1) [:p [:b (t :extra-spaces/Aboutdelete)]])
         [:div.alert.alert-danger
          (t :badge/Confirmdelete)]
         [:hr.line]
         [:div.btn-toolbar.text-center
          [:div.btn-group {:style {:float "unset"}}
           [:button.btn.btn-primary
            {:type "button"
             :aria-label (t :core/Cancel)
             :on-click #(do
                          (.preventDefault %)
                          (reset! (cursor state [:delete]) false))}
            (t :core/Cancel)]
           [:button.btn.btn-danger
            {:type "button"
             :on-click #(do
                          (.preventDefault %)
                          (delete-space! state))
             :data-dismiss "modal"}
            (t :core/Delete)]]]])]]])


(defn visibility-form [state init-fn]
 (let [site-name (session/get :site-name)
       vatom (cursor state [:space :visibility])]
  [:div.row
   [:div.col-md-12
    [:div
     [:div
      [:p [:b (t :extra-spaces/Aboutmembershipsetting)]]
      [:div.visibility-opts-group
       [:div.visibility-opt
         [:input.radio-btn {:id "private"
                            :type "radio"
                            :name "private"
                            :on-change #(do
                                          (.preventDefault %)
                                          (set-visibility "private" state init-fn))
                            :checked (= "private" @vatom)}]
         [:div.radio-tile
          [:div.icon [:i.fa.fa-user-secret]]
          [:label.radio-tile-label {:for "private"} (t :extra-spaces/Privatespace)]]]
       [:div.visibility-opt
         [:input.radio-btn {:id "controlled"
                            :type "radio"
                            :name "controlled"
                            :on-change #(do
                                          (.preventDefault %)
                                          (set-visibility "controlled" state init-fn))
                            :checked (= "controlled" @vatom)}]
         [:div.radio-tile
          [:div.icon [:i.fa.fa-user-plus]]
          [:label.radio-tile-label {:for "controlled"} (t :extra-spaces/Controlledspace)]]]
       [:div.visibility-opt
         [:input.radio-btn {:id "open"
                            :type "radio"
                            :name "open"
                            :on-change #(do
                                          (.preventDefault %)
                                          (set-visibility "open" state init-fn))

                            :checked (= "open" @vatom)}]
         [:div.radio-tile
          [:div.icon [:i.fa.fa-unlock]]
          [:label.radio-tile-label {:for "open"} (t :extra-spaces/Openspace)]]]]
      [:div {:style {:margin "10px auto"}}
       (case @vatom
         "private" [:div
                    [:ul
                     [:li (t :extra-spaces/Privatespaceinfo1)]
                     [:li [:b (t :extra-spaces/Privatespaceinfo2)]]]]

         "controlled" [:div
                       [:ul
                        [:li (t :extra-spaces/Visiblespace)]
                        [:li [:b (t :extra-spaces/Controlledspaceinfo)]]]]

         "open"  [:div
                      [:ul
                       [:li (t :extra-spaces/Visiblespace)]
                       [:li [:b (t :extra-spaces/Openspaceinfo)]]]]
         [:div])]]]]]))

(defn manage-visibility [state init-fn]
  [:div.panel.panel-default
   [:div.panel-heading.weighted
    (t :extra-spaces/Membership)]
   [:div.panel-body
    [visibility-form state init-fn]]])

(defn manage-space [state]
  [:div.row
   [manage-status state]
   [manage-visibility state nil]
   [manage-admins state]])

(defn space-navi [state]
 (let [disable-link (when (= "deleted" @(cursor state [:space :status])) "btn disabled")]
  [:div.row.flip-table
   [:div.col-md-3]
   [:div.col-md-9
     [:ul {:class "nav nav-tabs wrap-grid"}
       [:li.nav-item {:class  (if (or (nil? (:tab-no @state)) (= 1 (:tab-no @state))) "active")}
        [:a.nav-link {:href "#" :on-click #(swap! state assoc :tab [view-space state] :tab-no 1)}
         [:div  [:i.nav-icon.fa.fa-info-circle.fa-lg] (t :metabadge/Info)]]]
       [:li.nav-item {:class  (if (or (nil? (:tab-no @state)) (= 2 (:tab-no @state))) "active")}
        [:a.nav-link {:class disable-link :href "#" :on-click #(swap! state assoc :tab [edit-space state]  :tab-no 2)}
         [:div  [:i.nav-icon.fa.fa-edit.fa-lg] (t :extra-spaces/Edit)]]]
       #_[:li.nav-item {:class  (if (or (nil? (:tab-no @state)) (= 3 (:tab-no @state))) "active")}
          [:a.nav-link {:href "#" :on-click #(swap! state assoc :tab [edit-space state]  :tab-no 3)}
           [:div  [:i.nav-icon.fa.fa-cog.fa-lg] (t :extra-spaces/Manage)]]]
       [:li.nav-item {:class  (if (or (nil? (:tab-no @state)) (= 4 (:tab-no @state))) "active")}
        [:a.nav-link {:href "#" :on-click #(swap! state assoc :tab [manage-space state]  :tab-no 4)}
         [:div  [:i.nav-icon.fa.fa-cogs.fa-lg] (t :extra-spaces/Manage)]]]
       #_[:li.nav-item {:class  (if (or (nil? (:tab-no @state)) (= 5 (:tab-no @state))) "active")}
          [:a.nav-link {:class disable-link :href "#" :on-click #(swap! state assoc :tab [delete-space-content state] :tab-no 5)}
           [:div  [:i.nav-icon {:class "fa fa-trash fa-lg"}] (t :core/Delete)]]]]]]))

(defn membership-btn [state]
  ;(fn []
   [:div.row
    [:div.col-md-12
     [:div.pull-right
       (if (nil? @(cursor state [:member_info]))
          [:div
           [:button.btn.btn-primary.btn-bulky
                {:type "button"
                 :on-click #(do
                              (.preventDefault %)
                              (join-space (:id @state) state))}
             (t :extra-spaces/Joinspace)]]

          [:div
           [:button.btn.btn-danger.btn-danger
            {:type "button"
             :on-click #(do
                          (.preventDefault %)
                          (leave-space (:id @state) state))}
            (if (= "accepted"  @(cursor state [:member_info :status]))
              (t :extra-spaces/Leavespace)
              (t :extra-space/Cancelmembershiprequest))]])]]])




(defn space-content [state]
  [:div#space
   (membership-btn state)
   (when (= "admin" (session/get-in [:user :role]))
     [space-navi state])
   [:div.col-md-12
    [space-logo state]
    [:div.col-md-9
     [:div {:style {:margin "10px 0"}}
       (or
         (:tab @state)
         (case (:tab-no @state)
           2 [edit-space state]
           4 [manage-space state]
           ;5 [delete-space-content state]
           [view-space state]))]]]])

(defn handler [params]
  (let [id (:id params)
        no-of-members (:member_count params)
        state (atom {:id id
                     :tab-no 1
                     :in-modal true
                     :member_count no-of-members
                     :new-admins []
                     :delete false})]
    (init-data id state)
    (fn []
      [space-content state])))

(def ^:export modalroutes
  {:space {:info handler}})
