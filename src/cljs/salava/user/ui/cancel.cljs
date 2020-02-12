(ns salava.user.ui.cancel
  (:require [reagent.core :refer [atom cursor]]
            [reagent.session :as session]
            [salava.core.ui.ajax-utils :as ajax]
            [salava.core.ui.helper :refer [input-valid? js-navigate-to path-for]]
            [salava.core.ui.layout :as layout]
            [salava.core.i18n :refer [t translate-text]]
            [salava.user.schemas :as schemas]
            [reagent-modals.modals :as m]))

(defn password-valid? [password]
  (input-valid? (:password schemas/User) password))

(defn cancel-account [state]
  (let [lng (session/get-in [:user :language] "en")]
    (ajax/POST
      (path-for "/obpv1/user/delete")
      {:params  {:password (:password @state)}
       :handler (fn [data]
                  (if (= (:status data) "error")
                    (swap! state assoc :error-message (t :user/Erroroccuredduringaccountcancellation))
                    (js-navigate-to (str "/user/delete-user/" lng))))})))

(defn cancel-account-modal [state]
  [:div
   [:div.modal-body
    [:div.row
     [:div.col-md-12
      [:button {:type  "button"
                :class  "close"
                :data-dismiss "modal"
                :aria-label   "OK"}

       [:span {:aria-hidden  "true"
               :dangerouslySetInnerHTML {:__html "&times;"}}]]]]
    [:div
     [:p (t :user/Confirmaccountremoval1)]
     [:p [:b (t :user/Confirmaccountremoval2)]]
     [:form {:class "form-horizontal"}
      [:div.form-group
       [:fieldset {:id "export-pdf" :class "col-md-12 checkbox"}
        [:div.col-md-12 [:label
                         [:input {:type     "checkbox"
                                  :on-change (fn [e]
                                               (if (.. e -target -checked)
                                                 (swap! state assoc :confirm-delete "true")(swap! state assoc :confirm-delete "false")))}]

                         (t :user/Confirmaccountremoval3)]]]]]]]
   [:div.modal-footer
    [:button {:type         "button"
              :class        "btn btn-primary"
              :style {:margin-bottom "unset"}
              :data-dismiss "modal"}
     (t :core/Cancel)]
    [:button {:type         "button"
              :class        "btn btn-warning"
              :style {:margin-bottom "unset"}
              :disabled (if-not (= "true" (:confirm-delete @state))
                          "disabled")
              :on-click   #(do
                             (if (= "true" (:confirm-delete @state))
                               (cancel-account state)))
              :data-dismiss "modal"}

     (t :user/Cancelaccount)]]])


(defn cancel-form [state]
  (let [password-atom (cursor state [:password])]
    [:div {:class "form-horizontal"}
     (if (:error-message @state)
       [:div {:class "alert alert-warning" :role "alert"}
        (translate-text (:error-message @state))])
     [:div.form-group
      [:label {:class "col-xs-3"
               :for "input-password"}
       (t :user/Password)
       [:span.form-required " *"]]
      [:div.col-xs-9
       [:input {:class       "form-control"
                :id          "input-password"
                :type        "password"
                :name        "password"
                :read-only   true
                :on-change   #(reset! password-atom (.-target.value %))
                :on-focus    #(.removeAttribute (.-target %) "readonly")
                :placeholder (t :user/Tocancelaccountenterpassword)
                :value       @password-atom}]]]
     [:button {:class    "btn btn-warning"
               :disabled (if-not (password-valid? @password-atom)
                           "disabled")
               :on-click #(m/modal![cancel-account-modal state] {:size :lg})
               #_(cancel-account state)}
      (t :user/Cancelaccount)]]))

#_(defn content [state]
   [:div {:id "cancel-account"}
    [:h1.uppercase-header (t :user/Cancelaccount)]
    (if (:has-password? @state)
      [:div
       [m/modal-window]
       [:div {:id "cancel-info"}
        [:p (t :user/Cancelaccountinstructions1)]
        [:p (t :user/Cancelaccountinstructions2) ":"]
        [:p (t :user/Cancelaccountinstructions3) ":"]
        [:ul
         [:li (t :user/Goto) " " [:a {:href (path-for "/badge/export")} (t :user/Badgeexport)] " " (t :user/page)]
         [:li {:dangerouslySetInnerHTML
               {:__html (t :user/Cancelaccountinstructions4)}}]]
        [:p (t :user/Cancelaccountinstructions5) ":"]
        [:ul
         [:li (t :user/Goto) " " [:a {:href (path-for "/badge/export")} (t :user/Badgeexport)] " " (t :user/page)]
         [:li {:dangerouslySetInnerHTML
               {:__html (t :user/Cancelaccountinstructions6)}}]
         [:li {:dangerouslySetInnerHTML
               {:__html (t :user/Cancelaccountinstructions7)}}]
         [:li  (t :user/Cancelaccountinstructions8)]]
        [:p [:b (t :user/Cancelaccountinstructions9)]]]
       [:div {:class "panel"}
        [:div.panel-body
         (cancel-form state)]]]
      [:div {:class "panel"}
       [:span (t :oauth/Cannotunlink) " " (t :oauth/Setpasswordfrom)
                 " " [:a {:href (path-for "/user/edit/password")} (t :user/Passwordsettings)] "."]])])

(defn content [state]
  [:div {:id "cancel-account"}
   [:h1.uppercase-header (t :user/Cancelaccount)]
   (if (:has-password? @state)
     [:div
      [m/modal-window]
      [:div {:id "cancel-info"}
       [:p (t :user/Cancelaccountinstructions1)]
       [:ul
         [:li (t :user/Removeaccountinstruction)]
         [:li (t :user/Removeaccountinstruction2)]]
       [:p [:b (t :user/Cancelaccountinstructions9)]]]
      [:div {:class "panel"}
       [:div.panel-body
        (cancel-form state)]]]
     [:div {:class "panel"}
      [:span (t :oauth/Cannotunlink) " " (t :oauth/Setpasswordfrom)
                " " [:a {:href (path-for "/user/edit/password")} (t :user/Passwordsettings)] "."]])])

(defn init-data [state]
  (ajax/GET
    (path-for "/obpv1/user/edit" true)
    {:handler (fn [data]
                (swap! state assoc :has-password? (get-in data [:user :password?])))}))

(defn handler [site-navi]
  (let [state (atom {:password ""
                     :error-message nil
                     :has-password? nil
                     :confirm-delete "false"})]
    (init-data state)
    (fn []
      (layout/default site-navi (content state)))))
