(ns salava.admin.ui.tickets
  (:require [reagent.core :refer [atom cursor]]
            [reagent.session :as session]
            [reagent-modals.modals :as m]
            [clojure.set :as set :refer [intersection]]
            [clojure.string :refer [trim]]
            [salava.core.ui.ajax-utils :as ajax]
            [salava.core.ui.layout :as layout]
            [salava.core.ui.grid :as g]
            [salava.core.ui.helper :refer [path-for unique-values]]
            [salava.core.i18n :refer [t]]
            [salava.core.helper :refer [dump]]
            [salava.admin.ui.helper :refer [message-form email-select status-handler]]
            [salava.core.time :refer [date-from-unix-time]]))

(defn text-shorter [text length]
  (let [sorted-text (subs text 0 (min (count text) length))
        show-atom (atom true)]
    (fn []
      (if @show-atom
        [:p sorted-text " " [:a {:href "#" :on-click #(do
                                                        (reset! show-atom false)
                                                        (.preventDefault %))} (t :admin/Showmore)]]
        [:div text " " [:a {:href "#" :on-click #(do
                                                   (reset! show-atom true)
                                                   (.preventDefault %))} (t :admin/Showless)] ]))
    ))


(defn ticket-send-message [state name]
(let [{:keys [user_id gallery-state init-data info]} @state
        mail (cursor state [:mail])
        status (cursor state [:status])
        email-atom (cursor state [:selected-email])
        ]

  [:div {:class "row flip"}
   [:div {:class "col-xs-12"}
      [:div.form-group
        [:label
         (str (t :user/Email) ":")]
        (email-select (:emails info) email-atom) ]
      (message-form mail)
      [:button {:type         "button"
                :class        "btn btn-primary pull-right"
                :disabled (if-not (and
                                   (< 1 (count (:subject @mail)))
                                   (< 1 (count (:message @mail))))
                            "disabled")
                :on-click     #(ajax/POST
                                (path-for (str "/obpv1/admin/send_message/" user_id ))
                                {:response-format :json
                                 :keywords?       true
                                 :params        {:subject (:subject @mail)
                                                 :message (:message @mail)
                                                 :email  @email-atom}
                                 :handler         (fn [data]
                                                    (reset! status data)
                                                    (reset! mail {:subject ""
                                                                  :message ""})
                                                      )
                                 :error-handler   (fn [{:keys [status status-text]}]
                                                    (.log js/console (str status " " status-text))
                                                    )})
                }
       (t :admin/Sendmessage)]
       (status-handler status "user")]
     ])
  )


(defn ticket-modal-container [state]
  (let [{:keys [user user_id info email]} @state]
    [:div {:class "admin-modal"}
     [:div.row
      [:div {:class "col-sm-12 badge-info"}
       [:div {:class "col-xs-12"}
        (ticket-send-message state user)]]]]))


(defn ticket-modal [state]
  [:div
   [:div.modal-header
      [:button {:type         "button"
                :class        "close"
                :data-dismiss "modal"
                :aria-label   "OK"}
       [:span {:aria-hidden             "true"
               :dangerouslySetInnerHTML {:__html "&times;"}}]]
    [:h4 {:class "modal-title"} (t :admin/Sendmessagetoreporter)]]
   [:div.modal-body
    (ticket-modal-container state)
      ]])

(defn get-closed-tickets [state]
  (ajax/GET
   (path-for "/obpv1/admin/closed_tickets/")
   {:handler (fn [tickets]
               (swap! state assoc :tickets tickets))}))

(defn init-data [state]
  (ajax/GET
   (path-for "/obpv1/admin/tickets/")
   {:handler (fn [tickets]
               (swap! state assoc :tickets tickets))}))

