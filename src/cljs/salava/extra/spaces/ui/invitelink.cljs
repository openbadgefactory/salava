(ns salava.extra.spaces.ui.invitelink
  (:require
   [reagent.core :refer [atom cursor]]
   [reagent.session :as session]
   [salava.core.ui.helper :refer [path-for]]
   [salava.core.ui.ajax-utils :as ajax]
   [salava.core.i18n :refer [t]]
   [salava.extra.spaces.ui.helper :refer [input-button]]))

(defn generate-invite-link [token state]
  (let [name @(cursor state [:space :name])
        alias @(cursor state [:space :alias])]
   (str (session/get :site-url) (path-for (str "space/member_invite/" alias "/" token)))))

(defn init-invite-link [state]
  (ajax/POST
   (path-for (str "/obpv1/spaces/invitelink/" (:id @state)))
   {:handler (fn [{:keys [status token]}]
               (swap! state assoc :link_status status :token token :url (generate-invite-link token state)))}))

(defn update-link-status [status state]
  (ajax/POST
   (path-for (str "/obpv1/spaces/invitelink/update_status/" (:id @state)) true)
   {:params {:status status}
    :handler (fn [data]
               (when (= (:status data) "success")
                 (init-invite-link state)))}))

(defn refresh-token [state]
  (ajax/POST
   (path-for (str "/obpv1/spaces/invitelink/refresh_token/" (:id @state)) true)
   {:handler (fn [data]
               (when (= (:status data) "success")
                 (init-invite-link state)))}))

(defn invite-link [params]
 (let [state (atom {:id (:id params)
                    :url ""
                    :link_status false
                    :space {:name (:name params)
                            :alias (:alias params)}})]
   (init-invite-link state)
   (fn []
     (let [status (cursor state [:link_status])
           url (cursor state [:url])
           space (cursor state [:space])]
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
            ;[:label {:for "invitelink"} (str (t :admin/Url) ": ")]
            [input-button (str (t :admin/Url) ": ") "invitelink"  (cursor state [:url])]
            #_[:input.form-control
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
              (t :extra-spaces/Resetinvitelink)])]]]))))
