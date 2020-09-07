(ns salava.extra.spaces.ui.modal
  (:require
    [clojure.string :refer [blank? join split]]
    [reagent.core :refer [cursor atom]]
    [reagent-modals.modals :as m]
    [salava.core.ui.ajax-utils :as ajax]
    [salava.core.ui.modal :as mo]
    [salava.core.ui.helper :refer [path-for js-navigate-to current-route-path]]
    [salava.core.i18n :refer [t]]
    [salava.core.time :refer [date-from-unix-time iso8601-to-unix-time]]
    [salava.extra.spaces.ui.creator :as creator]
    [salava.extra.spaces.ui.helper :as sh :refer [button]]
    [salava.extra.spaces.ui.invitelink :refer [invite-link]]
    [salava.user.ui.helper :refer [profile-picture profile-link-inline-modal]]
    [reagent.session :as session]
    [salava.extra.spaces.ui.userlist :as ul]
    [salava.extra.spaces.ui.report :as report]
    [salava.extra.spaces.ui.message-tool :as mt]))

(defn check-membership [id state]
 (ajax/POST
  (path-for (str "/obpv1/spaces/check_membership/" id) true)
  {:handler (fn [data]
              (swap! state assoc :member_info data))}))

(defn init-member-list [id state]
  (ajax/POST
   (path-for (str "/obpv1/spaces/userlist/" id) true)
   {:handler (fn [data]
               (swap! state assoc :users data))}))

(defn init-data [id state]
  (ajax/GET
    (path-for (str "/obpv1/spaces/"id))
    {:handler (fn [data]
                (swap! state assoc :space data :member_info nil)
                (check-membership id state)
                (init-member-list id state))}))

(defn extend-subscription [state]
  (let [id @(cursor state [:space :id])
        valid_until @(cursor state [:space :valid_until])]
    (ajax/POST
          (path-for (str "/obpv1/spaces/extend/" id)) ;true)
          {:params {:valid_until  (iso8601-to-unix-time valid_until)}
           :handler (fn [data]
                      (when (= (:status data) "success")
                        (reset! (cursor state [:show-date-input]) false)
                        (init-data id state)))})))

(defn join-space [id state]
  (ajax/POST
   (path-for (str "/obpv1/spaces/user/join/" id) true)
   {:handler (fn [data]
               (when (= (:status data) "success")
                 (init-data id state)))}))

