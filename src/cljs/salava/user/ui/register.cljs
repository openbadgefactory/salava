(ns salava.user.ui.register
  (:require [reagent.core :refer [atom cursor]]
            [ajax.core :as ajax]
            [clojure.walk :as walk :refer [keywordize-keys]]
            [salava.core.ui.helper :refer [input-valid?]]
            [salava.core.ui.layout :as layout]
            [salava.core.countries :refer [all-countries-sorted]]
            [salava.core.i18n :refer [t]]
            [salava.user.schemas :as schemas]
            [salava.core.helper :refer [dump]]))

(defn email-valid? [email-address]
  (input-valid? (:email schemas/User) email-address))

(defn first-name-valid? [first-name]
  (input-valid? (:first_name schemas/User) first-name))

(defn last-name-valid? [last-name]
  (input-valid? (:last_name schemas/User) last-name))

(defn country-valid? [country]
  (input-valid? (:country schemas/User) country))

(defn register [state]
  (let [{:keys [email first-name last-name country]} @state]
    (ajax/POST
      (str "/obpv1/user/register/")
      {:params  {:email email
                 :first_name first-name
                 :last_name last-name
                 :country country}
       :handler (fn [data]
                  (let [data-with-kws (keywordize-keys data)]
                    (if (= (:status data-with-kws) "error")
                      (swap! state assoc :error-message (:message data-with-kws))
                      (swap! state assoc :registration-sent true))))})))

(defn login-form [state]
  (let [email-atom (cursor state [:email])
        first-name-atom (cursor state [:first-name])
        last-name-atom (cursor state [:last-name])
        country-atom (cursor state [:country])]
    [:div {:class "form form-horizontal"}
     (if (:error-message @state)
       [:div {:class "alert alert-danger" :role "alert"}
        (:error-message @state)])
     [:div.form-group
      [:label {:class "col-xs-4"
               :for "input-email"}
       (t :user/Email)
       [:span.form-required " *"]]
      [:div.col-xs-8
       [:div {:class (str "form-bar " (if (email-valid? @email-atom) "form-bar-success" "form-bar-error"))}
        [:input {:id "input-email"
                 :class "form-control"
                 :type "text"
                 :on-change #(reset! email-atom (.-target.value %))
                 :value @email-atom}]]]]
     [:div.row
      [:div.col-xs-12
       (t :user/Emailinfotext)]]
     [:div.form-group
      [:label {:class "col-xs-4"
               :for "input-first-name"}
       (t :user/Firstname)
       [:span.form-required " *"]]
      [:div.col-xs-8
       [:div {:class (str "form-bar " (if (first-name-valid? @first-name-atom) "form-bar-success" "form-bar-error"))}
        [:input {:id "input-first-name"
                 :class "form-control"
                 :type "text"
                 :on-change #(reset! first-name-atom (.-target.value %))
                 :value @first-name-atom}]]]]
     [:div.form-group
      [:label {:class "col-xs-4"
               :for "input-last-name"}
       (t :user/Lastname)
       [:span.form-required " *"]]
      [:div.col-xs-8
       [:div {:class (str "form-bar " (if (last-name-valid? @last-name-atom) "form-bar-success" "form-bar-error"))}
        [:input {:id "input-last-name"
                 :class "form-control"
                 :type "text"
                 :on-change #(reset! last-name-atom (.-target.value %))
                 :value @last-name-atom}]]]]
     [:div.form-group
      [:label {:class "col-xs-4"
               :for "input-country"}
       (t :user/Country)
       [:span.form-required " *"]]
      [:div.col-xs-8
       [:div {:class (str "form-bar " (if (country-valid? @country-atom) "form-bar-success" "form-bar-error"))}
        [:select {:id "input-country"
                  :class "form-control"
                  :value     @country-atom
                  :on-change #(reset! country-atom (.-target.value %))}
         [:option {:value ""
                   :key ""}
          "- " (t :user/Choosecountry) " -"]
         (for [[country-key country-name] (map identity all-countries-sorted)]
           [:option {:value country-key
                     :key country-key} country-name])]]
       ]]
     [:button {:class    "btn btn-primary"
               :disabled (if-not (and (email-valid? @email-atom)
                                      (first-name-valid? @first-name-atom)
                                      (last-name-valid? @last-name-atom)
                                      (country-valid? @country-atom))
                           "disabled")
               :on-click #(register state)}
      (t :user/Createnewaccount)]]))

(defn content [state]
  [:div {:id "login-panel"
         :class "panel"}
   [:div.panel-body
    (if (:registration-sent @state)
      [:div {:class "alert alert-success"
             :role "alert"}
       (t :user/Welcomemessagesent)]
      (login-form state))]])

(defn handler []
  (let [state (atom {:email ""
                     :first-name ""
                     :last-name ""
                     :country ""
                     :error-message nil
                     :registration-sent nil})]
    (fn []
      (layout/landing-page (content state)))))
