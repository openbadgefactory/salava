(ns salava.badgeIssuer.ui.modal
  (:require
   [clojure.string :refer [blank?]]
   [reagent.core :refer [atom cursor create-class]]
   [reagent-modals.modals :as m]
   [reagent.session :as session]
   [salava.badgeIssuer.ui.block :as block]
   [salava.badgeIssuer.ui.creator :as creator]
   [salava.badgeIssuer.ui.helper :refer [badge-content badge-image profile-link-inline md->html add-evidence-block]]
   [salava.badgeIssuer.ui.util :refer [toggle-setting delete-selfie-badge issue-selfie-badge issuing-history revoke-badge-content]]
   [salava.core.i18n :refer [t]]
   [salava.core.ui.helper :refer [plugin-fun js-navigate-to navigate-to]]
   [salava.core.ui.modal :as mo]
   [salava.core.ui.popover :refer [info]]
   [salava.core.time :refer [date-from-unix-time]]
   [salava.user.ui.helper :refer [profile-picture profile-link-inline-modal]]))

(defn preview-selfie [badge]
  (let [{:keys [name description tags criteria image]} badge]
    [:div {:id "badge-info" :class "row flip" :style {:margin "10px 0"}}
     [badge-image badge]
     [:div {:class "col-md-9 badge-info view-tab" :style {:display "block"}}
      [badge-content badge]]]))

