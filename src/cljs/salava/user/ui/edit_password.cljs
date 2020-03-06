(ns salava.user.ui.edit-password
  (:require [reagent.core :refer [atom cursor]]
            [reagent.session :as session]
            [reagent-modals.modals :as m]
            [clojure.string :refer [blank?]]
            [salava.core.ui.ajax-utils :as ajax]
            [salava.core.ui.helper :refer [input-valid? js-navigate-to path-for plugin-fun]]
            [salava.core.ui.layout :as layout]
            [salava.core.i18n :refer [t translate-text]]
            [salava.core.common :refer [deep-merge]]
            [salava.user.schemas :as schemas]
            [salava.core.helper :refer [dump]]
            [salava.core.countries :refer [all-countries-sorted]]
            [salava.user.ui.input :as input]))

(defn clear-password-fields [state]
  (do
    (swap! state assoc-in [:current_password] "")
    (swap! state assoc-in [:new_password] "")
    (swap! state assoc-in [:new_password_verify] "")))

(defn save-user-info [state]
  (let [params @state
        current-password (if-not (blank? (:current_password params)) (:current_password params))
        new-password (if-not (blank? (:new_password params)) (:new_password params))
        new-password-verify (if-not (blank? (:new_password_verify params)) (:new_password_verify params))]
    (ajax/POST
      (path-for "/obpv1/user/edit/password")
      {:params {:current_password current-password
                :new_password new-password
                :new_password_verify new-password-verify}

       :handler (fn [data]
                  (if (= (:status data) "success")
                    (do
                      (clear-password-fields state)
                      (swap! state assoc :message {:class "alert-success" :content (:message data)}))
                    (do
                      (swap! state assoc :message {:class "alert-danger" :content (:message data)}))))})))



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
  (let [current-password-atom (cursor state [:current_password])
        new-password-atom (cursor state [:new_password])
        new-password-verify-atom (cursor state [:new_password_verify])
        message (:message @state)
        current-password? (get-in @state [:password?])]
    [:div {:class "panel" :id "edit-user"}
     [m/modal-window]
     (if message
       [:div {:class (str "alert " (:class message))}
        (translate-text (:content message))])
     [:div {:class "panel-body"}
      [:form.form-horizontal

       (if current-password?
         [:div.form-group
          [:label {:for "input-current-password" :class "col-md-3"} (t :user/Currentpassword)]
          [:div {:class "col-md-9"}
           [:input {:class       "form-control"
                    :id          "input-current-password"
                    :name        "current_password"
                    :type        "password"
                    :placeholder (t :user/Enteryourcurrentpassword)
                    :read-only   true
                    :on-focus    #(.removeAttribute (.-target %) "readonly")
                    :on-change   #(reset! current-password-atom (.-target.value %))
                    :value       @current-password-atom}]]])
       [:div {:id "new-password" :class "form-group"}
        [:div.col-md-12 (t :user/Tochangecurrentpassword)]
        [:br]
        [:div.flip
         [:label {:for "input-new_password" :class "col-md-3"} (t :user/Newpassword)]
         [:div {:class "col-md-9"}
          [input/text-field {:name "new_password" :atom new-password-atom :password? true}]]]]

       [:div.form-group
        [:label {:for "input-new_password_verify" :class "col-md-3"} (t :user/Confirmnewpassword)]
        [:div {:class "col-md-9"}
         [input/text-field {:name "new_password_verify" :atom new-password-verify-atom :password? true}]]]

       [:div.row
        [:div.col-xs-12
         [:button {:class "btn btn-primary"
                   :disabled (if-not (new-password-valid? current-password? @current-password-atom @new-password-atom @new-password-verify-atom)
                               "disabled")
                   :on-click #(do
                               (.preventDefault %)
                               (save-user-info state))}
          (t :core/Save)]]]]]]))

(def initial-state
  {:current_password    nil
   :new_password        nil
   :new_password_verify nil
   :message             nil})


(defn init-data [state]
  (ajax/GET
    (path-for "/obpv1/user/edit/password" true)
    {:handler (fn [data]
                (swap! state assoc :password? (:password? data)))}))

(defn handler [site-navi]
  (let [state (atom {:new_password_verify nil
                     :current_password    nil
                     :password?           true
                     :new_password        nil
                     :message             nil})]
    (init-data state)
    (fn []
      (layout/default site-navi (content state)))))
