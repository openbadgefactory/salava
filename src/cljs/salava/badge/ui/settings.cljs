(ns salava.badge.ui.settings
  (:require [reagent.core :refer [cursor]]
            [salava.core.ui.ajax-utils :as ajax]
            [salava.core.i18n :refer [t]]
            [salava.core.time :refer [date-from-unix-time]]
            [salava.core.ui.tag :as tag]
            [salava.core.ui.rate-it :as r]
            [salava.core.ui.helper :refer [navigate-to path-for]]
            [salava.core.ui.helper :refer [path-for]]
            [salava.badge.ui.helper :as bh]))

(defn set-visibility [visibility state]
  (swap! state assoc-in [:badge-settings :visibility] visibility))

(defn delete-badge [id state init-data badgeinfo?]
  (ajax/DELETE
    (path-for (str "/obpv1/badge/" id))
    {:handler
    (if badgeinfo?
        (navigate-to "/badge")
      (fn []
        (init-data state)))}))

(defn save-settings [state init-data]
  (let [{:keys [id visibility tags rating evidence-url]} (:badge-settings @state)]
    (ajax/POST
      (path-for (str "/obpv1/badge/save_settings/" id))
      {:params  {:visibility   visibility
                 :tags         tags
                 :rating       (if (pos? rating) rating nil)
                 :evidence-url evidence-url}
       :handler (fn []
                  (init-data state id))})))

(defn toggle-recipient-name [id show-recipient-name-atom]
  (let [new-value (not @show-recipient-name-atom)]
    (ajax/POST
      (path-for (str "/obpv1/badge/toggle_recipient_name/" id))
      {:params {:show_recipient_name new-value}
       :handler (fn [] (reset! show-recipient-name-atom new-value))})))

(defn toggle-evidence [state]
  (let [id (:id @state)
        new-value (not (:show_evidence @state))]
    (ajax/POST
      (path-for (str "/obpv1/badge/toggle_evidence/" id))
      {:params {:show_evidence new-value}
       :handler (fn [] (swap! state assoc :show_evidence new-value))})))

