(ns salava.extra.spaces.ui.admin
 (:require
  [cljs-uuid-utils.core :refer [make-random-uuid uuid-string]]
  [reagent.core :refer [cursor atom create-class]]
  [reagent-modals.modals :as m]
  [reagent.session :as session]
  [salava.core.ui.helper :refer [path-for js-navigate-to navigate-to]]
  [salava.core.ui.ajax-utils :as ajax]
  [salava.core.ui.layout :as layout]
  [salava.core.i18n :refer [t]]
  [salava.core.time :as time :refer [no-of-days-passed]]
  [salava.extra.spaces.ui.modal :as sm]
  [salava.extra.spaces.ui.creator :as sc]
  [salava.extra.spaces.ui.helper :as sh]
  [salava.extra.spaces.ui.invitelink :refer [invite-link]]
  [salava.extra.spaces.schemas :as schemas]))

#_(defn generate-invite-link [token state]
    (let [name @(cursor state [:space :name])
          alias @(cursor state [:space :alias])]
     (str (session/get :site-url) (path-for (str "space/member_invite/" alias "/" token)))))

#_(defn init-invite-link [state]
    (ajax/POST
     (path-for (str "/obpv1/spaces/invitelink/" (:id @state)))
     {:handler (fn [{:keys [status token]}]
                 (swap! state assoc :link_status status :token token :url (generate-invite-link token state)))}))

(defn init-data [state]
 (let [id (:id @state)]
   (ajax/GET
    (path-for (str "/obpv1/spaces/" id) true)
    {:handler (fn [data]
                (reset! (cursor state [:space]) data)
                #_(when (= @(cursor state [:space :visibility] ) "private")
                    (init-invite-link state)))})))

#_(defn update-link-status [status state]
    (ajax/POST
     (path-for (str "/obpv1/spaces/invitelink/update_status/" (:id @state)) true)
     {:params {:status status}
      :handler (fn [data]
                 (when (= (:status data) "success")
                   (init-invite-link state)))}))

#_(defn refresh-token [state]
    (ajax/POST
     (path-for (str "/obpv1/spaces/invitelink/refresh_token/" (:id @state)) true)
     {:handler (fn [data]
                 (when (= (:status data) "success")
                   (init-invite-link state)))}))

(defn edit-space [state]
  (reset! (cursor state [:error-message]) nil)
  (let [data (select-keys @(cursor state [:space])[:id :name :description :alias :url :logo :css :banner])
        validate-info (sh/validate-inputs schemas/edit-space data)]
    (if (some false? validate-info)
      (do
        (reset! (cursor state [:error-message])
          (case (.indexOf validate-info false)
            0 (t :extra-spaces/Namefieldempty)
            1 (t :extra-spaces/Descriptionfieldempty)
            2 (t :extra-spaces/Aliasfieldempty)
            3 (t :extra-spaces/Missinglogo)
            4 (t :extra-spaces/Urlfieldempty)
            5 (t :extra-spaces/Invalidurlerror)
            (t :extra-spaces/Errormsg)))
        (m/modal! (sh/error-msg state) {}))

      (ajax/POST
       (path-for (str "/obpv1/spaces/edit/" (:id data)))
       {:params data
        :handler (fn [data]
                  (when (= (:status data) "error")
                    (m/modal! (sh/upload-modal data) {}))
                  (when (= (:status data) "success")
                    (m/modal! (sh/upload-modal (assoc data :message "extra-spaces/Spaceinfosaved"))  {:hidden (fn []  (js-navigate-to "/space/edit"))})))}))))


