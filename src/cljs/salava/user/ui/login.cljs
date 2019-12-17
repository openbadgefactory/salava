(ns salava.user.ui.login
  (:require [reagent.core :refer [atom cursor]]
            [reagent.session :as session]
            [reagent-modals.modals :as m]
            [clojure.string :as string]
            [salava.core.helper :as h]
            [salava.core.ui.ajax-utils :as ajax]
            [salava.core.ui.input :as input]
            ;[salava.user.ui.input :as input]
            [salava.oauth.ui.helper :refer [facebook-link linkedin-link google-link]]
            [salava.core.ui.helper :refer [base-path js-navigate-to path-for private? plugin-fun input-valid? enable-background-image]]
            [salava.core.ui.layout :as layout]
            [salava.social.ui.helper :refer [social-plugin?]]
            [salava.core.helper :refer [dump]]
            [salava.user.schemas :as schemas]
            [salava.core.i18n :refer [t translate-text]]
            [salava.translator.ui.helper :refer [translate]]
            [dommy.core :as dommy :refer-macros [sel sel1]]))

(defn verification-token [url]
  (if-let [match (re-find #"verification_key=([\w-]+)" url)]
    (second match)))

(defn plugin-blocks [fn-name]
  (let [blocks (plugin-fun (session/get :plugins) "block" fn-name)]
    [:div (doall (map #(%) blocks))]))

(defn follow-up-url []
  (let [verification-key (verification-token js/window.location.search)
         manual-referrer (or (session/get :referrer)
                             (some->> js/document.cookie js/decodeURIComponent (re-find #"login_redirect=(/[^; ]+)") last))
         referrer js/document.referrer
         site-url (str (session/get :site-url) (base-path))
         path (if (and referrer site-url (string/starts-with? referrer site-url)) (string/replace referrer site-url ""))]

    (session/put! :referrer nil)
    (aset js/document "cookie" "login_redirect=")
    (cond
      (not (empty? verification-key))  (str "/user/verify_email/" verification-key)
      (and (not (empty? manual-referrer)) (string/starts-with? manual-referrer "/")) manual-referrer
      ;(and (not (empty? path)) (not= "/user/login" path) (not= path (path-for "/user/login"))) path
      :else "/social")))

(defn toggle-accept-terms [state]
  (let [user-id (:user-id @state)
        accept-terms (:accept-terms @state)]
    (ajax/POST
     (path-for (str "/obpv1/user/accept_terms"))
     {:params {:accept_terms accept-terms :user_id user-id}
      :handler (fn [data]
                 (when (and (= "success" (:status data)) (= "accepted" (:input data)))
                   (do
                     (session/put! :user-has-accepted-terms true)
                     (js-navigate-to (follow-up-url)))))})))

(defn terms-and-conditions-modal [state f name]
  (let [bname (keyword (str "user/" name))]

    [:div
     [:div
      [:div.modal-body
       [:div.row
        [:div.col-md-12
         [:button {:type  "button"
                   :class  "close pull-left"
                   :data-dismiss "modal"
                   :aria-label   "OK"}
          [:span {:aria-hidden  "true"
                  :dangerouslySetInnerHTML {:__html "&times;"}}]]]

        [:div {:style (if (or (string/blank? (layout/terms-and-conditions-fr)) (string/blank? (layout/terms-and-conditions))) {:display "none"})}
         [:div {:id "lang-buttons" :style {:text-align "center" :margin-top "50px"}}
          [:ul
           [:li [:a {:href "#" :on-click #(swap! state assoc :modal-content (layout/terms-and-conditions))} "EN"]]
           [:li [:a {:href "#" :on-click #(swap! state assoc :modal-content (layout/terms-and-conditions-fr))} "FR"]]]]]]
       [:div
        [:div
         (:modal-content @state)]
        [:fieldset {:class "col-md-12 checkbox"}
         [:div.col-md-12 {:style {:text-align "center"}} [:label
                                                          [:input {:type     "checkbox"
                                                                   :on-change (fn [e]
                                                                                (if (.. e -target -checked)
                                                                                  (swap! state assoc :accept-terms "accepted") (swap! state assoc :accept-terms "declined")))}]
                                                          (t :user/Doyouaccept)]]]]]
      [:div.modal-footer {:style {:text-align "center"}}
       [:button {:type         "button"
                 :class        "btn btn-primary"
                 :disabled     (if-not (= (:accept-terms @state) "accepted") "disabled")
                 :on-click #(f)}
        (t bname)]]]]))

(defn login [state]
  (let [{:keys [email password]} @state
        f (fn [] (toggle-accept-terms state))
        terms-enabled? (session/get :terms-enabled?)]
    (ajax/POST
      (path-for "/obpv1/user/login")
      {:params  {:email    email
                 :password password}
       :handler (fn [data]
                  (cond
                    (and (= (:status data) "success")(or (= (:terms data) "accepted") (= false (:terms data)))) (js-navigate-to (or (:redirect-to data) (follow-up-url)))
                    ;(and (= (:status data) "success") (= (:terms data) "accepted")) (js-navigate-to (follow-up-url))
                    (and (= (:status data) "success") (nil? (:terms data))) (do (swap! state assoc :user-id (:id data))  (m/modal![terms-and-conditions-modal state f "Login"] {:size :lg}))
                    (and (= (:status data) "success") (= (:terms data) "declined")) (do (swap! state assoc :user-id (:id data)) (m/modal![terms-and-conditions-modal state f "Login"] {:size :lg}))
                    :else (swap! state assoc :error-message (:message data))
                    ))})))

(defn content [state]
  (let [email-atom (cursor state [:email])
        password-atom (cursor state [:password])
        error-message-atom (cursor state [:error-message])
        f (fn [] (js-navigate-to "/user/register"))
        l (session/get-in [:user :language] "en")]
    [:div {:id "login-page"}
     (plugin-blocks "login_top")
     [m/modal-window]
     [:div {:id "narrow-panel"
            :class "panel"}
      [:div {:class "panel-body"}
       (if @error-message-atom
         [:div {:class "alert alert-warning"}
          (translate-text @error-message-atom)])
       [:form
        [:div.form-group
         [input/text-field {:name "email" :atom email-atom :error-message-atom error-message-atom :placeholder (translate l :user/Email) :aria-label (translate l :user/Email)}]]
        [:div.form-group
         [input/text-field {:name "password" :atom password-atom :error-message-atom error-message-atom :placeholder (translate l :user/Password) :aria-label (translate l :user/Password) :password? true}]]
        [:button {:class    "btn btn-primary login-button"
                  :on-click #(do (.preventDefault %)
                                 (if-not (input-valid? schemas/LoginUser {:email @email-atom :password @password-atom})
                                   (swap! state assoc :error-message "user/Loginfailed"))
                                 (login state))}
         (t :user/Login)]
        [:div {:class "row login-links"}
         [:div.management-links.col-md-12
          (if-not (private?)
            [:div {:class "col-sm-6 left-column"}
             [:a {:href "#" :on-click #(js-navigate-to "/user/register")} (t :user/Createnewaccount)]])
          [:div {:class (if (private?) "col-xs-12" "col-sm-6 right-column")}
           [:a {:href (path-for "/user/reset")} (t :user/Requestnewpassword)]]]
         (into [:div]
           (for [f (plugin-fun (session/get :plugins) "block" "oauth_login_form")]
             [f]))

         #_[:div {:class "row oauth-buttons"}
            [:div.col-md-12
             [:div {:class "col-sm-4 left-column"} (facebook-link false nil)]
             [:div.col-sm-4.right-column (linkedin-link nil nil)]
             [:div.col-sm-4 [google-link false nil]]]]]
        #_[:div {:class "row login-links"}
           [:div.management-links
            (if-not (private?)
              [:div {:class "col-xs-6"}
               [:a {:href (path-for "/user/register")} (t :user/Createnewaccount)]])
            [:div {:class (if (private?) "col-xs-12" "col-sm-6")}
             [:a {:href (path-for "/user/reset")} (t :user/Requestnewpassword)]]]
           [:div {:class "row oauth-buttons"}
            [:div {:class "col-xs-6"} (facebook-link false nil)]
            [:div.col-sm-6 (linkedin-link nil nil)]]]]]]
     (plugin-blocks "login_bottom")]))

(defn handler [site-navi params]
  (let [flash-message (t (keyword (session/get! :flash-message)))
        state (atom {:email         ""
                     :password      ""
                     :error-message (if (not-empty flash-message) flash-message)
                     :accept-terms "declined"
                     :modal-content (layout/terms-and-conditions)})
        lang (or (:lang params) (-> (or js/window.navigator.userLanguage js/window.navigator.language) (string/split #"-") first))]
    (when (and lang (some #(= lang %) (session/get :languages)))
      (session/assoc-in! [:user :language] lang)
      (-> (sel1 :html) (dommy/set-attr! :lang lang)))
    (fn []
      (enable-background-image)
      (layout/landing-page site-navi (content state)))))
