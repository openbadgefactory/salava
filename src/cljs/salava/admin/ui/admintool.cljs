(ns salava.admin.ui.admintool
  (:require [reagent.core :refer [atom cursor]]
            [reagent-modals.modals :as m]
            [clojure.string :refer [trim]]
            [reagent.session :as session]
            [salava.core.ui.ajax-utils :as ajax]
            [salava.core.ui.layout :as layout]
            [salava.core.ui.grid :as g]
            [salava.core.ui.helper :refer [path-for current-path navigate-to input-valid?]]
            [salava.core.i18n :refer [t]]
            [salava.core.helper :refer [dump]]
            [salava.admin.ui.helper :refer [valid-item-type? valid-item-id? checker admin?]]
            [salava.admin.ui.admintool-content :refer [admin-modal]]))


(defn message-form [mail]
  (let [message (cursor mail [:message])
        subject (cursor mail [:subject])]
    [:div
     [:div.form-group
      [:label 
       (str (t :admin/Subjectforitemowner) ":")]
      [:input {:class    "form-control"
               :value    @subject
               :onChange #(reset! subject (.-target.value %)) }]]
     [:div.form-group
      [:label 
       (str (t :admin/Messageforitemowner) ":")]
      [:textarea {:class    "form-control"
                  :rows     "5"
                  :value    @message
                  :onChange #(reset! message (.-target.value %))}]]]))

(defn set-private [item-type item-id state init-data]
  (ajax/POST
   (path-for (str "/obpv1/admin/private_"item-type"/" item-id))
   {:response-format :json
    :keywords?       true
    :params          {:item-type item-type :item-id item-id}
    :handler         (fn [data]
                       (if (not-empty (str init-data))
                         (init-data state nil)
                         (navigate-to "/admin")))
    :error-handler   (fn [{:keys [status status-text]}]
                       (.log js/console (str status " " status-text)))}))

(defn admin-private-modal [item-type item-id state init-data]
  [:div
   [:div.modal-header
    (t :admin/Private)
    [:button {:type "button"
              :class "close"
              :data-dismiss "modal"
              :aria-label "OK"}
     [:span {:aria-hidden "true"
             :dangerouslySetInnerHTML {:__html "&times;"}}]]]
   [:div.modal-body
    (str  (t :admin/Privatethis) "?" (:user-id state))]
   [:div.modal-footer
    [:button {:type "button"
              :class "btn btn-primary"
              :data-dismiss "modal"
              :on-click #(set-private item-type item-id state init-data)}
     (t :admin/Yes)]
    [:button {:type "button"
              :class "btn btn-primary"
              :data-dismiss "modal"}
     (t :admin/No)]]])

(defn delete-item [item-type item-id message user-id state init-data]
  (let [{:keys [subject message]} @message]

    (ajax/POST
     (path-for (str "/obpv1/admin/delete_"item-type"/" item-id))
     {:response-format :json
      :keywords?       true
      :params          {:subject subject
                        :message message
                        :user-id user-id}
      :handler         (fn [data]
                         (if (not-empty (str init-data))
                         (init-data state nil)
                         (navigate-to "/admin")))
      :error-handler   (fn [{:keys [status status-text]}]
                        ; (.log js/console "Lol kek")
                         )}))
  )

(defn send-message [user-id message]
  (let [message @message]
    (ajax/POST
     (path-for (str "/obpv1/admin/send_message/" user-id ))
     {:response-format :json
      :keywords?       true
      :params          message
      :handler         (fn [data]
                         ;(navigate-to "/admin")
                         )
      :error-handler   (fn [{:keys [status status-text]}]
                         ;(.log js/console "Lol kek")
                         )}))
  )


(defn delete-user [user-id message]
  (let [message @message]
    (ajax/POST
     (path-for (str "/obpv1/admin/delete_user/" user-id ))
     {:response-format :json
      :keywords?       true
      :params          message
      :handler         (fn [data]
                         ;(navigate-to "/admin")
                         )
      :error-handler   (fn [{:keys [status status-text]}]
                         ;(.log js/console "Lol kek")
                         )}))
  )
(defn get-user-name-and-email [user-id]
  (ajax/GET
   (path-for (str "/obpv1/admin/user_name_and_email/" user-id))
   {:handler (fn [user]
               user)})
  )

(defn admin-send-message-modal [user-id mail]
  [:div
   [:div.modal-header
    (t :admin/Sendmessage)
      [:button {:type         "button"
                :class        "close"
                :data-dismiss "modal"
                :aria-label   "OK"}
       [:span {:aria-hidden             "true"
               :dangerouslySetInnerHTML {:__html "&times;"}}]]]
     [:div.modal-body
      (str  (t :admin/Deletethisitem) "?")
       (message-form mail)
      ]
     [:div.modal-footer
      [:button {:type         "button"
                :class        "btn btn-primary"
                :data-dismiss "modal"
                :on-click     #(send-message user-id mail)}
       (t :admin/Yes)]
      [:button {:type         "button"
                :class        "btn btn-primary"
                :data-dismiss "modal"}
       (t :admin/No)]]])

(defn admin-delete-modal [item-type item-id mail user-id state init-data]
  [:div
   [:div.modal-header
    (t :admin/Delete)
      [:button {:type         "button"
                :class        "close"
                :data-dismiss "modal"
                :aria-label   "OK"}
       [:span {:aria-hidden             "true"
               :dangerouslySetInnerHTML {:__html "&times;"}}]]]
   [:div.modal-body
      (str  (t :admin/Deletethisitem) "?")
      (message-form mail)
      ]
     [:div.modal-footer
      [:button {:type         "button"
                :class        "btn btn-primary"
                :data-dismiss "modal"
                :on-click     #(delete-item item-type item-id mail user-id state init-data)}
       (t :admin/Yes)]
      [:button {:type         "button"
                :class        "btn btn-primary"
                :data-dismiss "modal"}
       (t :admin/No)]]])


(defn admin-lock-user-modal [user-id mail]
  [:div
   [:div.modal-header
    (t :admin/Lockuser)
    [:button {:type "button"
              :class "close"
              :data-dismiss "modal"
              :aria-label "OK"}
     [:span {:aria-hidden "true"
             :dangerouslySetInnerHTML {:__html "&times;"}}]]]
   [:div.modal-body
    (str  (t :admin/Deletethisuser) "?")
    (message-form mail)
    ]
   [:div.modal-footer
    [:button {:type "button"
              :class "btn btn-primary"
              :data-dismiss "modal"
              :on-click #(delete-user user-id mail)
              }
     (t :admin/Yes)]
    [:button {:type "button"
              :class "btn btn-primary"
              :data-dismiss "modal"}
     (t :admin/No)]]])

(defn admin-gallery-modal [item-type item-id state init-data]
  [:div
   [:div.modal-header
    [:button {:type "button"
              :class "close"
              :data-dismiss "modal"
              :aria-label "OK"}
     [:span {:aria-hidden "true"
             :dangerouslySetInnerHTML {:__html "&times;"}}]]]
   [:div.modal-body
    (str  (t :admin/Privatethis) "?" )]
   [:div.modal-footer
    [:button {:type "button"
              :class "btn btn-primary"
              :data-dismiss "modal"
              :on-click #(set-private item-type item-id state init-data)}
     (t :admin/Yes)]
    [:button {:type "button"
              :class "btn btn-primary"
              :data-dismiss "modal"}
     (t :admin/No)]]])


(defn admintool[user-id]
   (if (admin?)  
     (let [{:keys [item-type item-id]} (checker (current-path))
           mail (atom {:subject ""
                       :message ""})]
       [:div.row
        [m/modal-window]
        [:div {:class "pull-right"}
         [:div {:class "dropdown"}
          [:button {:class "btn btn-default dropdown-toggle" :type "button" :id "admindropdownmenu" :data-toggle "dropdown" :aria-haspopup "true" :aria-expanded "true"} "admin tools"
           [:span.caret]]
          [:ul {:class "dropdown-menu" :aria-labelledby "admindropdownmenu"}
           (if (not (= item-type "badges"))
             [:li [:a {:on-click #(do (.preventDefault %)
                                      (m/modal! [admin-send-message-modal user-id  mail]))}
                   (t :admin/Sendmessage)]])
           (if (not (= item-type "user"))
               [:li [:a {:on-click #(do (.preventDefault %)
                                        (m/modal! (admin-private-modal item-type item-id "" "")))}
                     (t :admin/Private)]])
           (if (not (= item-type "user"))
             [:li [:a {:on-click #(do (.preventDefault %)
                                      (m/modal! [admin-delete-modal item-type item-id mail user-id "" ""]))}
                   (t :admin/Delete)]])
           (if (not (= item-type "badges"))
             [:li [:a {:on-click #(do (.preventDefault %)
                                      (m/modal! [admin-lock-user-modal user-id mail]))}
                   (t :admin/Lockuser)]])]]]])))

(defn admin-gallery-badge1 [item-id item-type state init-data]
  (if (admin?)
    (let [mail (atom {:subject ""
                      :message ""})]
      [:div {:class "dropup"}
       [:a {:class "bottom-link pull-right dropdown-toggle" :type "button" :id "admindropdownmenu" :data-toggle "dropdown" :aria-haspopup "true" :aria-expanded "true"} 
        "Admin tools" [:i {:class "fa fa-caret-up admintool-caret-up" :aria-hidden "true"}]]
       [:ul {:class "dropdown-menu pull-right" :aria-labelledby "admindropdownmenu"}
        
        (if (not (= item-type "user"))
          [:li [:a {:on-click #(do (.preventDefault %)
                                   (m/modal! (admin-private-modal item-type item-id state init-data) ))}
                (t :admin/Private)]])
        (if (not (= item-type "user"))
          [:li [:a {:on-click #(do (.preventDefault %)
                                   (m/modal! [admin-delete-modal item-type item-id mail nil state init-data]))}
                (t :admin/Delete)]])]])))

(defn open-modal [item-id item-type state init-data mail]
  (ajax/GET
    (path-for (str "/obpv1/gallery/public_badge_content/" item-id))
    {:handler (fn [data]
                (m/modal! [admin-modal item-type item-id mail nil state init-data data] {:size :lg}))}))


(defn admin-gallery-badge [item-id item-type state init-data]
  (if (admin?)
    (let [mail (atom {:subject ""
                      :message ""})]
      [:div
       [:a {:class "bottom-link pull-right"
            :on-click #(do (.preventDefault %)
                           (open-modal item-id item-type state init-data mail))}
        [:i {:class "fa fa-lock"}] "admin tools"]])))



(defn admintool-gallery-page [item-id item-type state init-data user-id]
  (if (and (admin?) (not (= user-id (session/get-in [:user :id]))))
    (let [mail (atom {:subject ""
                      :message ""})]
      [:div {:class "dropup media-bottom-admin"}
       [:a {:class "bottom-link pull-right dropdown-toggle" :type "button" :id "admindropdownmenu" :data-toggle "dropdown" :aria-haspopup "true" :aria-expanded "true"} 
        "Admin tools" [:i {:class "fa fa-caret-up admintool-caret-up" :aria-hidden "true"}]]
       [:ul {:class "dropdown-menu pull-right" :aria-labelledby "admindropdownmenu"}
        [:li [:a {:on-click #(do (.preventDefault %)
                                      (m/modal! [admin-send-message-modal user-id  mail]))}
              (t :admin/Sendmessage)]]
        [:li [:a {:on-click #(do (.preventDefault %)
                                   (m/modal! (admin-private-modal item-type item-id state init-data) ))}
                (t :admin/Private)]]
        [:li [:a {:on-click #(do (.preventDefault %)
                                   (m/modal! [admin-delete-modal item-type item-id mail user-id state init-data]))}
                (t :admin/Delete)]]
        
        [:li [:a {:on-click #(do (.preventDefault %)
                                      (m/modal! [admin-lock-user-modal user-id mail]))}
                   (t :admin/Lockuser)]]]])))
