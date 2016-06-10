(ns salava.user.ui.register
  (:require [reagent.core :refer [atom cursor]]
            [salava.core.ui.ajax-utils :as ajax]
            [salava.core.ui.layout :as layout]
            [salava.core.ui.helper :refer [path-for]]
            [salava.core.countries :refer [all-countries-sorted]]
            [salava.core.i18n :refer [t]]
            [salava.user.ui.input :as input]))

(defn send-registration [state]
  (let [{:keys [email first-name last-name country language]} @state]
    (ajax/POST
     (path-for "/obpv1/user/register/")
     {:params  {:email email
                :first_name first-name
                :last_name last-name
                :country country
                :language language}
      :handler (fn [data]
                 (if (= (:status data) "error")
                   (swap! state assoc :error-message (:message data))
                   (swap! state assoc :registration-sent true)))})))

(defn registration-form [state]
  (let [email-atom (cursor state [:email])
        first-name-atom (cursor state [:first-name])
        language-atom (cursor state [:language])
        last-name-atom (cursor state [:last-name])
        country-atom (cursor state [:country])
        languages (:languages @state)]
    [:form {:class "form-horizontal"}
     (if (:error-message @state)
       [:div {:class "alert alert-danger" :role "alert"}
        (:error-message @state)])

     [:div.form-group
      [:label {:class "col-xs-4"
               :for "input-email"}
       (t :user/Email)
       [:span.form-required " *"]]
      [:div.col-xs-8
       [:div {:class (str "form-bar " (if (input/email-valid? @email-atom) "form-bar-success" "form-bar-error"))}
        [input/text-field {:name "email" :atom email-atom}]]]
      [:div.col-xs-12
       (t :user/Emailinfotext)]]

     [:div.form-group
      [:label {:class "col-xs-4"
               :for "input-first-name"}
       (t :user/Firstname)
       [:span.form-required " *"]]
      [:div.col-xs-8
       [:div {:class (str "form-bar " (if (input/first-name-valid? @first-name-atom) "form-bar-success" "form-bar-error"))}
        [input/text-field {:name "first-name" :atom first-name-atom}]]]]

     [:div.form-group
      [:label {:class "col-xs-4"
               :for "input-last-name"}
       (t :user/Lastname)
       [:span.form-required " *"]]
      [:div.col-xs-8
       [:div {:class (str "form-bar " (if (input/last-name-valid? @last-name-atom) "form-bar-success" "form-bar-error"))}
        [input/text-field {:name "last-name" :atom last-name-atom}]]]]

     

     [:div.form-group
      [:label {:class "col-xs-4"
               :for "input-language"}
       (t :user/Language)
       [:span.form-required " *"]]
      [:div.col-xs-8
       [:div {:class (str "form-bar " (if (input/language-valid? @language-atom) "form-bar-success" "form-bar-error"))}
        [input/radio-button-selector languages language-atom]]]]

     [:div.form-group
      [:label {:class "col-xs-4"
               :for "input-country"}
       (t :user/Country)
       [:span.form-required " *"]]
      [:div.col-xs-8
       [:div {:class (str "form-bar " (if (input/country-valid? @country-atom) "form-bar-success" "form-bar-error"))}
        [input/country-selector country-atom]]]]

     [:button {:class    "btn btn-primary"
               :disabled (if-not (and (input/email-valid? @email-atom)
                                      (input/first-name-valid? @first-name-atom)
                                      (input/last-name-valid? @last-name-atom)
                                      (input/country-valid? @country-atom)
                                      (input/language-valid? @language-atom))
                           "disabled")
               :on-click #(do
                           (.preventDefault %)
                           (send-registration state))}
      (t :user/Createnewaccount)]]))

(defn content [state]
  [:div {:id "registration-page"}
   [:div {:id "narrow-panel"
          :class "panel"}
    [:div.panel-body
     (if (:registration-sent @state)
       [:div {:class "alert alert-success"
              :role "alert"}
        (t :user/Welcomemessagesent) "."]
       (registration-form state))]]])

(defn init-data [state]
  (ajax/GET
    (path-for "/obpv1/user/register" true)
    {:handler (fn [data]
                (let [{:keys [languages]} data]
                  (swap! state assoc :languages languages)))}))

(defn handler []
  (let [state (atom {:email ""
                     :first-name ""
                     :last-name ""
                     :language ""
                     :country ""
                     :languages []
                     :error-message nil
                     :registration-sent nil})]
    (init-data state)    
    (fn []
      (layout/landing-page (content state)))))
