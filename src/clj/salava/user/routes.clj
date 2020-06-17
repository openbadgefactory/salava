(ns salava.user.routes
  (:require [compojure.api.sweet :refer :all]
            [ring.util.http-response :refer :all]
            [ring.util.response :refer [redirect]]
            [ring.util.io :as io]
            [salava.core.layout :as layout]
            [schema.core :as s]
            [salava.core.util :refer [get-base-path get-plugins]]
            [salava.user.schemas :as schemas]
            [salava.user.db :as u]
            [salava.mail.email-notifications :as en]
            [salava.registerlink.db :refer [right-token?  in-email-whitelist?]]
            [salava.core.helper :refer [dump private?]]
            [salava.core.access :as access]
            [salava.user.data :as md]
            salava.core.restructure))

(defn route-def [ctx]
  (routes
   (context "/user" []
            :tags ["user"]

            (layout/main ctx "/login")
            (layout/main ctx "/login/:lang")
            (layout/main ctx "/register")
            (layout/main ctx "/register/:lang")
            (layout/main ctx "/reset")
            (layout/main ctx "/reset/:lang")
            (layout/main ctx "/activate/:userid/:timestamp/:code")
            (layout/main ctx "/activate/:userid/:timestamp/:code/:lang")
            (layout/main ctx "/edit")
            (layout/main ctx "/edit/password")
            (layout/main ctx "/edit/email-addresses")
            (layout/main ctx "/edit/fboauth")
            (layout/main ctx "/edit/linkedin")
            (layout/main-meta ctx "/profile/:id/embed" :user)
            (layout/main ctx "/edit/profile")
            (layout/main ctx "/cancel")
            (layout/main ctx "/remote/facebook")
            (layout/main ctx "/remote/linkedin")
            (layout/main-meta ctx "/data/:id" :user)
            (layout/main ctx "/delete-user/:lang")
            (layout/main ctx "/terms")
            (layout/main ctx "/registration-complete")
            (layout/main ctx "/external/data/:id")

            (GET "/verify_email/:verification_key" []
                 :no-doc true
                 :path-params [verification_key :- s/Str]
                 :summary "Confirm user email address"
                 :current-user current-user
                 (if current-user
                   (do
                     (u/verify-email-address ctx verification_key (:id current-user) (:activated current-user))
                     (u/set-session ctx (found (str (get-base-path ctx) "/user/edit/email-addresses"))  (:id current-user)))
                   (found (str (get-base-path ctx) "/user/login/?verification_key=" verification_key)))))

   (context "/obpv1/user" []
            :tags ["user"]
            (POST "/login" req
                  :body [login-content schemas/LoginUser]
                  :summary "User logs in"
                  (let [{:keys [email password]} login-content
                        accepted-terms? (u/accepted-terms? ctx email)
                        login-status (-> (u/login-user ctx email password)
                                         (assoc :terms accepted-terms?)
                                         (assoc :redirect-to (get-in req [:cookies "login_redirect" :value]))
                                         (assoc :invitation (get-in req [:session :invitation] nil)))]
                    (if (= "success" (:status login-status))
                      (u/finalize-login ctx (ok login-status) (:id login-status) (get-in req [:session :pending :user-badge-id]) false)
                      (ok login-status))))

            (POST "/logout" []
                  :summary "End user session"
                  (-> (ok)
                      (assoc-in [:session :identity] nil)
                      (assoc-in [:cookies "login_redirect"] {:value nil :max-age 600 :http-only true :path "/"})))

            (GET "/register" req
                 :no-doc true
                 :summary "Get config data for register form"
                 (if (private? ctx)
                   (forbidden)
                   (-> (ok {:languages (get-in ctx [:config :core :languages])})
                       (assoc :session (assoc (get req :session {}) :seen-terms true)))))

            (POST "/register" req
                  :return {:status (s/enum "error" "success")
                           (s/optional-key :message) (s/maybe s/Str)
                           (s/optional-key :id) s/Int}
                  :body [form-content schemas/RegisterUser]
                  :summary "Create new user account"
                  (let [{:keys [email first_name last_name country language password password_verify accept_terms]} form-content
                        save (u/register-user ctx email first_name last_name country language password password_verify)
                        user-id (u/get-user-by-email ctx email)
                        update-accept-term (u/insert-user-terms ctx (:id user-id) accept_terms)]

                    (if (= "error" (:status save))
                       ;return error status from save
                      (ok save)
                      (if (not (private? ctx))
                        (let [login-status (assoc (u/login-user ctx email password) :invitation (get-in req [:session :invitation] nil))]
                          (if (and (= "success" (:status login-status)) (= "success" (:status update-accept-term)) (or (= "accepted" (:input update-accept-term)) (= "disabled" (:input update-accept-term))))
                            (u/finalize-login ctx (assoc-in (ok login-status) [:session :new-user] (:id user-id)) (:id login-status) (get-in req [:session :pending :user-badge-id]) true)
                            (ok login-status)))
                        (cond
                          (not (right-token? ctx (:token form-content)))        (forbidden)
                          (not (in-email-whitelist? ctx (:email form-content))) (ok {:status "error" :message "user/Invalidemail"})
                          :else                                                 (let [login-status  (u/login-user ctx email password)]
                                                                                  (if (and (= "success" (:status login-status)) (= "success" (:status update-accept-term)) (or (= "accepted" (:input update-accept-term)) (= "disabled" (:input update-accept-term))))
                                                                                    (u/set-session ctx (assoc-in (ok login-status) [:session :new-user] (:id user-id)) (:id login-status))
                                                                                    (ok login-status))))))))
            (GET "/register/complete" req
                 :no-doc true
                 :return {:status (s/enum "success" "error")}
                 :current-user current-user
                 :auth-rules access/signed
                 :summary "Check for successful registration"
                 (let [check (= (get-in req [:session :new-user]) (:id current-user))]
                   (ok {:status (if check "success" "error")})))

            (POST "/activate" []
                  :return {:status (s/enum "success" "error")
                           :message (s/maybe s/Str)}
                  :body [activation-data schemas/ActivateUser]
                  :summary "Set password and activate user account"
                  (let [{:keys [user_id code password password_verify]} activation-data]
                    (ok (u/set-password-and-activate ctx user_id code password password_verify))))

            (GET "/edit" []
                 :no-doc true
                 :summary "Get user information for editing"
                 :auth-rules access/signed
                 :current-user current-user
                 (let [user-info (u/user-information ctx (:id current-user))]
                   (ok {:user      (-> user-info
                                       (dissoc :profile_picture :profile_visibility :about)
                                       (assoc :password? (u/has-password? ctx (:id current-user))))
                        :languages (get-in ctx [:config :core :languages])
                        :email-notifications (get-in ctx [:config :user :email-notifications] false)})))

            (GET "/edit/password" []
                 :no-doc true
                 :summary "Get user information for editing"
                 :auth-rules access/signed
                 :current-user current-user
                 (ok {:password? (u/has-password? ctx (:id current-user))}))

            (POST "/edit/password" []
                  :return {:status (s/enum "success" "error")
                           :message (s/maybe s/Str)}
                  :body [user-data schemas/EditUserPassword]
                  :summary "Get user information for editing"
                  :auth-rules access/signed
                  :current-user current-user
                  (ok (u/edit-user-password ctx user-data (:id current-user))))

            (POST "/edit" []
                  :return {:status (s/enum "success" "error")
                           :message (s/maybe s/Str)}
                  :body [user-data schemas/EditUser]
                  :summary "Save user information"
                  :auth-rules access/signed
                  :current-user current-user
                  (ok (u/edit-user ctx user-data (:id current-user))))

            (GET "/email-addresses" []
                 :return [schemas/EmailAddress]
                 :summary "Get user email addresses"
                 :auth-rules access/signed
                 :current-user current-user
                 (ok (u/email-addresses ctx (:id current-user))))

            (POST "/add_email" []
                  :return {:status (s/enum "success" "error")
                           (s/optional-key :message) s/Str
                           (s/optional-key :new-email) schemas/EmailAddress}
                  :body-params [email :- (:email schemas/User)]
                  :summary "Add new unverified email address"
                  :auth-rules access/signed
                  :current-user current-user
                  (ok (u/add-email-address ctx email (:id current-user))))

            (POST "/delete_email" []
                  :return {:status (s/enum "success" "error")}
                  :body-params [email :- (:email schemas/User)]
                  :summary "Remove email address"
                  :auth-rules access/signed
                  :current-user current-user
                  (ok (u/delete-email-address ctx email (:id current-user))))

            (POST "/send_verified_link" []
                  :return {:status (s/enum "success" "error")
                           (s/optional-key :message) s/Str
                           (s/optional-key :new-email) schemas/EmailAddress}
                  :body-params [email :- (:email schemas/User)]
                  :summary ""
                  :auth-rules access/signed
                  :current-user current-user
                  (ok (u/send-email-verified-link ctx email (:id current-user))))

            (POST "/set_primary_email" []
                  :return {:status (s/enum "success" "error")}
                  :body-params [email :- (:email schemas/User)]
                  :summary "Set primary email address"
                  :auth-rules access/signed
                  :current-user current-user
                  (ok (u/set-primary-email-address ctx email (:id current-user))))

            #_(GET "/profile/:userid" []
                    ;:return ""
                   :summary "Get user information and profile fields"
                   :path-params [userid :- s/Int]
                   :current-user current-user
                   (let [profile (u/user-information-and-profile ctx userid (:id current-user))
                         visibility (get-in profile [:user :profile_visibility])]
                     (if (or (= visibility "public")
                             (and (= visibility "internal") current-user))
                       (ok profile)
                       (unauthorized))))

            (POST "/accept_terms" []
                  :return {:status (s/enum "success" "error") :input s/Str}
                  :summary "save info that user has accepted GDPR terms and conditions"
                  :auth-rules access/signed
                  :current-user current-user
                  (ok (u/insert-user-terms ctx (:id current-user) "accepted")))

            (GET "/data/:userid" []
                 :no-doc true
                 :summary "Get everything on user"
                 :path-params [userid :- s/Int]
                 :auth-rules access/authenticated
                 :current-user current-user
                 (ok (md/all-user-data ctx userid (:id current-user) "true")))

            (GET "/export-to-pdf/:userid" []
                 :no-doc true
                 :summary "export user data to pdf"
                 :path-params [userid :- s/Int]
                 :current-user current-user
                 (-> (io/piped-input-stream (md/export-data-to-pdf ctx userid (:id current-user)))
                     ok
                     (header  "Content-Disposition" (str "attachment; filename=\" Copy-of-Mydata.pdf\""))
                     (header "Content-Type" "application/pdf")))

            #_(GET "/edit/profile" []
                    ;:return
                   :summary "Get user information and profile fields for editing"
                   :auth-rules access/signed
                   :current-user current-user
                   (ok (u/user-profile-for-edit ctx (:id current-user))))

            (POST "/profile/set_visibility" []
                  :return (:profile_visibility schemas/User)
                  :body-params [visibility :- (:profile_visibility schemas/User)]
                  :summary "Update profile visibility"
                  :auth-rules access/signed
                  :current-user current-user
                  (if (:private current-user)
                    (forbidden)
                    (ok (u/set-profile-visibility ctx visibility (:id current-user)))))

            #_(POST "/profile" []
                     ;:return
                    :body-params [profile_visibility :- (:profile_visibility schemas/User)
                                  profile_picture :- (:profile_picture schemas/User)
                                  about :- (:about schemas/User)
                                  fields :- [{:field (apply s/enum (map :type schemas/contact-fields)) :value (s/maybe s/Str)}]]
                    :summary "Save user's profile fields, visibility, picture and about text"
                    :auth-rules access/signed
                    :current-user current-user
                    (ok (str (u/save-user-profile ctx profile_visibility profile_picture about fields (:id current-user)))))

            (POST "/reset" []
                  :return {:status (s/enum "success" "error")}
                  :body-params [email :- (:email schemas/User)]
                  :summary "Send password reset link to requested email address"
                  (ok (u/send-password-reset-link ctx email)))

            (POST "/delete" []
                  :return {:status (s/enum "success" "error")}
                  :body-params [password :- (:password schemas/User)]
                  :summary "Delete user account"
                  :auth-rules access/signed
                  :current-user current-user
                  (let [result (u/delete-user ctx (:id current-user) password)]
                    (if (= "success" (:status result))
                      (assoc-in (ok result) [:session :identity] nil)
                      (ok result))))

            (GET "/test" []
                 :no-doc true
                 :summary "Test is user authenticated"
                 :auth-rules access/signed
                 (ok))

            (GET "/public-access" []
                 :summary "Test is user authenticated and in private mode"
                 :auth-rules access/signed
                 :current-user current-user
                 (if (:private current-user)
                   (forbidden)
                   (ok {:status "success"})))

            (GET "/dashboard" []
                 :no-doc true
                 :summary "Get dashboard information"
                 :auth-rules access/signed
                 :current-user current-user
                 (ok (u/dashboard-info ctx (:id current-user))))

            (GET "/external/data/:id" []
                 :no-doc true
                 :path-params [id :- s/Str]
                 :summary "Get external user's data"
                 (ok (u/external-user-info ctx id)))

            (DELETE "/external/:id" []
                 :no-doc true
                 :return (s/enum "success" "error")
                 :path-params [id :- s/Str]
                 :summary "Delete external user data"
                 (ok (u/delete-external-user! ctx id)))

            (GET "/external/data/export/:lng/:id" []
                 :no-doc true
                 :summary "Export external user data"
                 :path-params [id :- s/Str
                               lng :- s/Str]
                 (-> (io/piped-input-stream (u/export-external-user-data ctx id lng))
                     ok
                     (header "Content-Disposition" (str "attachment; filename=\"mydata.csv\""))
                     (header "Content-Type" "text/csv"))))))