(defn leave-space [id state in-space?]
  (ajax/POST
   (path-for (str "/obpv1/spaces/user/leave/" id) true)
   {:params {:current-space in-space?}
    :handler (fn [data]
               (when (= (:status data) "success")
                 ;(check-membership id state)
                 (init-data id state)
                 (when in-space? (js-navigate-to (current-route-path)))))}))

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
  (let [{:keys [name description ctime status alias css visibility]} @(cursor state [:space])
        {:keys [p-color s-color t-color]} css]
    [:div {:style {:line-height "2.5"}}
      [space-banner state]
      [:h1.uppercase-header {:style {:word-break "break-word"}} name]
      [:div.well.well-sm ;{:style {:margin "10px 0"}}
       (case visibility
        "private" [:div [:i.fa.fa-user-secret.fa-lg] " " [:span [:b (t :extra-spaces/Privatespaceinfo2)]]]
        "controlled" [:div [:i.fa.fa-user-plus.fa-lg] " " [:span [:b (t :extra-spaces/Controlledspaceinfo)]]]
        "open" [:div [:i.fa.fa-unlock.fa-lg] " " [:span [:b  (t :extra-spaces/Openspaceinfo)]]]
        [:div])]
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

(defn- process-time [time]
 (if (string? time)
   time
   (let [t  (as-> (date-from-unix-time (* time 1000)) $
                  (split $ ".")
                  (reverse $))]
       (->> t (map #(if (= 1 (count %)) (str "0"%) %)) (join "-")))))

(defn manage-status [state]
 (let [validity-atom (cursor state [:space :valid_until])
       valid_until (if (number? @validity-atom) @validity-atom (iso8601-to-unix-time @validity-atom))]
  [:div#space-gallery.form-group
   [:div.panel.panel-default
    [:div.panel-heading.weighted
     (t :extra-spaces/Status)
     [:span.pull-right
      (when (and @(cursor state [:space :valid_until]) (= "active" @(cursor state [:space :status])))
        (str (t :extra-spaces/Subcriptionexpires) " " (sh/num-days-left valid_until) " " (t :badge/days)))
      (when (= "expired" @(cursor state [:space :status]))
        (t :extra-spaces/Subscriptionhasexpired))]]
    [:div.panel-body
     [:div.row
      [:div.col-md-12 {:style {:line-height "3"}}
       [:div.blob {:class @(cursor state [:space :status])}]
       [:span.weighted @(cursor state [:space :status])
        (when (and @(cursor state [:space :valid_until]) (= @(cursor state [:space :status]) "active"))
         (str " " (t :extra-spaces/until) " " (date-from-unix-time (* 1000 valid_until))))]
       (when-not @(cursor state [:delete])
         [:div.pull-right

          (case @(cursor state [:space :status])
            "active" [:div.btn-toolbar
                      [:div.btn-group
                       [button {:func #(reset! (cursor state [:show-date-input]) true) :name (t :extra-spaces/Extendsubscription) :type :primary}]
                       [button {:func #(update-status state "suspended") :name (t :extra-spaces/Suspend) :type :warning}]
                       [button {:func #(reset! (cursor state [:delete]) true) :name (t :core/Delete) :type :danger}]]]

             "suspended" [:div.btn-toolbar
                          [:div.btn-group
                           [button {:func #(update-status state "active") :name (t :extra-spaces/Activate) :type :primary}]
                           [button {:func #(reset! (cursor state [:show-date-input]) true) :name (t :extra-spaces/Extendsubscription) :type :primary}]
                           [button {:func #(reset! (cursor state [:delete]) true) :name (t :core/Delete) :type :danger}]]]
             "expired"  [:div.btn-toolbar
                         (when (pos? (sh/num-days-left  @(cursor state [:space :valid_until])))
                           [button {:func #(update-status state "active") :name (t :extra-spaces/Activate) :type :primary}])
                         [button {:func #(reset! (cursor state [:show-date-input]) true) :name (t :extra-spaces/Extendsubscription) :type :primary}]
                         [button {:func #(reset! (cursor state [:delete]) true) :name (t :core/Delete) :type :danger}]]

             "deleted"    [button {:func #(update-status state "active") :name (t :extra-spaces/Undodelete) :type :primary}]
             [:div])])]]

     (when @(cursor state [:delete])
       [:div {:style {:margin-top "15px"}}
         [:div.alert.alert-danger
          (if (> (:member_count @state) 1) (t :extra-spaces/Aboutdelete) (t :badge/Confirmdelete))]
         [:hr.line]
         [:div.btn-toolbar.text-center
          [:div.btn-group {:style {:float "unset"}}
           [button {:func #(reset! (cursor state [:delete]) false) :name (t :core/Cancel) :type :primary}]
           [button {:name (t :core/Delete) :func #(delete-space! state) :type :danger :data-dismiss "modal"}]]]])

     (when @(cursor state [:show-date-input])
      [:div.row
       [:div.col-md-12
        [:div.input-group
           [:input.form-control
            {:style {:max-width "unset"}
             :type "date"
             :id "date"
             :value (process-time @(cursor state [:space :valid_until]))
             :on-change #(do
                           (reset! (cursor state [:space :valid_until]) (.-target.value %)))}]
           [:span.input-group-btn
            [:button.btn.btn-primary.input-btn
             {:type "button"
              :style {:margin-top  "unset"}
              :on-click #(extend-subscription state)
              :disabled (number? @(cursor state [:space :valid_until]))}
             "OK"]]]]])]]]))


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

(defn manage-message-tool [state]
  [:div#space-gallery.form-group
   [:div.panel.panel-default
    [:div.panel-heading.weighted
     (t :extra-spaces/MessageTool)]
    [:div.panel-body
     [mt/manage-message-tool (get-in @state [:space :id] 0) state true]]
    [:div.panel-footer
     [:button.btn.btn-primary.btn-bulky
      {:on-click #(do
                   (.preventDefault %)
                   (reset! (cursor state [:message_setting_updating]) true)
                   (ajax/POST
                    (path-for (str "/obpv1/space/message_tool/settings/" (get-in @state [:space :id] 0)))
                    {:params {:settings (dissoc (assoc @(cursor state [:message_setting]) :issuers @(cursor state [:message_setting :enabled_issuers])) :enabled_issuers)}
                     :handler (fn [data]
                                (when (= "success" (:status data))
                                  (mt/init-message-tool-settings (get-in @state [:space :id] 0) state)
                                  (reset! (cursor state [:message_setting_updating]) false)))}))}


      [:span (when @(cursor state [:message_setting_updating]) [:i.fa.fa-spin.fa-cog.fa-lg]) " "(t :admin/Savechanges)]]]]])


(defn manage-space [state]
  [:div.row
   [manage-status state]
   (when-not (or (= "deleted" @(cursor state [:space :status])) (= "expired" @(cursor state [:space :status])))
    [:div
     [manage-visibility state nil]
     ;(when (= "private" @(cursor state [:space :visibility]))
     [invite-link (select-keys (:space @state) [:id :name :alias])]
     [manage-admins state]
     [manage-message-tool state]])])


(defn memberlist [state]
 [:div#space.user-list.modal-view
  [ul/grid-form state]
  [ul/user-list state]])

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
       [:li.nav-item {:class  (if (or (nil? (:tab-no @state)) (= 4 (:tab-no @state))) "active")}
        [:a.nav-link {:href "#" :on-click #(swap! state assoc :tab [manage-space state]  :tab-no 4)}
         [:div  [:i.nav-icon.fa.fa-cogs.fa-lg] (t :extra-spaces/Manage)]]]
       [:li.nav-item {:class  (if (or (nil? (:tab-no @state)) (= 5 (:tab-no @state))) "active")}
        [:a.nav-link {:class disable-link :href "#" :on-click #(swap! state assoc :tab [memberlist state] :tab-no 5)}
         [:div  [:i.nav-icon {:class "fa fa-users fa-lg"}] (t :extra-spaces/Members)]]]]]]))

(defn membership-btn [state]
 (let [current-space (session/get-in [:user :current-space])
       in-space? (= (:id current-space (:id @state)))]
   (when-not (= "private" (get-in @state [:space :visibility]))
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
                              (if in-space? (m/close-modal!))
                              (leave-space (:id @state) state in-space?))}
                (if (= "accepted"  @(cursor state [:member_info :status]))
                  (t :extra-spaces/Leavespace)
                  (t :extra-spaces/Cancelmembershiprequest))]])]]])))

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
           5 [memberlist state]
           [view-space state]))]]]])

(defn handler [params]
  (let [id (:id params)
        no-of-members (:member_count params)
        state (atom {:id id
                     :tab-no 1
                     :in-modal true
                     :member_count no-of-members
                     :new-admins []
                     :delete false
                     :show-date-input false
                     :role-selected []
                     :role-all true
                     :order "mtime"
                     :custom-field-filters {}
                     :message_setting {} ;{:messages_enabled false :issuers [] :selected [] :enabled_issuers []}
                     :search ""})]
    (init-data id state)
    (fn []
      [space-content state])))

(defn- add-or-remove [x coll]
   (if (some #(= x %) @coll)
     (reset! coll (->> @coll (remove #(= x %)) vec))
     (reset! coll (conj @coll x))))

(defn message-setting-modal [state]
  (let [issuers @(cursor state [:message_setting :issuers])
        selected (cursor state [:message_setting :selected])
        enabled-issuers (cursor state [:message_setting :enabled_issuers])
        id (or @(cursor state [:space :id]) 0)]

    (fn []
     (let [issuers @(cursor state [:message_setting :issuers])
           issuers (->> issuers (remove #(clojure.string/blank? (:issuer_name %)))
                                (filter #(re-find (re-pattern (str "(?i)" @(cursor state [:search]))) (:issuer_name %))))
           enabled-issuers (cursor state [:message_setting :enabled_issuers])]
       [:div.col-md-12
        [:div.well.well-sm
         ;[:p [:b "Select issuers whose badges can be used to send messages"]]
         [:input.form-control
           {:on-change #(reset! (cursor state [:search]) (.-target.value %))
            :type "text"
            :id "searchissuer"
            :placeholder (str (t :extra-spaces/Filter) "...")
            :style {:max-width "300px"}}]]
        [:div {:style {:max-height "700px" :overflow "auto"}}
         (reduce
          (fn [r i]
            (conj r
              [:li.list-group-item
               [:input
                {:style {:margin "0 5px"}
                 :type "checkbox"
                 :name (str "input-"(:issuer_name i))
                 :default-value (:issuer_name i)
                 :id (str "input-"(:issuer_name i))
                 :on-change #(add-or-remove (:issuer_name i) enabled-issuers)
                 :default-checked (some #(= (:issuer_name i)  %)  @enabled-issuers)}]
               (:issuer_name i)]))
          [:ul.list-group]
          issuers)]
        [:div.btn-toolbar.well.well-sm
         [:div.btn-group
          [:button.btn.btn-primary {:type "button"
                                    :on-click #(if (:in-modal @state) (mo/previous-view) (m/close-modal!))}
            (t :core/Continue)]
          [:button.btn.btn-warning
           {:type "button"
            :on-click #(reset! selected (->> @(cursor state [:message_setting :issuers]) (filterv :enabled)))}
           (t :core/Cancel)]]]]))))

(def ^:export modalroutes
  {:space {:info handler
           :badges report/badges-modal
           :message_setting message-setting-modal
           :badges-mt mt/badge-modal}})