(defn delete-selfie-content [state]
  (let [{:keys [name image id]} (:badge @state)]
    [:div {:id "badge-info" :class "row flip" :style {:margin "10px 0"}}
     [badge-image (:badge @state)]
     [:div {:class "col-md-9 badge-info view-tab" :style {:display "block"}}
      [:div {:class "alert alert-warning" :style {:margin "20px 0"}}
       (t :badge/Confirmdelete)]
      [:div
       [:button {:type     "button"
                 :class    "btn btn-primary btn-bulky"
                 :data-dismiss "modal"}
                ;:on-click #(swap! state assoc-in [:badge-settings :confirm-delete?] false)}
        (t :badge/Cancel)]
       [:button {:type         "button"
                 :class        "btn btn-danger btn-bulky"
                 :data-dismiss "modal"
                 :on-click     #(delete-selfie-badge state)}
        (t :badge/Delete)]]]]))

(defn endorsement-request-block [state]
  (let [selected-users (cursor state [:send_request_to])
        request (cursor state [:request-comment])]
     [:div.panel.panel-default.endorsement-coded-panel
      [:div.panel-heading {:style {:padding "8px"}}
       [:div.panel-title {:style {:margin-bottom "unset"}}
        [:span.label.endorsement-draft (t :badgeIssuer/Endorsementrequestdraft)]
        [:p {:style {:margin "unset"}} (t :badgeIssuer/Requestendorsementinfo)]]]
      [:div;.btn-toolbar.pull-right
       [:div;.btn-group
        [:button.close.panel-edit
         {:role "button"
          :title (t :badgeIssuer/Editrequest)
          :aria-label (t :badgeIssuer/Editrequest)
          :on-click #(do (.preventDefault %)
                         (mo/open-modal [:badge :requestendorsement] {:state state :context "endorsement_selfie"}))}
         [:i.fa.fa-edit.edit-evidence]]
        [:button.close;.close
         {:role "button"
          :aria-label (t :badgeIssuer/Deleterequestdraft)
          :title (t :badgeIssuer/Deleterequestdraft)
          :on-click #(do
                       (.preventDefault %)
                       (swap! state assoc :request-comment "" :send_request_to []))}
         [:i.fa.fa-trash]]]]
      [:div.panel-body {:style {:padding "10px"}}

       (reduce (fn [r u]
                 (let [{:keys [id first_name last_name profile_picture]} u]
                   (conj r [:div.user-item [profile-link-inline-modal id first_name last_name profile_picture]
                            [:a {:href "#" :on-click (fn [] (reset! selected-users (->> @selected-users (remove #(= id (:id %))) vec)))}
                             [:span.close {:aria-hidden "true" :dangerouslySetInnerHTML {:__html "&times;"}}]]])))
               [:div.selected-users-container] @selected-users)
       [:div
        {:dangerouslySetInnerHTML {:__html (md->html @request)}}]]]))

(defn issue-selfie-content [state]
  (let [badge (:badge @state)
        {:keys [id name image]} badge
        selected-users (cursor state [:selected-users])
        its (cursor state [:issue_to_self])
        current-user {:id (session/get-in [:user :id])}
        request-mode (cursor state [:request-mode])
        request (cursor state [:request-comment])
        visibility (cursor state [:visibility])]
   [:div {:id "badge-info" :class "row flip" :style {:margin "10px 0"}}
    [badge-image badge]
    [:div {:class "col-md-9 badge-info view-tab" :style {:display "block"}}
     (when @(cursor state [:success-alert])
       [:div.alert.alert-success
        (t :badgeIssuer/Badgesuccessfullyissued)])
     [badge-content badge]
     [:hr.border]
     [:div {:style {:margin "15px 0"}}
      [:p [:b (t :badgeIssuer/Setbadgedetails)]]
      [:div.form-horizontal
       [:div.form-group {:style {:margin "15px 0"}}
        [:label {:for "date"} (t :badge/Expireson)]
        [:input.form-control
         {:type "date"
          :id "date"
          :on-change #(do
                        (reset! (cursor state [:badge :expires_on]) (.-target.value %)))}]]]

      [:div.its_block
       [:div.form-group
        [:fieldset {:class "checkbox"}
         [:legend.sr-only ""]
         [:div
          [:label {:for "its"}
           [:input {:name "its_checkbox"
                    :type      "checkbox"
                    :id        "its"
                    :on-change #(do
                                  (toggle-setting its))
                    :checked   @its}]
           (str (t :badgeIssuer/Issuetoself))]]]]

       [:div#badge-settings

        ;;visibility
        (when (pos? @its)
          [:div
           (into [:div]
             (for [f (plugin-fun (session/get :plugins) "block" "badge_visibility_form")]
               [f visibility]))
           [:div
            [:hr.border.dotted-border]]]


          #_[:div.row
             [:div.col-md-12
              [:div.panel.panel-default
               [:div.panel-heading {:style {:padding "8px"}}
                [:div.panel-title {:style {:margin-bottom "unset" :font-size "16px"}}
                 (t :badge/Setbadgevisibility) [info {:style {:position "absolute" :right "0" :top "0"} :content (t :badge/Visibilityinfo) :placement "left"}]]]
               [:div.panel-body {:style {:padding "15px"}}

                [:div.visibility-opts-group
                 [:div.visibility-opt
                   [:input.radio-btn {:id "private"
                                      :type "radio"
                                      :name "private"
                                      :on-click #(do
                                                   (.preventDefault %)
                                                   (reset! visibility "private"))
                                      :checked (= "private" @visibility)}]
                   [:div.radio-tile
                    [:div.icon [:i.fa.fa-lock.fa-4x]]
                    [:label.radio-tile-label {:for "private"} (t :badge/Private)]]]
                 [:div.visibility-opt
                   [:input.radio-btn {:id "internal"
                                      :type "radio"
                                      :name "internal"
                                      :on-click #(do
                                                   (.preventDefault %)
                                                   (reset! visibility "internal"))
                                      :checked (= "internal" @visibility)}]
                   [:div.radio-tile
                    [:div.icon [:i.fa.fa-group.fa-3x]]
                    [:label.radio-tile-label {:for "internal"} (t :badge/Shared)]]]
                 [:div.visibility-opt
                   [:input.radio-btn {:id "public"
                                      :type "radio"
                                      :name "public"
                                      :on-click #(do
                                                   (.preventDefault %)
                                                   (reset! visibility "public"))
                                      :checked (= "public" @visibility)}]
                   [:div.radio-tile
                    [:div.icon [:i.fa.fa-globe.fa-3x]]
                    [:label.radio-tile-label {:for "public"} (t :badge/Public)]]]]]]]
             [:div.col-md-12
              [:hr.border.dotted-border]]])


        ;;evidences
        (when (pos? @its)
          [add-evidence-block state])

        ;;endorsement-requests
        (when (pos? @its)
         (if (and (seq @(cursor state [:send_request_to])) (not (blank? request)))
          [endorsement-request-block state]
          [:div
           [:div.request-link {:id "endorsebadge" :style {:margin "10px 0"}}
            [:a {:href "#"
                 :on-click #(mo/open-modal [:badge :requestendorsement] {:state state :context "endorsement_selfie"})
                 :id "#request_endorsement"}
             [:span [:i.fa.fa-fw.fa-hand-o-right] (t :badge/Requestendorsement)]]
            [:span.text-muted  [:em (str " - " (t :badgeIssuer/Optional))]]]]))]

       [:div.btn-toolbar {:style {:margin-top "20px"}}
        [:div.btn-group
         (when (pos? @its)
           [:button.btn.btn-primary.btn-bulky
            {:data-dismiss "modal"
             :on-click #(do
                          (.preventDefault %)
                          (issue-selfie-badge state (fn [] (js-navigate-to "/badge"))))}

            [:span [:i.fa.fa-paper-plane.fa-lg] (t :badgeIssuer/Issuenow)]])

         (when-not (pos? @its)
          [:button.btn.btn-primary.btn-bulky
           {:on-click #(mo/open-modal [:gallery :profiles] {:type "pickable"
                                                            :context "selfie_issue"
                                                            :selected-users-atom selected-users
                                                            :id id
                                                            :selfie badge
                                                            :func (fn [] (issue-selfie-badge state (fn []
                                                                                                     (if (some (fn [u] (= u (session/get-in [:user :id]))) (map :id @selected-users))
                                                                                                       (js-navigate-to "/badge")
                                                                                                       (mo/previous-view)))))})}

           [:span [:i.fa.fa-users.fa-lg] (t :badgeIssuer/Selectrecipients)]])]]]]]]))

