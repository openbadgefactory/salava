(ns salava.user.ui.reset
  (:require [reagent.core :refer [atom cursor]]
            [reagent.session :as session]
            [salava.core.ui.ajax-utils :as ajax]
            [salava.core.ui.layout :as layout]
            [salava.core.ui.helper :refer [path-for]]
            [salava.core.countries :refer [all-countries-sorted]]
            [salava.core.i18n :refer [t]]
            [salava.user.ui.input :as input]
            [dommy.core :as dommy :refer-macros [sel sel1]]
            [clojure.string :as string]))

(defn send-password-reset-link [state]
  (swap! state assoc :reset-status "in-progress")
  (let [email (:email @state)]
    (ajax/POST
     (path-for "/obpv1/user/reset/")
     {:params  {:email email}
      :handler (fn [data]
                 (swap! state assoc :reset-status (:status data)))})))

(defn content [state]
  (let [email-atom (cursor state [:email])]
    [:div {:id "reset-password"}
     [:div {:id "narrow-panel"
            :class "panel"}
      [:div.panel-heading [:h1.sectiontitle {:style {:padding "0 20px" :font-size "24px"}} (t :user/Requestnewpassword)]]
      [:div.panel-body
       (cond
         (= "in-progress" (:reset-status @state)) [:div.text-center
                                                   [:progress]]
         (= "success" (:reset-status @state)) [:div {:class "alert alert-success" :role "alert"}
                                               (t :user/Passwordresetlinksent) ": " (:email @state)]
         (= "error" (:reset-status @state)) [:div {:class "alert alert-warning" :role "alert"}
                                             (t :user/Errorsendingresetlink)])
       [:form {:style {:display (if (:reset-status @state) "none" "block")}}
        [:div.form-group
         [:label {:for "input-email"} (t :user/Emailaddress)]
         [input/text-field {:name "email" :atom email-atom}]]
        [:button {:class "btn btn-primary"
                  :on-click #(do (.preventDefault %) (send-password-reset-link state))
                  :disabled (not (input/email-valid? @email-atom))}
         (t :user/Sendpasswordresetlink)]]]]]))

(defn handler [site-navi params]
  (let [state (atom {:email ""
                     :reset-status nil})
        lang (or (:lang params) (session/get-in [:user :language] (-> (or js/window.navigator.userLanguage js/window.navigator.language) (string/split #"-") first)))]
    (when (and lang (some #(= lang %) (session/get :languages)))
      (session/assoc-in! [:user :language] lang)
      (-> (sel1 :html) (dommy/set-attr! :lang lang)))

    (fn []
      (layout/landing-page site-navi (content state)))))
