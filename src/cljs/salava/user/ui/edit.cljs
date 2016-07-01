(ns salava.user.ui.edit
  (:require [reagent.core :refer [atom cursor]]
            [clojure.string :refer [blank?]]
            [salava.core.ui.ajax-utils :as ajax]
            [salava.core.ui.helper :refer [input-valid? navigate-to path-for]]
            [salava.core.ui.layout :as layout]
            [salava.core.i18n :refer [t translate-text]]
            [salava.core.common :refer [deep-merge]]
            [salava.user.schemas :as schemas]
            [salava.core.countries :refer [all-countries-sorted]]
            [salava.user.ui.input :as input]))

(defn clear-password-fields [state]
  (do
    (swap! state assoc-in [:user :current_password] "")
    (swap! state assoc-in [:user :new_password] "")
    (swap! state assoc-in [:user :new_password_verify] "")))

(defn save-user-info [state]
  (let [params (:user @state)
        current-password (if-not (blank? (:current_password params)) (:current_password params))
        new-password (if-not (blank? (:new_password params)) (:new_password params))
        new-password-verify (if-not (blank? (:new_password_verify params)) (:new_password_verify params))]
    (ajax/POST
      (path-for "/obpv1/user/edit")
      {:params  (-> params
                    (dissoc :password?)
                    (assoc :current_password current-password
                           :new_password new-password
                           :new_password_verify new-password-verify))
       :handler (fn [data]
                  (if (= (:status data) "success")
                    (do
                      (swap! state assoc :message {:class "alert-success" :content (:message data)})
                      (clear-password-fields state))
                    (do
                      (swap! state assoc :message {:class "alert-danger" :content (:message data)})
                      (clear-password-fields state)))
                  )})))

(defn new-password-valid? [has-password? current-password new-password new-password-verify]
  (or
   (and (empty? new-password)
        (empty? new-password-verify)
        (empty? current-password))
   (and (not-empty new-password)
        (not-empty new-password-verify)
        (input/password-valid? new-password)
        (input/password-valid? new-password-verify)
        (= new-password new-password-verify)
        (or (not has-password?) (not-empty current-password)))))
(defn content [state]
  (let [current-password-atom (cursor state [:user :current_password])
        new-password-atom (cursor state [:user :new_password])
        new-password-verify-atom (cursor state [:user :new_password_verify])
        language-atom (cursor state [:user :language])
        first-name-atom (cursor state [:user :first_name])
        last-name-atom (cursor state [:user :last_name])
        country-atom (cursor state [:user :country])
        message (:message @state)
        current-password? (get-in @state [:user :password?])]
    [:div {:class "panel" :id "edit-user"}
     
     (if message
       [:div {:class (str "alert " (:class message))}
       (translate-text (:content message)) ])
     [:div {:class "panel-body"}
      [:form.form-horizontal
       
       (if current-password?
         [:div.form-group
          [:label {:for "input-current-password" :class "col-md-3"} (t :user/Currentpassword)]
          [:div {:class "col-md-9"}
           [:input {:class       "form-control"
                    :id          "input-current-password"
                    :name        name
                    :type        "password"
                    :placeholder (t :user/Enteryourcurrentpassword)
                    :read-only   true
                    :on-focus    #(.removeAttribute (.-target %) "readonly")
                    :on-change   #(reset! current-password-atom (.-target.value %))
                    :value       @current-password-atom}]]])
       [:div.form-group
        [:div.col-md-12 (t :user/Tochangecurrentpassword)]
        [:br]
        [:label {:for "input-new-password" :class "col-md-3"} (t :user/Newpassword)]
        [:div {:class "col-md-9"}
         [input/text-field {:name "new-password" :atom new-password-atom :password? true}]]]

       [:div.form-group
        [:label {:for "input-new-password-verify" :class "col-md-3"} (t :user/Confirmnewpassword)]
        [:div {:class "col-md-9"}
         [input/text-field {:name "new-password-verify" :atom new-password-verify-atom :password? true}]]]
       
       [:div.form-group
        [:label {:for "languages"
                 :class "col-md-3"}
         (t :user/Language)]
        [:div.col-md-9
         [input/radio-button-selector (:languages @state) language-atom]]]

       [:div.form-group
        [:label {:for "input-first-name" :class "col-md-3"} (t :user/Firstname)]
        [:div {:class "col-md-9"}
         [input/text-field {:name "first-name" :atom first-name-atom}]]]

       [:div.form-group
        [:label {:for "input-last-name" :class "col-md-3"} (t :user/Lastname)]
        [:div {:class "col-md-9"}
         [input/text-field {:name "last-name" :atom last-name-atom}]]]
       
       [:div.form-group
        [:label {:for "input-country"
                 :class "col-md-3"}
         (t :user/Country)]
        [:div.col-md-9
         [input/country-selector country-atom]]]

       [:div.row
        [:div.col-xs-12
         [:button {:class "btn btn-primary"
                   :disabled (if-not (and (input/first-name-valid? @first-name-atom)
                                          (input/last-name-valid? @last-name-atom)
                                          (input/country-valid? @country-atom)
                                          
                                          (new-password-valid? current-password? @current-password-atom @new-password-atom @new-password-verify-atom))
                               "disabled")
                   :on-click #(do
                               (.preventDefault %)
                               (save-user-info state))}
          (t :core/Save)]
         [:a {:id "cancel-button" :class "btn btn-warning" :href (path-for "/user/cancel")} (t :user/Cancelaccount)]]]]]]))

(def initial-state
  {:user {:current_password nil
          :new_password nil
          :new_password_verify nil}
   :message nil})

(defn init-data [state]
  (ajax/GET
    (path-for "/obpv1/user/edit" true)
    {:handler (fn [data]
                (reset! state (deep-merge initial-state (update-in data [:user] dissoc :id))))}))

(defn handler [site-navi]
  (let [state (atom {})]
    (init-data state)
    (fn []
      (layout/default site-navi (content state)))))
