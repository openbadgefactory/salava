(ns salava.badge.ui.settings
  (:require [ajax.core :as ajax]
            [clojure.string :refer [trim lower-case]]
            [salava.core.i18n :refer [t]]
            [salava.core.time :refer [date-from-unix-time]]))

(defn set-visibility [visibility state]
  (swap! state assoc-in [:badge-settings :visibility] visibility))

(defn add-tag [state]
  (let [tags (get-in @state [:badge-settings :tags])
        new-tag (-> (get-in @state [:badge-settings :new-tag])
                    trim)
        tag-exists? (some #(= (lower-case new-tag)
                          (lower-case %)) tags)]
    (when (and (not (empty? new-tag))
               (not tag-exists?))
      (swap! state assoc-in [:badge-settings :tags] (conj (vec tags) new-tag))
      (swap! state assoc-in [:badge-settings :new-tag] ""))))

(defn remove-tag [tag state]
  (let [tags (get-in @state [:badge-settings :tags])]
    (swap! state assoc-in [:badge-settings :tags] (remove #(= % tag) tags))))

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
                 :rating       rating
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
          [:h2 name]
          (if (> issued_on 0)
            [:div.issued_on
             [:label (str (t :badge/Issuedon) ":")]
             [:span (date-from-unix-time (* 1000 issued_on))]])
          [:div.issuer
           [:label (str (t :badge/Issuedby) ":")]
           [:a {:href issuer_url
                :target "_blank"} issuer_name]
           (if-not (empty? issuer_contact)
             [:span
              " / " [:a {:href (str "mailto:" issuer_contact)} issuer_contact]])]
          [:div.row
           [:div.col-md-12
            description]]
          ]]]]
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
          [:div {:class "col-md-12 labels"}
           (for [tag (get-in @state [:badge-settings :tags])]
             [:span {:class "label label-default"}
              tag
              [:a {:class "remove-tag"
                   :dangerouslySetInnerHTML {:__html "&times;"}
                   :on-click #(remove-tag tag state)}]])]]
         [:div {:class "form-group"}
          [:div {:class "col-md-12"}
           [:input {:type        "text"
                    :class       "form-control"
                    :placeholder (t :badge/Typetag)
                    :value       (get-in @state [:badge-settings :new-tag])
                    :on-change   #(swap! state assoc-in [:badge-settings :new-tag] (-> % .-target .-value))
                    :on-key-down #(if (= (.-which %) 13)
                                   (add-tag state))}]]]
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
                :class        "btn btn-default btn-primary"
                :data-dismiss "modal"
                :on-click     #(save-settings state)}
       (t :badge/Save)]]]))