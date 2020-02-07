(ns salava.badgeIssuer.ui.modal
  (:require
    [clojure.string :refer [blank?]]
    [reagent.core :refer [atom cursor create-class]]
    [reagent.session :as session]
    [salava.badgeIssuer.ui.creator :as creator]
    [salava.badgeIssuer.ui.util :refer [toggle-setting delete-selfie-badge issue-selfie-badge issuing-history]]
    [salava.core.i18n :refer [t]]
    [salava.core.ui.modal :as mo]))

(defn badge-image [badge]
  (let [{:keys [name image]} badge]
    [:div {:class "col-md-3"}
     [:div.badge-image
      [:img {:src (if (re-find #"^data:image" image)
                    image
                    (str "/" image))
             :alt name}]]]))

(defn badge-content [badge]
  (let [{:keys [name description tags criteria image]} badge]
    [:div ;{:class "col-md-9 badge-info view-tab" :style {:display "block"}}
     [:div.row
      [:div {:class "col-md-12"}
       [:h1.uppercase-header name]
       [:div.description description]]]

     (when-not (blank? criteria)
       [:div {:class "row criteria-html"}
        [:div.col-md-12
         [:h2.uppercase-header (t :badge/Criteria)]
         [:div {:dangerouslySetInnerHTML {:__html criteria}}]]])

     [:div.row
      (if (not (empty? tags))
        (into [:div.col-md-12 {:style {:margin "10px 0"}}]
              (for [tag tags]
                [:span {:id "tag"
                        :style {:font-weight "bold" :padding "0 2px"}}
                 (str "#" tag)])))]]))

(defn preview-selfie [badge]
 (let [{:keys [name description tags criteria image]} badge]
   [:div {:id "badge-info" :class "row flip" :style {:margin "10px 0"}}
    [badge-image badge]
    [:div {:class "col-md-9 badge-info view-tab" :style {:display "block"}}
     [badge-content badge]]]))


(defn delete-selfie [state]
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

(defn issue-selfie [state]
  (let [badge (:badge @state)
        {:keys [id name image]} badge
        selected-users (cursor state [:selected-users])
        its (cursor state [:issue_to_self])
        current-user {:id (session/get-in [:user :id])}]
   [:div {:id "badge-info" :class "row flip" :style {:margin "10px 0"}}
    [badge-image badge]
    [:div {:class "col-md-9 badge-info view-tab" :style {:display "block"}}
     [badge-content badge]
     [:hr.border]
     [:div {:style {:margin "15px 0"}}
      [:p [:b (t :badgeIssuer/Setbadgedetails)]]
      [:div.form-horizontal
       [:div.form-group {:style {:margin "15px 0"}}
        [:label {:for "date"} (t :badge/Expireson)]
        [:input.form-control {:type "date"
                              :id "date"
                              :on-change #(do
                                             (reset! (cursor state [:badge :expires_on]) (.-target.value %)))}]]]

      [:div.col-md-12.its_block
       [:div.form-group
        [:fieldset {:class "checkbox"}
         [:legend.col-md-9 ""]
         [:div;.col-md-12.its_block
           [:label {:for "its"}
            [:input {:type      "checkbox"
                     :id        "its"
                     :on-change #(do
                                   (toggle-setting its)
                                   (reset! selected-users (conj @selected-users current-user)))
                     :checked   @its}]
            (str (t :badgeIssuer/Issuetoself))]]]]

       (when (pos? @its)
        [:button.btn.btn-primary.btn-bulky
         {:on-click #(do
                       (.preventDefault %)
                       (issue-selfie-badge state))
          :data-dismiss "modal"}
         [:span [:i.fa.fa-paper-plane.fa-lg](t :badgeIssuer/Issuenow)]])
       [:button.btn.btn-primary.btn-bulky
        {:on-click #(mo/open-modal [:gallery :profiles] {:type "pickable" :context "selfie_issue" :selected-users-atom selected-users :id id :selfie badge :func (fn [] (issue-selfie-badge state))})}
        [:span [:i.fa.fa-users.fa-lg](t :badgeIssuer/Selectrecipients)]]]]]]))

(defn selfie-issueing-history [state]
  (create-class
   {:reagent-render
    (fn []
      [:div.col-md-9.badge-info
       (if @(cursor state [:history :Initializing])
         [:span [:i.fa.fa-cog.fa-2x.fa-spin]]
         (if (seq @(cursor state [:history :data]))
           [:div ""]
           [:div.alert.alert-info "You are yet to issue this badge"]))])
    :component-will-mount
    (fn []
      (issuing-history state))}))

(defn manage-selfie [state]
  [:div#badge-info.row.flip {:style {:margin "10px 0"}}
   [badge-image (:badge @state)]
   [selfie-issueing-history state]])

(defn edit-selfie [state]
  (let [badge (:badge @state)]
    [:div#badge-info.row.flip
     #_[badge-image badge]
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
       [:a.nav-link {:href "#" :on-click #(swap! state assoc :tab [issue-selfie state]  :tab-no 2)}
        [:div  [:i.nav-icon.fa.fa-paper-plane.fa-lg] (t :badgeIssuer/Issue)]]]
      [:li.nav-item {:class  (if (or (nil? (:tab-no @state)) (= 3 (:tab-no @state))) "active")}
       [:a.nav-link {:href "#" :on-click #(swap! state assoc :tab [edit-selfie state]  :tab-no 3)}
        [:div  [:i.nav-icon.fa.fa-edit.fa-lg] (t :badgeIssuer/Edit)]]]
      [:li.nav-item {:class  (if (or (nil? (:tab-no @state)) (= 4 (:tab-no @state))) "active")}
       [:a.nav-link {:href "#" :on-click #(swap! state assoc :tab [manage-selfie state]  :tab-no 4)}
        [:div  [:i.nav-icon.fa.fa-tasks.fa-lg] (t :badgeIssuer/Manage)]]]
      [:li.nav-item {:class  (if (or (nil? (:tab-no @state)) (= 5 (:tab-no @state))) "active")}
       [:a.nav-link {:href "#" :on-click #(swap! state assoc :tab [delete-selfie state] :tab-no 5)}
        [:div  [:i.nav-icon {:class "fa fa-trash fa-lg"}] (t :core/Delete)]]]]]]))

(defn selfie-content [state]
  [:div
   [selfie-navi state]
   (if (:tab @state)
     (:tab @state)
     (if (:tab-no @state)
       (case (:tab-no @state)
         2 [issue-selfie state]
         3 [edit-selfie state]
         4 [delete-selfie state]
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
                     :in-modal true})]
    (fn []
      (selfie-content state))))

(defn issue-handler [params]
  (let [selected-users-atom (:container params)
        badge (:badge params)
        state (atom {:selected selected-users-atom
                     :badge badge
                     :generating-image false
                     :id (:id badge)
                     :error-message nil
                     :step 0
                     :in-modal true
                     :selected-users []})]


    (fn []
      (issue-selfie state))))

(def ^:export modalroutes
  {:selfie {:preview preview-badge-handler
            :view view-handler
            :issue issue-handler}})
