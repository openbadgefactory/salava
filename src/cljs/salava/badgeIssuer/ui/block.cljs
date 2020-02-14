(ns salava.badgeIssuer.ui.block
  (:require
   [reagent.core :refer [atom cursor create-class]]
   [reagent.session :as session]
   [salava.badgeIssuer.ui.helper :refer [badge-image badge-content]]
   [salava.badgeIssuer.ui.util :refer [issue-selfie-badge]]
   [salava.core.i18n :refer [t]]
   [salava.core.ui.ajax-utils :as ajax]
   [salava.core.ui.helper :refer [path-for js-navigate-to]]
   [salava.core.ui.modal :as mo]))

(defn get-selfie-badge [id state]
  (ajax/GET
    (path-for (str "/obpv1/selfie/" id))
    {:handler (fn [data]
               (reset! (cursor state [:badge]) data))}))

(defn issue-badge-content [params]
  (let [id (:id params)
        state (atom {:initializing true :badge {}})]
    (create-class
     {:reagent-render
      (fn []
        (if (:initializing @state)
          [:span [:i.fa.fa-cog.fa-lg.fa-spin] " " (t :core/Loading) "..."]
          [:div#badge-info.row.flip
           [badge-image (:badge @state)]
           [:div.col-md-9.badge-info
            [badge-content (:badge @state)]
            [:hr.border]
            [:div {:style {:margin "15px 0"}}
             [:p [:b (t :badgeIssuer/Setbadgedetails)]]
             [:div.form-horizontal
              [:div.form-group {:style {:margin "15px 0"}}
               [:label {:for "date"} (t :badge/Expireson)]
               [:input.form-control
                {:type "date"
                 :name "input-date"
                 :id "date"
                 :on-change #(do
                               (reset! (cursor state [:badge :expires_on]) (.-target.value %)))}]]]]
            [:div.its_block.text-center
             [:button.btn.btn-bulky.btn-primary
              {:role "button"
               :on-click #(do
                            (.preventDefault %)
                            (swap! state assoc :issued_from_gallery true
                                               :selected-users [{:id (session/get-in [:user :id])}])
                            (issue-selfie-badge state (fn [] (js-navigate-to "/badge"))))}
              [:span [:i.fa.fa-paper-plane.fa-lg] ](t :badgeIssuer/Issuenow)]
             [:button.btn.btn-bulky.btn-warning
              {:role "button"
               :on-click #(do
                            (.preventDefault %)
                            (mo/previous-view))}
              (t :core/Cancel)]]]]))

      :component-did-mount
      (fn []
        (ajax/GET
          (path-for (str "/obpv1/selfie/" id))
          {:handler (fn [data]
                     (swap! state assoc :badge data
                                        :initializing false))}))})))


(defn issue-badge [gallery_id]
  (let [state (atom {:visible false})
        visible (cursor state [:visible])]
    (create-class
     {:reagent-render
      (fn []
        (when @visible
         [:div {:style {:padding "5px 0"}}
          [:a {:href "#"
               :on-click #(do
                            (.preventDefault %)
                            (mo/open-modal [:selfie :issue] {:id (:selfie_id @state)}))}
           [:span [:i.fa.fa-paper-plane.fa-lg] (t :badgeIssuer/Issuetoself)]]]))
      :component-did-mount
      (fn []
        (ajax/POST
          (path-for (str "/obpv1/selfie/is_issuable/" gallery_id))
          {:handler (fn [{:keys [issuable_from_gallery selfie_id]}]
                      (swap! state assoc :visible issuable_from_gallery
                                         :selfie_id selfie_id))}))})))

(defn ^:export issue_badge_link [gallery_id]
  (issue-badge gallery_id))
