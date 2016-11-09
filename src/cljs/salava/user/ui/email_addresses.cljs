(ns salava.user.ui.email-addresses
  (:require [reagent.core :refer [atom cursor]]
            [reagent-modals.modals :as modal]
            [salava.core.ui.ajax-utils :as ajax]
            [salava.core.ui.helper :refer [input-valid? path-for str-cat]]
            [salava.core.ui.layout :as layout]
            [salava.core.i18n :refer [t]]
            [salava.user.schemas :as schemas]
            [salava.core.helper :refer [dump]]
            [salava.core.countries :refer [all-countries-sorted]]
            [salava.user.ui.input :as input]))

(defn add-email-address [state]
  (ajax/POST
    (path-for "/obpv1/user/add_email")
    {:params  {:email (:new-address @state)}
     :handler (fn [{:keys [status message new-email]} data]
                (if (= status "success")
                  (let [emails (:emails @state)]
                    (swap! state assoc :emails (conj emails new-email)
                           :new-address ""
                           :message {:class "alert-success" :content (str (t :user/Confirmyouremailbody1) " " (:email new-email) ". " (t :user/Confirmyouremailbody2) ".")}))
                  (swap! state assoc :message {:class "alert-danger" :content (t (keyword message))})))}))

(defn set-primary-email [email state]
  (ajax/POST
    (path-for "/obpv1/user/set_primary_email")
    {:params {:email email}
     :handler (fn [{:keys [status]} data]
                (if (= status "success")
                  (let [emails (map #(assoc % :primary_address (= (:email %) email)) (:emails @state))]
                    (swap! state assoc :emails emails :message {:class "alert-success" :content (str (t :user/Emailaddress) " " email " " (t :user/setasprimary))}))
                  (swap! state assoc :message {:class "alert-danger" :content (t :user/Errorsettingprimaryemail)}))
                (modal/close-modal!))}))

(defn delete-email [email state]
  (ajax/POST
    (path-for "/obpv1/user/delete_email")
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
     [:th.text-center (t :user/Verified)]
     [:th (t :user/Actions)]]]
   (into [:tbody]
         (for [address (sort-by :ctime (:emails @state))
               :let [{:keys [email verified primary_address]} address]]
           [:tr
            [:td email]
            [:td.text-center (if verified [:i {:class "fa fa-check"}])]
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
        (t :core/Add)]]]]))


(defn init-data [state]
  (ajax/GET
    (path-for "/obpv1/user/email-addresses" true)
    {:handler (fn [data]
                (do
                  (swap! state assoc :emails data)
                  (if (not-empty (filter #(not (:verified %)) data))
                    (let [not-verified-emails (map #(:email %) (filter #(not (:verified %)) data))]
                      (swap! state assoc :message {:class "alert alert-warning" :content (str (if (= 1 (count not-verified-emails)) (t :user/Confirmemailaddress) (t :user/Confirmemailaddresses)) " " (str-cat not-verified-emails) )}))
                    )))}))

(defn handler [site-navi]
  (let [state (atom {:emails []
                     :new-address ""
                     :message nil})]
    (init-data state)
    (fn []
      (layout/default site-navi (content state)))))
