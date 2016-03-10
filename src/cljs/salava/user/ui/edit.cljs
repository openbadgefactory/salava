(ns salava.user.ui.edit
  (:require [reagent.core :refer [atom cursor]]
            [salava.core.ui.ajax-utils :as ajax]
            [salava.core.ui.helper :refer [input-valid? navigate-to]]
            [salava.core.ui.layout :as layout]
            [salava.core.i18n :refer [t]]
            [salava.core.common :refer [deep-merge]]
            [salava.user.schemas :as schemas]
            [salava.core.countries :refer [all-countries-sorted]]
            [salava.user.ui.input :as input]))

(defn save-user-info [state]
  (ajax/POST
    "/obpv1/user/edit"
    {:params  (:user @state)
     :handler (fn [data]
                (navigate-to "/user/edit"))}))

(defn new-password-valid? [current-password new-password new-password-verify]
  (if (or (not-empty new-password) (not-empty new-password-verify))
    (and (input/password-valid? new-password)
         (input/password-valid? new-password-verify)
         (= new-password new-password-verify)
         current-password)
    true))

(defn content [state]
  (let [current-password-atom (cursor state [:user :current_password])
        new-password-atom (cursor state [:user :new_password])
        new-password-verify-atom (cursor state [:user :new_password_verify])
        language-atom (cursor state [:user :language])
        first-name-atom (cursor state [:user :first_name])
        last-name-atom (cursor state [:user :last_name])
        country-atom (cursor state [:user :country])
        message (:message @state)]
    [:div {:class "panel" :id "edit-user"}
     [:div {:class "panel-body"}
      [:form.form-horizontal
       (if message
         [:div {:class (str "alert " (:class message))}
          (:content message)])
       [:div.form-group
        [:label {:for "input-current-password" :class "col-md-3"} (t :user/Currentpassword)]
        [:div {:class "col-md-9"}
         [input/text-field {:name "current-password" :atom current-password-atom :password? true}]]
        [:div.col-md-12 (t :user/Enteryourcurrentpassword)]]

       [:div.form-group
        [:label {:for "input-new-password" :class "col-md-3"} (t :user/Newpassword)]
        [:div {:class "col-md-9"}
         [input/text-field {:name "new-password" :atom new-password-atom :password? true}]]]

       [:div.form-group
        [:label {:for "input-new-password-verify" :class "col-md-3"} (t :user/Confirmnewpassword)]
        [:div {:class "col-md-9"}
         [input/text-field {:name "new-password-verify" :atom new-password-verify-atom :password? true}]]
        [:div.col-md-12 (t :user/Tochangecurrentpassword)]]

       [:div.form-group
        [:label {:class "col-md-3"}
         (t :user/Language)]
        (into [:div {:class "col-md-9"}]
              (for [language (:languages @state)]
                [:label.radio-inline
                 [:input {:type      "radio"
                          :name      "language"
                          :value     language
                          :checked   (= @language-atom language)
                          :on-click  #(reset! language-atom language)}]
                 (t (keyword (str "core/" language)))]))]
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
                                          (new-password-valid? @current-password-atom @new-password-atom @new-password-verify-atom))
                               "disabled")
                   :on-click #(do
                               (.preventDefault %)
                               (save-user-info state))}
          (t :core/Save)]
         [:a {:id "cancel-button" :class "btn btn-warning" :href "/user/cancel"} (t :user/Cancelaccount)]]]]]]))

(def initial-state
  {:user {:current_password nil
          :new_password nil
          :new_password_verify nil}
   :message nil})

(defn init-data [state]
  (ajax/GET
    (str "/obpv1/user/edit/")
    {:handler (fn [data]
                (reset! state (deep-merge initial-state (update-in data [:user] dissoc :id))))}))

(defn handler [site-navi]
  (let [state (atom {})]
    (init-data state)
    (fn []
      (layout/default site-navi (content state)))))