(defn issued-badge-element [element-data state]
  (let [{:keys [expires_on revoked issued_on status id last_name first_name profile_picture user_id]} element-data
        revocation (cursor state [:revocation-request])]
    [:div.row;.col-md-12
     [:div.flip-table.col-md-12.issued-badge-row
      [:div.col-md-4.image-container [profile-link-inline user_id first_name last_name profile_picture]]
      [:div.col-md-2 [:span.hidden-label  (t :badge/Issuedon)] (date-from-unix-time (* 1000 issued_on))]
      [:div.col-md-2 [:span.hidden-label (t :badge/Expireson)] (if expires_on (date-from-unix-time (* 1000 expires_on)) "-")]
      [:div.col-md-2 [:span.hidden-label  (t :user/Status)]   (if (pos? revoked)
                                                                [:span.label.label-danger (t :badgeIssuer/revoked)]
                                                                (case status
                                                                  "accepted" [:span.label-success.label status]
                                                                  "pending"  [:span.label.label-info status]
                                                                  [:span.label.label-danger status]))]
      [:div.col-md-2 #_{:style {:padding "unset"}} (when-not (pos? revoked)
                                                     [:a.revoke-link
                                                      {:on-click #(do
                                                                    (.preventDefault %)
                                                                    (reset! (cursor state [:revoke-id]) id)
                                                                    (if @revocation
                                                                      (reset! revocation false)
                                                                      (reset! revocation true))
                                                                    ;(m/modal! (revoke-selfie-badge-modal id state) {})
                                                                    #_(revoke-selfie-badge id state))}
                                                      [:span [:i.fa.fa-close.fa-remove {:style {:vertical-align "text-bottom"}}] (t :badgeIssuer/Revoke)]])]]
     (revoke-badge-content id state)]))

(defn selfie-issueing-history [state]
  (create-class
   {:reagent-render
    (fn []
      [:div.col-md-9;.badge-info
       (if @(cursor state [:history :Initializing])
         [:span [:i.fa.fa-cog.fa-2x.fa-spin]]

         (if (seq @(cursor state [:history :data]))
           [:div#badge-stats.issuing-history
            #_[:h2.uppercase-header (t :badgeIssuer/Issuinghistory)]
            [:div.panel.panel-default;.issuing-history-panel
             [:div.panel-heading
              [:div.row
               [:div.col-md-12
                [:div.col-md-4]
                [:div.col-md-2.hidden-header (t :badge/Issuedon)]
                [:div.col-md-2.hidden-header (t :badge/Expireson)]
                [:div.col-md-2.hidden-header (t :user/Status)]
                [:div.col-md-2 ""]]]]
             [:div.issuing-history-panel
              [:div.panel-body
               (reduce
                (fn [r e]
                  (conj r [issued-badge-element e state]))
                [:div.row.body {:style {:margin "15px 0"}}]
                @(cursor state [:history :data]))]]]]
           [:div.alert.alert-info (t :badgeIssuer/Yettoissue)]))])
    :component-will-mount
    (fn []
      (issuing-history state))}))

