(ns salava.admin.ui.admintool-content
  (:require [reagent.core :refer [atom cursor]]
            [reagent-modals.modals :as m]
            [clojure.string :refer [trim]]
            [salava.badge.ui.helper :as bh]
            [reagent.session :as session]
            [salava.core.ui.ajax-utils :as ajax]
            [salava.core.ui.layout :as layout]
            [salava.core.ui.grid :as g]
            [salava.user.ui.helper :refer [profile-picture profile-link-inline]]
            [salava.core.ui.helper :refer [path-for current-path navigate-to input-valid?]]
            [salava.core.time :refer [date-from-unix-time]]
            [salava.core.i18n :refer [t]]
            [salava.core.helper :refer [dump]]
            [salava.admin.ui.helper :refer [valid-item-type? valid-item-id? checker admin?]]))

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


(defn delete-item [item_type item_id mail item_owner_id visible_area item_owner gallery-state init-data]
  (let [{:keys [subject message]} @mail]
    [:div {:class "row"}
     [:div {:class "col-md-12 sub-heading"}
      [:a {:href "#" :on-click #(do (.preventDefault %) (reset! visible_area (if (= "delete-item" @visible_area) "" "delete-item")))} "Poista" ]]
     (if (= @visible_area "delete-item")
       [:div.col-md-12
        (str "haluatko varmasti poistaa merkin jonka on saanut " item_owner "?")
        [:br]
        "Lähetä sähköpostiviesti merkin omistajille:"
        (message-form mail)
        [:button {:type         "button"
                  :class        "btn btn-primary"
                  :data-dismiss "modal"
                  :on-click     #(ajax/POST
                                  (path-for (str "/obpv1/admin/delete_"item_type"/" item_id))
                                  {:response-format :json
                                   :keywords?       true
                                   :params          {:subject subject
                                                     :message message
                                                     :user-id  item_owner_id}
                                   :handler         (fn [data]
                                                      (if (and (= "success" data) init-data)
                                                        (init-data gallery-state)
                                                        (navigate-to "/admin"))
                                                      
                                                      )
                                   :error-handler   (fn [{:keys [status status-text]}]
                                        ; (.log js/console "Lol kek")
                                                      )})}
         (t :admin/Yes)]
        [:button {:type         "button"
                  :class        "btn btn-primary"
                  :data-dismiss "modal"}
         (t :admin/No)]])])
  )

(defn send-message [mail item_owner_id visible_area gallery-state init-data]
  [:div {:class "row"}
    [:div {:class "col-md-12 sub-heading"}
     [:a {:href "#" :on-click #(do (.preventDefault %) (reset! visible_area (if (= "send-message" @visible_area) "" "send-message")))} "Lähetä viestiä" ]]
   (if (= @visible_area "send-message")
     (let [message @mail]
       [:div.col-md-12
        (message-form mail)
        [:button {:type         "button"
                  :class        "btn btn-primary"
                  :data-dismiss "modal"
                  :on-click     #(ajax/POST
                                   (path-for (str "/obpv1/admin/send_message/" item_owner_id ))
                                   {:response-format :json
                                    :keywords?       true
                                    :params          message
                                    :handler         (fn [data]
                                        ;(navigate-to "/admin")
                                                       )
                                    :error-handler   (fn [{:keys [status status-text]}]
                                        ;(.log js/console "Lol kek")
                                                       )})
                  }
         (t :admin/Yes)]
        [:button {:type         "button"
                  :class        "btn btn-primary"
                  :data-dismiss "modal"}
         (t :admin/No)]]))])


(defn badge-info-block [info]
  (let [{:keys [issuer_content_name issuer_content_url issuer_contact issuer_image creator_name creator_url creator_email creator_image]} info]
    [:div
     (bh/issuer-label-image-link issuer_content_name issuer_content_url issuer_contact issuer_image)
     (bh/creator-label-image-link creator_name creator_url creator_email creator_image)]))


(defn user-info-block [info]
  [:div
   (str "emails:" (:emails info))
   [:br]
   (str "created: ") (date-from-unix-time (* 1000 (:ctime info)) "minutes")
   [:br]
   (str "Last login: ") (date-from-unix-time (* 1000 (:last_login info)) "minutes")])

(defn info-block [info item_type]
  (cond
    (or (= "badge" item_type) (= "badges" item_type)) (badge-info-block info)
    (= "user" item_type) (user-info-block info)
    (= "page" item_type) [:div "page"]
    :else ""))

(defn private-item [item-type item-id visible_area item_owner gallery-state init-data]
  [:div {:class "row"}
    [:div {:class "col-md-12 sub-heading"}
     [:a {:href "#" :on-click #(do (.preventDefault %) (reset! visible_area (if (= "private-item" @visible_area) "" "private-item")))} "Piilota"]]
   (if (= @visible_area "private-item")
     [:div.col-md-12
      (str "Piilota käyttäjien " item_owner " merkki?")
      [:button {:type         "button"
                :class        "btn btn-primary"
                :data-dismiss "modal"
                :on-click #(ajax/POST
                            (path-for (str "/obpv1/admin/private_"item-type"/" item-id))
                            {:response-format :json
                             :keywords?       true
                             :params          {:item-type item-type :item-id item-id}
                             :handler         (fn [data]
                                                (if init-data
                                                  (init-data gallery-state)
                                                  (navigate-to "/admin")))
                             :error-handler   (fn [{:keys [status status-text]}]
                                                (.log js/console (str status " " status-text)))})}
       (t :admin/Yes)]])])

(defn lock-user [mail item_owner_id visible_area item_owner gallery-state init-data]
  [:div {:class "row"}
    [:div {:class "col-md-12 sub-heading"}
     [:a {:href "#" :on-click #(do (.preventDefault %) (reset! visible_area (if (= "lock-user" @visible_area) "" "lock-user")))} "Lukitse käyttäjä" ]]
   (if (= @visible_area "lock-user")
     (let [message @mail]
       [:div.col-md-12
        (str "Piilota käyttäjä " item_owner " ?")
        (message-form mail)
        [:button {:type         "button"
                  :class        "btn btn-primary"
                  :data-dismiss "modal"
                  :on-click     #(ajax/POST
                                  (path-for (str "/obpv1/admin/delete_user/" item_owner_id ))
                                  {:response-format :json
                                   :keywords?       true
                                   :params          message
                                   :handler         (fn [data]
                                                      (if (and (= "success" data) init-data)
                                                        (init-data gallery-state)
                                                        (navigate-to "/admin"))
                                                      )
                                   :error-handler   (fn [{:keys [status status-text]}]
                                        ;(.log js/console "Lol kek")
                                                      )}) }
         (t :admin/Yes)]]))])

(defn admin-modal-container [state]
  (let [{:keys [item_type item_id item_owner_id image_file name info item_owner gallery-state init-data]} @state
        visible_area (cursor state [:visible_area])
        mail (cursor state [:mail])]
    [:div {:id "badge-contents"}
     [:div.row
      [:div {:class "col-sm-3 badge-image modal-left"}
       [:img {:src (profile-picture image_file)} ]]
      [:div {:class "col-sm-9 badge-info"}
       [:div.row
        [:div {:class "col-md-12"}
         [:h1.uppercase-header name]
         (info-block info item_type)]]

       
       (if (not (= item_type "badges"))
         (send-message mail item_owner_id visible_area gallery-state init-data))
       (if (not (= item_type "user"))
         (private-item item_type item_id visible_area item_owner gallery-state init-data))
       (if (not (= item_type "user"))
         (delete-item item_type item_id mail item_owner_id visible_area item_owner gallery-state init-data))
       (if (not (= item_type "badges"))
         (lock-user mail item_owner_id visible_area item_owner gallery-state init-data))]]]))

(defn admin-modal [state]
  [:div
   [:div.modal-header
      [:button {:type         "button"
                :class        "close"
                :data-dismiss "modal"
                :aria-label   "OK"}
       [:span {:aria-hidden             "true"
               :dangerouslySetInnerHTML {:__html "&times;"}}]]]
   [:div.modal-body
    (admin-modal-container state)
      ]
     [:div.modal-footer
      [:button {:type         "button"
                :class        "btn btn-primary"
                :data-dismiss "modal"}
       "close"]]])
