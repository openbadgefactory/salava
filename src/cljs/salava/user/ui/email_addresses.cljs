(ns salava.user.ui.email-addresses
  (:require [reagent.core :refer [atom cursor]]
            [reagent-modals.modals :as modal]
            [salava.core.ui.ajax-utils :as ajax]
            [salava.core.ui.helper :refer [input-valid?]]
            [salava.core.ui.layout :as layout]
            [salava.core.i18n :refer [t]]
            [salava.user.schemas :as schemas]
            [salava.core.countries :refer [all-countries-sorted]]
            [salava.user.ui.input :as input]))

(defn verify-email-address [email verification-key state]
  (ajax/POST
    "/obpv1/user/verify_email"
    {:params  {:email            email
               :verification_key verification-key}
     :handler (fn [{:keys [status]} data]
                (if (= status "success")
                  (let [emails (map #(assoc % :verified (or (:verified %) (= (:email %) email))) (:emails @state))]
                    (swap! state assoc :emails emails :message {:class "alert-success" :content (str (t :user/Emailaddress) " " email " " (t :user/verified))}))
                  (swap! state assoc :message {:class "alert-danger" :content (t :user/Errorwhileverifyingemail)}))
                (modal/close-modal!))}))

(defn add-email-address [state]
  (ajax/POST
    "/obpv1/user/add_email"
    {:params  {:email (:new-address @state)}
     :handler (fn [{:keys [status message new-email]} data]
                (if (= status "success")
                  (let [emails (:emails @state)]
                    (swap! state assoc :emails (conj emails new-email)
                                       :new-address "")))
                (swap! state assoc :message {:class status :content message}))}))

(defn set-primary-email [email state]
  (ajax/POST
    "/obpv1/user/set_primary_email"
    {:params {:email email}
     :handler (fn [{:keys [status]} data]
                (if (= status "success")
                  (let [emails (map #(assoc % :primary_address (= (:email %) email)) (:emails @state))]
                    (swap! state assoc :emails emails :message {:class "alert-success" :content (str (t :user/Emailaddress) " " email " " (t :user/setasprimary))}))
                  (swap! state assoc :message {:class "alert-danger" :content (t :user/Errorsettingprimaryemail)}))
                (modal/close-modal!))}))

(defn delete-email [email state]
  (ajax/POST
    "/obpv1/user/delete_email"
    {:params  {:email email}
     :handler (fn [{:keys [status]} data]
                (if (= status "success")
                  (let [email-removed (filter #(not= (:email %) email) (:emails @state))]
                    (swap! state assoc :emails email-removed :message {:class "alert-success" :content (str (t :user/Emailaddress) " " email " " (t :user/deleted))}))
                  (swap! state assoc :message {:class "alert-danger" :content (t :user/Errordeletingemailaddress)}))
                (modal/close-modal!))}))

(defn email-modal [content action-fn]
  [:div
   [:div.modal-header
    [:button {:type "button"
              :class "close"
              :data-dismiss "modal"
              :aria-label "OK"}
     [:span {:aria-hidden "true"
             :dangerouslySetInnerHTML {:__html "&times;"}}]]]
   [:div.modal-body
    [:div {:class (str "alert alert-warning")}
     [:p content]]]
   [:div.modal-footer
    [:button {:type "button"
              :class "btn btn-primary"
              :on-click action-fn}
     (t :core/Confirm)]
    [:a {:class "modal-cancel-button"
         :data-dismiss "modal"
         :href ""}
     (t :core/Cancel)]]])

(defn verify-modal [email verify-atom state]
  (reset! verify-atom "")
  (fn []
    [:div
     [:div.modal-header
      [:button {:type         "button"
               :class        "close"
               :data-dismiss "modal"
               :aria-label   "OK"}
       [:span {:aria-hidden             "true"
              :dangerouslySetInnerHTML {:__html "&times;"}}]]]
     [:div.modal-body
      [:form.form-horizontal
       [:p (t :user/Theemailaddress) " " [:i email] " " (t :user/iswaitingconfirmation)]
       [:div.form-group
        [:label.col-md-4 {:for "input-email-confirm"}
         (t :user/Verificationcode)]
        [:div.col-md-8
         [input/text-field {:atom verify-atom :name "email-confirm"}]]]]]
     [:div.modal-footer
      [:button {:type     "button"
                :class    "btn btn-primary"
                :on-click #(verify-email-address email @verify-atom state)}
       (t :user/Verifyemail)]
      [:a {:class "modal-cancel-button"
           :data-dismiss "modal"
           :href ""}
       (t :core/Cancel)]]]))

(defn email-options [email verified? state]
  (let [delete-email-fn (fn [] (delete-email email state))
        set-primary-fn (fn [] (set-primary-email email state))]
    [:div
     [:a {:href     "#"
         :on-click #(modal/modal! [email-modal (t :user/Areyousuredelete) delete-email-fn])}
     (t :core/Delete)]
     (if verified?
       [:span
        [:span.separate "|"]
        [:a {:href     "#"
             :on-click #(modal/modal! [email-modal (t :user/Areyousureprimary) set-primary-fn])}
         (t :user/Setasloginaddress)]])]))

(defn email-address-table [state]
  [:table.table
   [:thead
    [:tr
     [:th (t :user/Email)]
     [:th (t :user/Verified)]
     [:th (t :user/Actions)]]]
   (into [:tbody]
         (for [address (sort-by :ctime (:emails @state))
               :let [{:keys [email verified primary_address]} address]]
           [:tr
            [:td email]
            [:td (if verified
                   (t :core/Yes)
                   [:a {:href "#"
                        :on-click #(modal/modal! [verify-modal email (cursor state [:verify-code]) state] {:size :lg})}
                    (t :user/Clicktoverify)])]
            [:td (if primary_address
                   (t :user/Loginaddress)
                   (email-options email verified state))]]))])

(defn content [state]
  (let [new-address-atom (cursor state [:new-address])
        message-atom (cursor state [:message])]
    [:div {:class "panel"
           :id "edit-user"}
     [modal/modal-window]
     [:div {:class "panel-body"}
      (if @message-atom
        [:div {:class (str "alert " (:class @message-atom))}
         (:content @message-atom)])
      (email-address-table state)
      [:form
       [:div.form-group
        [:label {:for "input-new-address"}
         (t :user/Addnewemail)]
        [input/text-field {:name "new-address" :atom new-address-atom}]]
       [:button {:class    "btn btn-primary"
                 :disabled (not (input/email-valid? @new-address-atom))
                 :on-click #(do
                             (.preventDefault %)
                             (add-email-address state))}
        (t :core/Save)]]]]))

(defn init-data [state]
  (ajax/GET
    (str "/obpv1/user/email-addresses/")
    {:handler (fn [data]
                (swap! state assoc :emails data))}))

(defn handler [site-navi]
  (let [state (atom {:emails []
                     :new-address ""
                     :verify-code ""
                     :message nil})]
    (init-data state)
    (fn []
      (layout/default site-navi (content state)))))