#_(defn invite-link [state]
   (let [status (cursor state [:link_status])
         url (cursor state [:url])
         space (cursor state [:space])]
     (when (= "private" @(cursor state [:space :visibility]))
      [:div.panel.panel-default
       [:div.panel-heading.weighted
        (t :extra-spaces/Invitelink)]
       [:div.panel-body
        [:div.checkbox
         [:label
          [:input {:name      "visibility"
                   :type      "checkbox"
                   :on-change #(do
                                 (update-link-status (if @status false true) state)
                                 (.preventDefault %))
                   :checked   @status}]
          (t :extra-spaces/Activatelink)]]
        [:div#share
         [:p (t :extra-spaces/Aboutinvitelink)]
         [:div.form-group {:id "share-buttons" :class (when-not @status "share-disabled")}
          [:label {:for "invitelink"} (str (t :admin/Url) ": ")]
          [:input.form-control
           {:value @url
            :onChange #(reset! url (.-target.value %))
            :read-only true}]]
         [:p (t :extra-spaces/Aboutresetlink)]
         (if (:confirm-delete? @state)
           [:div
            [:div.alert.alert-warning
             (t :extra-spaces/Confirmlinkreset)]
            [:div.btn-toolbar
             [:div.btn-group
              [:button.btn.btn-primary.btn-bulky
               {:type "button"
                :on-click #(swap! state assoc :confirm-delete? false)}
               (t :badge/Cancel)]
              [:button.btn.btn-warning.btn-bulky
               {:type "button"
                :on-click #(do
                             (refresh-token state)
                             (swap! state assoc :confirm-delete? false))}
               (t :admin/Reset)]]]]
           [:button.btn.btn-warning.btn-bulky
            {:type "button"
             :on-click #(do
                          (.preventDefault %)
                          (swap! state assoc :confirm-delete? true))}
            (t :extra-spaces/Resetinvitelink)])]]])))

(defn manage-status [state]
  [:div#space-gallery.form-group
   [:div.panel.panel-default
    [:div.panel-heading.weighted
     (t :extra-spaces/Status)]
    [:div.panel-body
     [:div.row
      [:div.col-md-12 {:style {:line-height "3"}}
       [:div.blob {:class @(cursor state [:space :status])}] [:span.weighted @(cursor state [:space :status])]
       [:div.pull-right.weighted
        (when (and @(cursor state [:space :valid_until]) (= "active" @(cursor state [:space :status])))
          (str (t :extra-spaces/Subcriptionexpires) " " (sh/num-days-left @(cursor state [:space :valid_until])) " " (t :badge/days)))]]]]]])


(defn edit-space-content [state]
 (let [{:keys [logo banner name]} @(cursor state [:space])]
  [:div#space-creator
   [:div.row
    [:div.col-md-12
     [:div.panel.panel-default
      [:div.panel-heading
        [:div.panel-title
          [:div (if logo [:img {:src (if (re-find #"^data:image" logo) logo (str "/" logo))
                                :style {:width "40px" :height "40px"}}]
                         [:i.fa.fa-building.fa-fw.fa-2x])
              [:h4.inline " " (t :extra-spaces/Edit) "/" name]]]]
      [sc/create-form state false]
      [:hr.border]
      [:div.panel-footer.text-center
       [:div.btn-toolbar
        [:div.btn-group {:style {:float "unset"}}
         [:button.btn.btn-primary.btn-bulky
          {:type "button"
           :on-click #(do
                        (.preventDefault %)
                        (edit-space state))}
                        ;(schemas/edit-spacestate))}
          (t :extra-spaces/Edit)]
         [:button.btn.btn-warning.btn-bulky
          {:type "button"
           :on-click #(do
                        (.preventDefault %)
                       ; (reset! (cursor state [:space]) nil)
                        (navigate-to "space/admin"))}
          (t :core/Cancel)]]]]]]]]))


(defn edit-content [state]
  (create-class
   {:reagent-render
    (fn []
      [:div
       [m/modal-window]
       [edit-space-content state]])
    :component-did-mount
    (fn [] (init-data state))}))

(defn content [state]
 (create-class
  {:reagent-render
   (fn []
     [:div#space
      [m/modal-window]
      [manage-status state]
      [sm/manage-visibility state (fn [] (init-data state))]
      ;(when (= @(cursor state [:space :visibility] ) "private")) 
      [invite-link (select-keys (:space @state) [:id :name :alias])]])
   :component-did-mount
   (fn [] (init-data state))}))

(defn edit-handler [site-navi]
  (let [space (session/get-in [:user :current-space])
        state (atom {:space space :id (:id space)})]
    (fn []
      (layout/default site-navi [edit-content state]))))

(defn handler [site-navi]
  (let [space (session/get-in [:user :current-space])
        state (atom {:space space :id (:id space)})]
    (fn []
      (layout/default site-navi [content state]))))