(defn settings-modal [{:keys [id name description image_file issued_on expires_on revoked issuer_content_url issuer_content_name issuer_contact issuer_image issuer_description evidence_url show_evidence]} state init-data badgeinfo?]
  (let [expired? (bh/badge-expired? expires_on)
        show-recipient-name-atom (cursor state [:show_recipient_name])]
    [:div {:id "badge-settings"}
     [:div.modal-body
      [:div.row
       [:div.col-md-12
        [:button {:type         "button"
                  :class        "close"
                  :data-dismiss "modal"
                  :aria-label   "OK"
                  }
         [:span {:aria-hidden             "true"
                 :dangerouslySetInnerHTML {:__html "&times;"}}]]]]
      [:div.row
       [:div {:class "col-md-3 badge-image modal-left"}

        [:img {:src (str "/" image_file) :alt name}]
        [:h1.uppercase-header name]]

       [:div {:class "col-md-9 settings-content"}
       (if revoked
            [:div.revoked (t :badge/Revoked)])
          (if expired?
            [:div.expired (t :badge/Expiredon) ": " (date-from-unix-time (* 1000 expires_on))])

        [:form {:class "form-horizontal"}
         (if (and (not expired?) (not revoked))
           [:div
            [:fieldset {:class "form-group visibility"}
            [:legend {:class "col-md-12 sub-heading"}
              (t :badge/Badgevisibility)]
             [:div {:class (str "col-md-12 " (get-in @state [:badge-settings :visibility]))}
              [:div [:input {:id              "visibility-public"
                       :name            "visibility"
                       :value           "public"
                       :type            "radio"
                       :on-change       #(set-visibility "public" state)
                       :default-checked (= "public" (get-in @state [:badge-settings :visibility]))}]
              [:i {:class "fa fa-globe" }]
              [:label {:for "visibility-public"}
               (t :badge/Public)]]
              [:div [:input {:id              "visibility-internal"
                       :name            "visibility"
                       :value           "internal"
                       :type            "radio"
                       :on-change       #(set-visibility "internal" state)
                       :default-checked (= "internal" (get-in @state [:badge-settings :visibility]))}]
              [:i {:class "fa fa-group" }]
              [:label {:for "visibility-internal"}
               (t :badge/Shared)]]
              [:div [:input {:id              "visibility-private"
                       :name            "visibility"
                       :value           "private"
                       :type            "radio"
                       :on-change       #(set-visibility "private" state)
                       :default-checked (= "private" (get-in @state [:badge-settings :visibility]))}]
              [:i {:class "fa fa-lock" }]
              [:label {:for "visibility-private"}
               (t :badge/Private)]]]]])
            (if (and (not revoked) (not expired?))
               [:div.form-group
             [:fieldset {:class "col-md-12 checkbox"}
             [:legend {:class "col-md-12 sub-heading"}
              (t :badge/Earnervisibility)]
              [:div.col-md-12 [:label {:for "show-name"}
               [:input {:type      "checkbox"
                        :id        "show-name"
                        :on-change #(toggle-recipient-name id show-recipient-name-atom)
                        :checked   @show-recipient-name-atom}]
               (t :badge/Showyourname)]]]])

         (if (and (not revoked) (not expired?))
           [:div
            [:div {:class "row"}
             [:label {:class "col-md-12 sub-heading" :for "newtags"}
              (t :badge/Tags)]]
            [:div {:class "row"}
             [:div {:class "col-md-12"}
              [tag/tags (cursor state [:badge-settings :tags])]]]
            [:div {:class "form-group"}
             [:div {:class "col-md-12"}
              [tag/new-tag-input (cursor state [:badge-settings :tags]) (cursor state [:badge-settings :new-tag])]]]
            [:div {:class "row"}
             [:label {:class "col-md-12 sub-heading" :for "evidenceurl"}
              (t :badge/Evidenceurl)]]
            [:div {:class "form-group"}
             [:div {:class "col-md-12"}
              [:input {:class       "form-control"
                       :type        "text"
                       :id          "evidenceurl"
                       :placeholder (t :badge/EnterevidenceURLstartingwith)
                       :value       (get-in @state [:badge-settings :evidence-url])
                       :on-change   #(swap! state assoc-in [:badge-settings :evidence-url] (-> % .-target .-value))}]]]
            (if (not-empty (get-in @state [:badge-settings :evidence-url]))
               [:fieldset {:class "checkbox"}
               [:legend {:class "sub-heading"}
              (t :badge/Evidencevisibility)]
                [:div [:label {:class (str show_evidence)}
                 [:input {:type      "checkbox"
                          :on-change #(toggle-evidence state)
                          :checked   (:show_evidence @state)}]
                 (t :badge/Showevidence)]]])

              ])]
     [:div.modal-footer
      (if (and (not expired?) (not revoked ))
        [:button {:type         "button"
                  :class        "btn btn-primary"
                  :data-dismiss "modal"
                  :on-click     #(save-settings state init-data)}
         (t :badge/Save)])
         (if (get-in @state [:badge-settings :confirm-delete?])
           [:div {:class "delete-confirm"}
            [:div {:class "alert alert-warning"}
             (t :badge/Confirmdelete)]
            [:button {:type     "button"
                      :class    "btn btn-primary"
                      :on-click #(swap! state assoc-in [:badge-settings :confirm-delete?] false)}
             (t :badge/Cancel)]
            [:button {:type         "button"
                      :class        "btn btn-warning"
                      :data-dismiss "modal"
                      :on-click     #(delete-badge id state init-data badgeinfo?)}
             (t :badge/Delete)]]
           [:a {:class    "delete-button"
                :on-click #(swap! state assoc-in [:badge-settings :confirm-delete?] true)}
                [:i {:class "fa fa-trash"}]
            (t :badge/Deletebadge)])
         ]]]]
         ]))
