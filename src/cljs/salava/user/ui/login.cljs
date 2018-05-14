(ns salava.user.ui.login
  (:require [reagent.core :refer [atom cursor]]
            [reagent.session :as session]
            [reagent-modals.modals :as m]
            [clojure.string :as string]
            [salava.core.helper :as h]
            [salava.core.ui.ajax-utils :as ajax]
            [salava.user.ui.input :as input]
            [salava.oauth.ui.helper :refer [facebook-link linkedin-link]]
            [salava.core.ui.helper :refer [base-path js-navigate-to path-for private? plugin-fun input-valid?]]
            [salava.core.ui.layout :as layout]
            [salava.social.ui.helper :refer [social-plugin?]]
            [salava.core.helper :refer [dump]]

            [salava.user.schemas :as schemas]
            [salava.core.i18n :refer [t translate-text]]))

(defn verification-token [url]
  (if-let [match (re-find #"verification_key=([\w-]+)" url) ]
    (second match)))

(defn follow-up-url []

  (let [
         verification-key (verification-token js/window.location.search)
         manual-referrer (session/get :referrer)
         referrer js/document.referrer
         site-url (str (session/get :site-url) (base-path))
         path (if (and referrer site-url (string/starts-with? referrer site-url)) (string/replace referrer site-url "") )]

    (session/put! :referrer nil)
    (cond
      (not (empty? verification-key))  (str "/user/verify_email/" verification-key)
      (and (not (empty? manual-referrer)) (string/starts-with? manual-referrer "/")) manual-referrer
      (and (not (empty? path)) (not= "/user/login" path) (not= path (path-for "/user/login"))) path
      :else "/social/stream")))

(defn toggle-accept-terms [state]
  (let [ user-id (:user-id @state)
         accept-terms (:accept-terms @state)]
    (dump @state)
    (ajax/POST
      (path-for (str "/obpv1/user/accept_terms"))
      {:params {:accept_terms accept-terms :user_id user-id}
       :handler (fn [data]
                  (when (and (= "success" (:status data)) (= "accepted" (:input data)))
                    (js-navigate-to (follow-up-url))))})))

(defn terms-and-conditions-modal [state f name]
  (let [bname (keyword (str "user/" name))]
  [:div {:style {:text-align "center"}}
   [:div.modal-body
    [:div.row
     [:div.col-md-12
      [:button {:type  "button"
                :class  "close pull-left"
                :data-dismiss "modal"
                :aria-label   "OK"
                }
       [:span {:aria-hidden  "true"
               :dangerouslySetInnerHTML {:__html "&times;"}}]]]]
    [:div
     [:form {:class "form-horizontal"}
      [:div.form-group
       [:div.col-md-12
        (t :user/Termsandconditions)]
       [:fieldset {:class "col-md-12 checkbox"}
        [:div.col-md-12 [:label
                         [:input {:type     "checkbox"
                                  :on-change (fn [e]
                                               (if (.. e -target -checked)
                                                 (swap! state assoc :accept-terms "accepted")(swap! state assoc :accept-terms "declined")
                                                 ))}]
                         (t :user/Doyouaccept)]]]]]]]
   [:div.modal-footer {:style {:text-align "center"}}
    [:button {:type         "button"
              :class        "btn btn-primary"
              :disabled     (if-not (= (:accept-terms @state) "accepted") "disabled")
              :on-click #(f)}
     (t bname)]]]))

(defn login [state]
  (let [{:keys [email password]} @state
        f (fn [] (toggle-accept-terms state))]
    (ajax/POST
      (path-for "/obpv1/user/login")
      {:params  {:email    email
                 :password password}
       :handler (fn [data]
                  (cond
                    (and (= (:status data) "success") (= (:terms data) "accepted")) (js-navigate-to (follow-up-url))
                    (and (= (:status data) "success") (nil? (:terms data))) (do (swap! state assoc :user-id (:id data)) (m/modal![terms-and-conditions-modal state f "Login"] {:size :lg}))
                    (and (= (:status data) "success") (= (:terms data) "declined")) (do (swap! state assoc :user-id (:id data)) (m/modal![terms-and-conditions-modal state f "Login"] {:size :lg}))
                    :else (swap! state assoc :error-message (:message data))
                    ))})))

(defn content [state]
  (let [email-atom (cursor state [:email])
        password-atom (cursor state [:password])
        error-message-atom (cursor state [:error-message])]
    [:div {:id "login-page"}
     [m/modal-window]
     [:div {:id "narrow-panel"
            :class "panel"}
      [:div {:class "panel-body"}
       (if @error-message-atom
         [:div {:class "alert alert-warning"}
          (translate-text @error-message-atom)])
       ;(if login-info (login-info))
       [:form
        [:div.form-group {:aria-label "email"}
         [input/text-field {:name "email" :atom email-atom :error-message-atom error-message-atom :placeholder (t :user/Email) :aria-label (t :user/Email)}]]
        [:div.form-group
         [input/text-field {:name "password" :atom password-atom :error-message-atom error-message-atom :placeholder (t :user/Password) :aria-label (t :user/Password) :password? true}]]
        [:button {:class    "btn btn-primary login-button"
                  :on-click #(do (.preventDefault %)
                               (if-not (input-valid? schemas/LoginUser {:email @email-atom :password @password-atom})
                                 (swap! state assoc :error-message "user/Loginfailed"))
                               (login state))}
         (t :user/Login)]
        [:div {:class "row login-links"}
         [:div.management-links
          (if-not (private?)
            [:div {:class "col-sm-6 left-column"}
             [:a {:href (path-for "/user/register")} (t :user/Createnewaccount)]])
          [:div {:class (if (private?) "col-xs-12" "col-sm-6 right-column")}
           [:a {:href (path-for "/user/reset")} (t :user/Requestnewpassword)]]]
         [:div {:class "row oauth-buttons"}
          [:div {:class "col-sm-6 left-column"} (facebook-link false nil)]
          [:div.col-sm-6.right-column (linkedin-link nil nil)]]]
        #_[:div {:class "row login-links"}
           [:div.management-links
            (if-not (private?)
              [:div {:class "col-xs-6"}
               [:a {:href (path-for "/user/register")} (t :user/Createnewaccount)]])
            [:div {:class (if (private?) "col-xs-12" "col-sm-6")}
             [:a {:href (path-for "/user/reset")} (t :user/Requestnewpassword)]]]
           [:div {:class "row oauth-buttons"}
            [:div {:class "col-xs-6"} (facebook-link false nil)]
            [:div.col-sm-6 (linkedin-link nil nil)]]]]]]]))

(defn handler [site-navi params]
  (let [flash-message (t (keyword (session/get! :flash-message)))
        state (atom {:email         ""
                     :password      ""
                     :error-message (if (not-empty flash-message) flash-message)
                     :accept-terms "declined"})
        lang (:lang params)]
    (if (and lang (some #(= lang %) (session/get :languages)))
      (session/assoc-in! [:user :language] lang))
    (fn []

      (layout/landing-page site-navi (content state)))))
