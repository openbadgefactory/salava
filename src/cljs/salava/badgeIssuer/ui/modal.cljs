(ns salava.badgeIssuer.ui.modal
  (:require
    [clojure.string :refer [blank?]]
    [reagent.core :refer [atom cursor]]
    [salava.badgeIssuer.ui.creator :as creator]
    [salava.badgeIssuer.ui.util :refer [delete-selfie-badge]]
    [salava.core.i18n :refer [t]]
    [salava.core.ui.modal :as mo]))

(defn badge-image [badge]
  (let [{:keys [name image]} badge]
    [:div {:class "col-md-3"}
     [:div.badge-image
      [:img {:src image :alt name}]]]))

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
   [:div {:id "badge-info" :class "row flip"}
    [badge-image badge]
    [:div {:class "col-md-9 badge-info view-tab" :style {:display "block"}}
     [badge-content badge]]]))


(defn delete-selfie [state]
  (let [{:keys [name image id]} (:badge @state)]
   [:div {:id "badge-info" :class "row flip"}
    [badge-image (:badge @state)]
    [:div {:class "col-md-9 badge-info view-tab" :style {:display "block"}}
     [:div {:class "alert alert-warning" :style {:margin "20px 0"}}
      (t :badge/Confirmdelete)]
     [:div
      [:button {:type     "button"
                :class    "btn btn-primary"
                :data-dismiss "modal"}
                ;:on-click #(swap! state assoc-in [:badge-settings :confirm-delete?] false)}
       (t :badge/Cancel)]
      [:button {:type         "button"
                :class        "btn btn-warning"
                :data-dismiss "modal"
                :on-click     #(delete-selfie-badge state)}
       (t :badge/Delete)]]]]))

(defn issue-selfie [state]
  (let [badge (:badge @state)
        {:keys [id name image]} badge
        selected-users (cursor state [:selected-users])]
   [:div {:id "badge-info" :class "row flip"}
    [badge-image badge]
    [:div {:class "col-md-9 badge-info view-tab" :style {:display "block"}}
     [badge-content badge]
     [:hr.border]
     [:div
      [:p [:b (t :badgeIssuer/Setbadgedetails)]]
      [:div.form-group {:style {:margin "15px 0"}}
       [:label {:for "date"} (t :badge/Expireson)]
       [:input.form-control {:type "date"
                             :id "date"
                             :on-change #(do
                                            (reset! (cursor state [:badge :expires_on]) (.-target.value %)))}]]
      [:button.btn.btn-primary
       {:on-click #(mo/open-modal [:gallery :profiles] {:type "pickable" :context "selfie_issue" :selected-users-atom selected-users :id id :selfie badge})}
       (t :badgeIssuer/Selectrecipients)]]]]))

(defn edit-selfie [state]
  (let [badge (:badge @state)]
    [:div#badge-infio.row.flip
     #_[badge-image badge]
     [:div.col-md-12.badge-info.view-tab
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
       [:a.nav-link {:href "#" :on-click #(swap! state assoc :tab [edit-selfie state]  :tab-no 2)}
        [:div  [:i.nav-icon.fa.fa-certificate.fa-lg.fa-fw] (t :badgeIssuer/Edit)]]]
      [:li.nav-item {:class  (if (or (nil? (:tab-no @state)) (= 3 (:tab-no @state))) "active")}
       [:a.nav-link {:href "#" :on-click #(swap! state assoc :tab [delete-selfie state] :tab-no 3)}
        [:div  [:i.nav-icon {:class "fa fa-trash fa-lg"}] (t :core/Delete)]]]]]]))

(defn selfie-content [state]
  [:div
   [selfie-navi state]
   (if (:tab @state)
     (:tab @state)
     (if (:tab-no @state)
       (case (:tab-no @state)
         2 [edit-selfie state]
         3 [delete-selfie state]
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
                     :in-modal true})]


    (fn []
      (issue-selfie state))))

(def ^:export modalroutes
  {:selfie {:preview preview-badge-handler
            :view view-handler
            :issue issue-handler}})