(defn manage-selfie-content [state]
  [:div#badge-info.row.flip {:style {:margin "10px 0"}}
   [badge-image (:badge @state)]
   [selfie-issueing-history state]])

(defn edit-selfie-content [state]
  (let [badge (:badge @state)]
    [:div#badge-info.row.flip
     [:div.col-md-12.view-tab
      [creator/modal-content state]]]))

(defn selfie-navi [state]
  (let [badge (:badge @state)]
    [:div.row.flip-table
     [:div.col-md-3]
     [:div.col-md-9
      [:ul {:class "nav nav-tabs wrap-grid"}
       [:li.nav-item {:class  (if (or (nil? (:tab-no @state)) (= 1 (:tab-no @state))) "active")}
        [:a.nav-link {:href "#" :on-click #(swap! state assoc :tab [preview-selfie badge] :tab-no 1)}
         [:div  [:i.nav-icon.fa.fa-info-circle.fa-lg] (t :metabadge/Info)]]]
       [:li.nav-item {:class  (if (or (nil? (:tab-no @state)) (= 2 (:tab-no @state))) "active")}
        [:a.nav-link {:href "#" :on-click #(swap! state assoc :tab [issue-selfie-content state]  :tab-no 2)}
         [:div  [:i.nav-icon.fa.fa-paper-plane.fa-lg] (t :badgeIssuer/Issue)]]]
       [:li.nav-item {:class  (if (or (nil? (:tab-no @state)) (= 3 (:tab-no @state))) "active")}
        [:a.nav-link {:href "#" :on-click #(swap! state assoc :tab [edit-selfie-content state]  :tab-no 3)}
         [:div  [:i.nav-icon.fa.fa-edit.fa-lg] (t :badgeIssuer/Edit)]]]
       [:li.nav-item {:class  (if (or (nil? (:tab-no @state)) (= 4 (:tab-no @state))) "active")}
        [:a.nav-link {:href "#" :on-click #(swap! state assoc :tab [manage-selfie-content state]  :tab-no 4)}
         [:div  [:i.nav-icon.fa.fa-tasks.fa-lg] (t :badgeIssuer/History)]]]
       [:li.nav-item {:class  (if (or (nil? (:tab-no @state)) (= 5 (:tab-no @state))) "active")}
        [:a.nav-link {:href "#" :on-click #(swap! state assoc :tab [delete-selfie-content state] :tab-no 5)}
         [:div  [:i.nav-icon {:class "fa fa-trash fa-lg"}] (t :core/Delete)]]]]]]))

(defn selfie-content [state]
  [:div
   ;[m/modal-window]
   [selfie-navi state]
   (if (:tab @state)
     (:tab @state)
     (if (:tab-no @state)
       (case (:tab-no @state)
         2 [issue-selfie-content state]
         3 [edit-selfie-content state]
         4 [manage-selfie-content state]
         5 [delete-selfie-content state]
         [preview-selfie (:badge @state)])))])

(defn preview-badge-handler [params]
  (let [badge (:badge params)]
    (fn []
      (preview-selfie badge))))

(defn view-handler [params]
  (let [badge (:badge params)
        tab-no (:tab params)
        state (atom {:badge badge
                     :tab-no (or tab-no 1)
                     :in-modal true
                     :issue_to_self 0
                     :success-alert false
                     :send_request_to []
                     :request-comment ""
                     :evidence {}
                     :all_evidence []
                     :visibility "private"})]

    (fn []
      (selfie-content state))))

#_(defn issue-handler [params]
    (let [;selected-users-atom (:container params)
          badge (:badge params)
          state (atom {;:selected selected-users-atom
                       :badge badge
                       :generating-image false
                       :id (:id badge)
                       :error-message nil
                       :step 0
                       :in-modal true
                       :selected-users []
                       :request-comment ""})]

      (fn []
        (issue-selfie-content state))))

(def ^:export modalroutes
  {:selfie {:preview preview-badge-handler
            :view view-handler
            :issue block/issue-badge-content}})
