(ns salava.user.routes
  (:require [compojure.api.sweet :refer :all]
            [ring.util.http-response :refer :all]
            [ring.util.response :refer [redirect]]
            [salava.core.layout :as layout]
            [schema.core :as s]
            [salava.user.schemas :as schemas]
            [salava.user.db :as u]
            [salava.core.access :as access]
            salava.core.restructure))

(defn route-def [ctx]
  (routes
    (context "/user" []
             (layout/main ctx "/login")
             (layout/main ctx "/register")
             (layout/main ctx "/reset")
             (layout/main ctx "/activate/:userid/:timestamp/:code")
             (layout/main ctx "/edit")
             (layout/main ctx "/edit/email-addresses")
             (layout/main ctx "/edit/fboauth")
             (layout/main ctx "/edit/linkedin")
             (layout/main ctx "/profile/:id")
             (layout/main ctx "/edit/profile")
             (layout/main ctx "/cancel")

             (GET "/verify_email/:verification_key" []
                  :path-params [verification_key :- s/Str]
                  :summary "Confirm user email address"
                  :auth-rules access/authenticated
                  :current-user current-user
                  (do
                    (u/verify-email-address ctx verification_key (:id current-user))
                    (found "/user/edit/email-addresses"))))

    (context "/obpv1/user" []
             :tags ["user"]
             (POST "/login" []
                   ;:return ""
                   :body [login-content schemas/LoginUser]
                   :summary "User logs in"
                   (let [{:keys [email password]} login-content
                         login-status (u/login-user ctx email password)]
                     (if (= "success" (:status login-status))
                       (assoc-in (ok login-status) [:session :identity :id] (:id login-status))
                       (ok login-status))))

             (POST "/logout" []
                   (assoc-in (ok) [:session :identity] nil))

             (POST "/register" []
                   :return {:status (s/enum "success" "error")
                            :message (s/maybe s/Str)}
                   :body [form-content schemas/RegisterUser]
                   :summary "Create new user account"
                   (let [{:keys [email first_name last_name country]} form-content]
                     (ok (u/register-user ctx email first_name last_name country))))

             (POST "/activate" []
                   :return {:status (s/enum "success" "error")
                            :message (s/maybe s/Str)}
                   :body [activation-data schemas/ActivateUser]
                   :summary "Set password and activate user account"
                   (let [{:keys [user_id code password password_verify]} activation-data]
                     (ok (u/set-password-and-activate ctx user_id code password password_verify))))

             (GET "/edit" []
                  :summary "Get user information for editing"
                  :auth-rules access/authenticated
                  :current-user current-user
                  (let [user-info (u/user-information ctx (:id current-user))]
                    (ok {:user (dissoc user-info :profile_picture :profile_visibility :about)
                         :languages (get-in ctx [:config :core :languages])})))

             (POST "/edit" []
                   :return {:status (s/enum "success" "error")
                            :message (s/maybe s/Str)}
                   :body [user-data schemas/EditUser]
                   :summary "Save user information"
                   :auth-rules access/authenticated
                   :current-user current-user
                   (ok (u/edit-user ctx user-data (:id current-user))))

             (GET "/email-addresses" []
                  :return [schemas/EmailAddress]
                  :summary "Get user email addresses"
                  :auth-rules access/authenticated
                  :current-user current-user
                  (ok (u/email-addresses ctx (:id current-user))))

             (POST "/add_email" []
                   :return {:status (s/enum "success" "error")
                            (s/optional-key :message) s/Str
                            (s/optional-key :new-email) schemas/EmailAddress}
                   :body-params [email :- (:email schemas/User)]
                   :summary "Add new unverified email address"
                   :auth-rules access/authenticated
                   :current-user current-user
                   (ok (u/add-email-address ctx email (:id current-user))))

             (POST "/delete_email" []
                   :return {:status (s/enum "success" "error")}
                   :body-params [email :- (:email schemas/User)]
                   :summary "Remove email address"
                   :auth-rules access/authenticated
                   :current-user current-user
                   (ok (u/delete-email-address ctx email (:id current-user))))

             (POST "/set_primary_email" []
                   :return {:status (s/enum "success" "error")}
                   :body-params [email :- (:email schemas/User)]
                   :summary "Set primary email address"
                   :auth-rules access/authenticated
                   :current-user current-user
                   (ok (u/set-primary-email-address ctx email (:id current-user))))

             (GET "/profile/:userid" []
                  ;:return ""
                  :summary "Get user information and profile fields"
                  :path-params [userid :- s/Int]
                  :auth-rules access/authenticated
                  :current-user current-user
                  (let [profile (u/user-information-and-profile ctx userid)]
                    (ok (assoc profile :owner? (= userid (:id current-user))))))

             (GET "/edit/profile" []
                  ;:return
                  :summary "Get user information and profile fields for editing"
                  :auth-rules access/authenticated
                  :current-user current-user
                  (ok (u/user-profile-for-edit ctx (:id current-user))))

             (POST "/profile/set_visibility" []
                   :return (:profile_visibility schemas/User)
                   :body-params [visibility :- (:profile_visibility schemas/User)]
                   :summary "Update profile visibility"
                   :auth-rules access/authenticated
                   :current-user current-user
                   (ok (u/set-profile-visibility ctx visibility (:id current-user))))

             (POST "/profile" []
                   ;:return
                   :body-params [profile_visibility :- (:profile_visibility schemas/User)
                                 profile_picture :- (:profile_picture schemas/User)
                                 about :- (:about schemas/User)]
                   :summary "Save user's profile fields, visibility, picture and about text"
                   :auth-rules access/authenticated
                   :current-user current-user
                   (ok (str (u/save-user-profile ctx profile_visibility profile_picture about (:id current-user)))))

             (POST "/reset" []
                   :return {:status (s/enum "success" "error")}
                   :body-params [email :- (:email schemas/User)]
                   :summary "Send password reset link to requested email address"
                   (ok (u/send-password-reset-link ctx email)))

             (GET "/test" []
                  :summary "Test is user authenticated"
                  :auth-rules access/authenticated
                  (ok)))))