(defn open-ticket-modal [user-id name gallery-state init-data]
  (let [state (atom {:mail          {:subject ""
                                     :message ""}
                     :user    ""
                     :user_id ""
                     :image_file    nil
                     :info          {}
                     :gallery-state gallery-state
                     :init-data init-data
                     :status ""})]
    (ajax/GET
     (path-for (str "/obpv1/admin/user/" user-id))
     {:handler (fn [data]
                 (do
                   (let [primary-email (first (filter #(:primary_address %) (get-in data [:info :emails])))]
                     (swap! state assoc :name (:name data)
                            :image_file (:image_file data)
                            :user (:item_owner data)
                            :user_id (:item_owner_id data)
                            :selected-email (:email primary-email)
                            :info (:info data))))
                 (m/modal! [ticket-modal state init-data]))})))



(defn ticket [ticket-data state]
  (let [{:keys [item_type item_id item_name first_name last_name item_url reporter_id description ctime report_type id status mtime]} ticket-data
        open? (= "open" status)]
    [:div {:class (str "media " report_type) :id "ticket-container"}
     [:div.media-body
      [:div {:class (str "title-bar title-bar-" report_type ) :id (if open? "" "closed")}
       [:div {:id "ticket" :class "pull-right"}  (if open? (date-from-unix-time (* 1000 ctime) "minutes") (str (t :admin/Closed) ": " (date-from-unix-time (* 1000 mtime) "minutes")))]
       [:h3 {:class "media-heading"}
        [:u (t (keyword (str "admin/" report_type)))]]
       [:h4 {:class "media-heading"}
        [:a {:href item_url :target "_blank"}
         (str (t (keyword (str "admin/" item_type))) " - " item_name)]]]
      [:div.media-descriprtion
       [:div {:class "col-xs-12"  :id (if open? "" "closed")}
        [:div [:label (str (t :admin/Description) ": ")] " " (if (< 130 (count description)) [text-shorter description 130]   description) ]
        [:div [:label (str (t :admin/Reporter) ": ")] " " [:a {:href (path-for (str "/user/profile/" reporter_id))}(str first_name " " last_name)]]]
       [:button {:class    "btn btn-primary"
                 :disabled (not open?)
                 :on-click #(do
                              (.preventDefault %)
                              (open-ticket-modal reporter_id (str first_name " " last_name) init-data state)
                              )}
        (t :admin/Sendmessagetoreporter)]
       [:button {:class    "btn btn-primary pull-right"
                 :on-click #(do
                              (.preventDefault %)
                              (ajax/POST
                               (path-for (str "/obpv1/admin/close_ticket/" id))
                               {:response-format :json
                                :keywords? true
                                :params {:new-status (if open? "closed" "open")}
                                :handler (fn [data]
                                           (if open?
                                             (init-data state)
                                             (get-closed-tickets state))
                                           )
                                :error-handler (fn [{:keys [status status-text]}])}))}
        (if open? (t :admin/Done) (t :admin/Restore))]]]]))

(defn ticket-visible? [element state]
  (if (or (> (count
              (intersection
               (into #{} (:types-selected @state))
               #{(:report_type element)}))
             0)
          (= (:types-all @state)
             true))
    true false))

(defn grid-buttons-with-translates [title buttons key all-key state]
  [:div.form-group.flip
   [:label {:class "control-label col-sm-2"} title]
   [:div.col-sm-10
    (let [all-checked? (= ((keyword all-key) @state) true)
          buttons-checked ((keyword key) @state)]
      [:div.buttons
       [:button {:class (str "btn btn-default " (if all-checked? "btn-active"))
                 :id "btn-all"
                 :on-click (fn []
                             (swap! state assoc (keyword key) [])
                             (swap! state assoc (keyword all-key) true))}
        (t :core/All)]
       (doall
        (for [button buttons]
          (let [value button
                checked? (boolean (some #(= value %) buttons-checked))]
            [:button {:class    (str "btn btn-default " value " " (if checked? "btn-active"))
                      :key      value
                      :on-click (fn []
                                  (swap! state assoc (keyword all-key) false)
                                  (if checked?
                                    (do
                                      (if (= (count buttons-checked) 1)
                                        (swap! state assoc (keyword all-key) true))
                                      (swap! state assoc (keyword key)
                                             (remove (fn [x] (= x value)) buttons-checked)))
                                    (swap! state assoc (keyword key)
                                           (conj buttons-checked value))))}
             (t (keyword (str "admin/" value)))])))])]])

(defn grid-show-closed-tickets [state]
  (let [show-closed?  (cursor state [:show-closed?])]
    [:div {:id "archived" :class "form-group flip"}
     [:label {:class "control-label col-sm-2 col-xs-3"}
      (str (t :admin/Archived) ": ")]
     [:div.col-sm-10.col-xs-9
      [:label
       [:input {:name      "visibility"
                :type      "checkbox"
                :on-change #(do
                              (reset! show-closed? (if @show-closed? false true))
                              (if @show-closed?
                                (get-closed-tickets state)
                                (init-data state)))
                :checked   @show-closed?}]]]]))

(defn content [state]
  (let [tickets (:tickets @state)]
    [:div
     [:div {:id    "grid-filter"
            :class "form-horizontal"}
      [grid-buttons-with-translates (str (t :admin/Types) ":")  (unique-values :report_type tickets) :types-selected :types-all state]
      [grid-show-closed-tickets state]]
     [:div
      (into [:div {:class "row"}]
            (for [data tickets]
              (if (ticket-visible? data state)
                (ticket data state))))]
     [m/modal-window]]))

(defn handler [site-navi]
  (let [state (atom {:show-closed? false
                     :tickets []
                     :types-selected []
                     :types-all true})]
    (init-data state)
    (fn []
      (layout/default site-navi (content state)))))
