(ns salava.badge.ui.settings
  (:require [reagent.core :refer [cursor]]
            [salava.core.ui.ajax-utils :as ajax]
            [salava.core.i18n :refer [t]]
            [salava.core.time :refer [date-from-unix-time]]
            [salava.core.ui.tag :as tag]
            [salava.core.ui.rate-it :as r]
            [salava.core.ui.helper :refer [path-for]]
            [salava.badge.ui.helper :as bh]))

(defn set-visibility [visibility state]
  (swap! state assoc-in [:badge-settings :visibility] visibility))

(defn delete-badge [id state init-data]
  (ajax/DELETE
    (path-for (str "/obpv1/badge/" id))
    {:handler (fn []
                (init-data state))}))

(defn save-settings [state init-data]
  (let [{:keys [id visibility tags rating evidence-url]} (:badge-settings @state)]
    (ajax/POST
      (path-for (str "/obpv1/badge/save_settings/" id))
      {:params  {:visibility   visibility
                 :tags         tags
                 :rating       (if (pos? rating) rating nil)
                 :evidence-url evidence-url}
       :handler (fn []
                  (init-data state))})))

(defn settings-modal [{:keys [id name description image_file issued_on expires_on revoked issuer_content_url issuer_content_name issuer_contact issuer_image issuer_description]} state init-data]
  (let [expired? (bh/badge-expired? expires_on)]
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
        [:img {:src (str "/" image_file)}]]
       [:div {:class "col-md-9 badge-content"}
        [:div {:class "row" :id "badge-info"}
         [:div {:class "col-md-12 badge-info"}
          (if revoked
            [:div.revoked (t :badge/Revoked)])
          (if expired?
            [:div.expired (t :badge/Expiredon) ": " (date-from-unix-time (* 1000 expires_on))])
          [:h1.uppercase-header name]
          (if (> issued_on 0)
            [:div.issued_on
             [:label (t :badge/Issuedon ":")]
             [:span (date-from-unix-time (* 1000 issued_on))]])
          (bh/issuer-label-and-link issuer_content_name issuer_content_url issuer_contact)
          (bh/issuer-description  issuer_description)
          [:div.row
           [:div.col-md-12
            description]]]]]]
      [:div {:class "row modal-form"}
       [:div {:class "col-md-3 modal-left"}
        [:div.delete-buttons
         (if (get-in @state [:badge-settings :confirm-delete?])
           [:div
            [:div {:class "alert alert-warning"}
             (t :badge/Confirmdelete)]
            [:button {:type     "button"
                      :class    "btn btn-primary"
                      :on-click #(swap! state assoc-in [:badge-settings :confirm-delete?] false)}
             (t :badge/Cancel)]
            [:button {:type         "button"
                      :class        "btn btn-warning"
                      :data-dismiss "modal"
                      :on-click     #(delete-badge id state init-data)}
             (t :badge/Delete)]]
           [:button {:type     "button"
                     :class    "btn btn-warning delete-button"
                     :on-click #(swap! state assoc-in [:badge-settings :confirm-delete?] true)}
            (t :badge/Deletebadge)])]]
       [:div {:class "col-md-9 settings-content"}
        [:form {:class "form-horizontal"}
         (if (and (not expired?) (not revoked))
           [:div
            [:div {:class "row"}
             [:div {:class "col-md-12 sub-heading"}
              (t :badge/Badgevisibility)]]
            [:div {:class "form-group"}
             [:div {:class "col-md-12"}
              [:input {:id              "visibility-public"
                       :name            "visibility"
                       :value           "public"
                       :type            "radio"
                       :on-change       #(set-visibility "public" state)
                       :default-checked (= "public" (get-in @state [:badge-settings :visibility]))}]
              [:label {:for "visibility-public"}
               (t :badge/Public)]
              [:input {:id              "visibility-internal"
                       :name            "visibility"
                       :value           "internal"
                       :type            "radio"
                       :on-change       #(set-visibility "internal" state)
                       :default-checked (= "internal" (get-in @state [:badge-settings :visibility]))}]
              [:label {:for "visibility-internal"}
               (t :badge/Shared)]
              [:input {:id              "visibility-private"
                       :name            "visibility"
                       :value           "private"
                       :type            "radio"
                       :on-change       #(set-visibility "private" state)
                       :default-checked (= "private" (get-in @state [:badge-settings :visibility]))}]
              [:label {:for "visibility-private"}
               (t :badge/Private)]]]])
         (if (and (not revoked) (not expired?))
           [:div
            [:div {:class "row"}
             [:div {:class "col-md-12 sub-heading"}
              (t :badge/Tags)]]
            [:div {:class "row"}
             [:div {:class "col-md-12"}
              [tag/tags (cursor state [:badge-settings :tags])]]]
            [:div {:class "form-group"}
             [:div {:class "col-md-12"}
              [tag/new-tag-input (cursor state [:badge-settings :tags]) (cursor state [:badge-settings :new-tag])]]]
            [:div {:class "row"}
             [:div {:class "col-md-12 sub-heading"}
              (t :badge/Evidenceurl)]]
            [:div {:class "form-group"}
             [:div {:class "col-md-12"}
              [:input {:class       "form-control"
                       :type        "text"
                       :placeholder (t :badge/EnterevidenceURLstartingwith)
                       :value       (get-in @state [:badge-settings :evidence-url])
                       :on-change   #(swap! state assoc-in [:badge-settings :evidence-url] (-> % .-target .-value))}]]]
            [:div {:class "row"}
             [:div {:class "col-md-12 sub-heading"}
              (t :badge/Rating)]]
            [:div.row
             [:div.col-md-12
              [r/rate-it (get-in @state [:badge-settings :rating]) (cursor state [:badge-settings :rating])]]
             [:div {:class "col-md-12 rating-help"}
              (t :badge/Tellushowvaluableorusefullyouthinkthisbadgeis)]]])
         ]]]]
     [:div.modal-footer
      (if (and (not expired?) (not revoked ))
        [:button {:type         "button"
                  :class        "btn btn-primary"
                  :data-dismiss "modal"
                  :on-click     #(save-settings state init-data)}
         (t :badge/Save)])]]))
