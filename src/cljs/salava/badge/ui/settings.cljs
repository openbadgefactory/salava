(ns salava.badge.ui.settings
  (:require [reagent.core :refer [cursor]]
            [ajax.core :as ajax]
            [salava.core.i18n :refer [t]]
            [salava.core.time :refer [date-from-unix-time]]
            [salava.core.ui.tag :as tag]
            [salava.badge.ui.helper :as bh]))

(defn set-visibility [visibility state]
  (swap! state assoc-in [:badge-settings :visibility] visibility))

(defn delete-badge [id]
  (ajax/DELETE
    (str "/obpv1/badge/" id)
    {:handler (fn []
                (.reload js/window.location))}))

(defn save-settings [state]
  (let [{:keys [id visibility tags rating evidence-url]} (:badge-settings @state)]
    (ajax/POST
      (str "/obpv1/badge/save_settings/" id)
      {:params  {:visibility   visibility
                 :tags         tags
                 :rating       (or rating 0)
                 :evidence-url evidence-url}
       :handler (fn []
                  (.reload js/window.location))})))

(defn settings-modal [{:keys [id name description image_file issued_on issuer_url issuer_name issuer_contact]} state]
  (fn []
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
        [:div.row
         [:div {:class "col-md-12 badge-info"}
          [:h2.uppercase-header name]
          (if (> issued_on 0)
            [:div.issued_on
             [:label (str (t :badge/Issuedon) ":")]
             [:span (date-from-unix-time (* 1000 issued_on))]])
          (bh/issuer-label-and-link issuer_name issuer_url issuer_contact)
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
            [:button {:type "button"
                      :class "btn btn-primary"
                      :on-click #(swap! state assoc-in [:badge-settings :confirm-delete?] false)}
             (t :badge/Cancel)]
            [:button {:type "button"
                      :class "btn btn-warning"
                      :on-click #(delete-badge id)}
             (t :badge/Delete)]]
           [:button {:type "button"
                     :class "btn btn-warning delete-button"
                     :on-click #(swap! state assoc-in [:badge-settings :confirm-delete?] true)}
            (t :badge/Deletebadge)])]]
       [:div {:class "col-md-9 settings-content"}
        [:form {:class "form-horizontal"}
         [:div {:class "row"}
          [:div {:class "col-md-12 sub-heading"}
           (t :badge/Badgevisibility)]]
         [:div {:class "form-group"}
          [:div {:class "col-md-12"}
           [:input {:id             "visibility-public"
                    :name           "visibility"
                    :value          "public"
                    :type           "radio"
                    :on-change       #(set-visibility "public" state)
                    :checked (= "public" (get-in @state [:badge-settings :visibility]))}]
           [:label {:for "visibility-public"}
            (t :badge/Public)]
           [:input {:id             "visibility-internal"
                    :name           "visibility"
                    :value          "internal"
                    :type           "radio"
                    :on-change       #(set-visibility "internal" state)
                    :checked (= "internal" (get-in @state [:badge-settings :visibility]))}]
           [:label {:for "visibility-internal"}
            (t :badge/Shared)]
           [:input {:id             "visibility-private"
                    :name           "visibility"
                    :value          "private"
                    :type           "radio"
                    :on-change       #(set-visibility "private" state)
                    :checked (= "private" (get-in @state [:badge-settings :visibility]))}]
           [:label {:for "visibility-private"}
            (t :badge/Private)]]]
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
           [:input {:class     "form-control"
                    :type      "text"
                    :value     (get-in @state [:badge-settings :evidence-url])
                    :on-change #(swap! state assoc-in [:badge-settings :evidence-url] (-> % .-target .-value))}]]]
         [:div {:class "row"}
          [:div {:class "col-md-12 sub-heading"}
           (t :badge/Rating)]]
         [:div {:class "form-group"}
          [:div {:class "col-md-12"}
           [:select {:class "form-control select-rating"
                     :value (get-in @state [:badge-settings :rating])
                     :on-change #(swap! state assoc-in [:badge-settings :rating] (-> % .-target .-value (js/parseInt)))}
            [:option {:value 0} "-"]
            (for [rating (range 1 6)]
              [:option {:value rating} rating])]]]
         ]]]]
     [:div.modal-footer
      [:button {:type         "button"
                :class        "btn btn-primary"
                :data-dismiss "modal"
                :on-click     #(save-settings state)}
       (t :badge/Save)]]]))