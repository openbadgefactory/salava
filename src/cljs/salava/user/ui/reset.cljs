(ns salava.user.ui.reset
  (:require [reagent.core :refer [atom cursor]]
            [salava.core.ui.ajax-utils :as ajax]
            [salava.core.ui.layout :as layout]
            [salava.core.ui.helper :refer [path-for]]
            [salava.core.countries :refer [all-countries-sorted]]
            [salava.core.i18n :refer [t]]
            [salava.user.ui.input :as input]))

(defn send-password-reset-link [state]
  (let [email (:email @state)]
    (ajax/POST
      (path-for "/obpv1/user/reset/")
      {:params  {:email email}
       :handler (fn [data]
                  (swap! state assoc :reset-link-sent (:status data)))})))

(defn content [state]
  (let [email-atom (cursor state [:email])]
    [:div {:id "reset-password"}
     [:div {:id "narrow-panel"
            :class "panel"}
      [:div.panel-body
       (cond
         (= "success" (:reset-link-sent @state)) [:div {:class "alert alert-success" :role "alert"}
                                                  (t :user/Passwordresetlinksent) ": " (:email @state)]
         (= "error" (:reset-link-sent @state)) [:div {:class "alert alert-warning" :role "alert"}
                                                (t :user/Errorsendingresetlink)])
       [:form
        [:div.form-group
         [:label {:for "input-email"} (t :user/Emailaddress)]
         [input/text-field {:name "email" :atom email-atom}]]
        [:button {:class "btn btn-primary"
                  :on-click #(do (.preventDefault %) (send-password-reset-link state))
                  :disabled (not (input/email-valid? @email-atom))}
         (t :user/Sendpasswordresetlink)]]]]]))

(defn handler []
  (let [state (atom {:email ""
                     :reset-link-sent nil})]
    (fn []
      (layout/landing-page (content state)))))
