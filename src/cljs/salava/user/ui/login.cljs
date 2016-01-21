(ns salava.user.ui.login
  (:require [reagent.core :refer [atom cursor]]
            [ajax.core :as ajax]
            [clojure.walk :as walk :refer [keywordize-keys]]
            [salava.core.ui.helper :refer [navigate-to]]
            [salava.core.ui.layout :as layout]
            [salava.core.i18n :refer [t]]))

(defn login [state]
  (let [{:keys [email password]} @state]
    (ajax/POST
      (str "/obpv1/user/login")
      {:params  {:email    email
                 :password password}
       :handler (fn [data]
                  (let [data-with-kws (keywordize-keys data)]
                    (if (= (:status data-with-kws) "success")
                      (navigate-to "/badge/mybadges"))

                    ;(if (= (:status data-with-kws) "error")
                    ;(swap! state assoc :error-message (:message data-with-kws))
                    ;(swap! state assoc :registration-sent true))

                    ))})))

(defn content [state]
  (let [email-atom (cursor state [:email])
        password-atom (cursor state [:password])]
    [:div {:id "login-panel"
           :class "panel"}
     [:div {:class "panel-body form"}
      [:div.form-group
       [:input {:class "form-control"
                :type "text"
                :name "email"
                :placeholder (t :user/Email)
                :on-change #(reset! email-atom (.-target.value %))
                :value @email-atom}]]
      [:div.form-group
       [:input {:class "form-control"
                :type "password"
                :name "password"
                :placeholder (t :user/Password)
                :on-change #(reset! password-atom (.-target.value %))
                :value @password-atom}]]
      [:button {:class "btn btn-warning"
                :on-click #(login state)}
       (t :user/Login)]
      [:div {:class "row login-links"}
       [:div {:class "col-xs-6 text-right"}
        [:a {:href "/user/register"} (t :user/Createnewaccount)]]
       [:div {:class "col-xs-6 text-left"}
        [:a {:href "/user/password"} (t :user/Requestnewpassword)]]]]]))

(defn handler []
  (let [state (atom {:email ""
                     :password ""})]
    (fn []
      (layout/landing-page (content state)))))
