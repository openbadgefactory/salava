(ns salava.user.routes
  (:require [compojure.api.sweet :refer :all]
            [ring.util.http-response :refer :all]
            [ring.util.response :refer [redirect]]
            [ring.util.io :as io]
            [salava.core.layout :as layout]
            [schema.core :as s]
            [salava.core.util :refer [get-base-path]]
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
             (layout/main-meta ctx "/profile/:id" :user)
             (layout/main-meta ctx "/profile/:id/embed" :user)
             (layout/main ctx "/edit/profile")
             (layout/main ctx "/cancel")
             (layout/main ctx "/remote/facebook")
             (layout/main ctx "/remote/linkedin")
             (layout/main-meta ctx "/data/:id" :user)
             (layout/main ctx "/delete-user")
             (layout/main-meta ctx "/terms/:id" :user)

             (GET "/verify_email/:verification_key" []
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
                   ;:return ""
                   :body [login-content schemas/LoginUser]
                   :summary "User logs in"
                   (let [{:keys [email password]} login-content
                         accepted-terms? (u/accepted-terms? ctx email)
                         login-status (-> (u/login-user ctx email password)
                                          (assoc :terms (:status accepted-terms?)))]
                     (if (= "success" (:status login-status))
                       (u/finalize-login ctx (ok login-status) (:id login-status) (get-in req [:session :pending :user-badge-id]))
                       (ok login-status))))

             (POST "/logout" []
                   (assoc-in (ok) [:session :identity] nil))

             (GET "/register" []
                  :summary "Get languages"
                  (if (private? ctx)
                    (forbidden)
                    (ok {:languages (get-in ctx [:config :core :languages])})))

             (POST "/register" req
                   ;:return
                   #_{:status  (s/maybe  (s/enum "success" "error"))
                      :message (s/maybe s/Str)
                      :id      (s/maybe s/Int)
                      :role    (s/maybe s/Str)
                      :private (s/maybe s/Bool)}
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
                         ;(ok save)
                         (let [login-status (u/login-user ctx email password)]
                           (if (and (= "success" (:status login-status)) (= "success" (:status update-accept-term)) (= "accepted" (:input update-accept-term)))
                             (u/finalize-login ctx (ok login-status) (:id login-status) (get-in req [:session :pending :user-badge-id]))
                             (ok login-status)))
                         (cond
                           (not (right-token? ctx (:token form-content)))        (forbidden)
                           (not (in-email-whitelist? ctx (:email form-content))) (ok {:status "error" :message "user/Invalidemail"})
                           :else                                                 (let [ login-status  (u/login-user ctx email password)]
                                                                                   (if (and (= "success" (:status login-status)) (= "success" (:status update-accept-term)) (= "accepted" (:input update-accept-term)))
                                                                                     (u/set-session ctx (ok login-status) (:id login-status))
                                                                                     (ok login-status))))))))

             (POST "/activate" []
                   :return {:status (s/enum "success" "error")
                            :message (s/maybe s/Str)}
                   :body [activation-data schemas/ActivateUser]
                   :summary "Set password and activate user account"
                   (let [{:keys [user_id code password password_verify]} activation-data]
                     (ok (u/set-password-and-activate ctx user_id code password password_verify))))

             (GET "/edit" []
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
                  :summary "Get user information for editing"
                  :auth-rules access/signed
                  :current-user current-user
                  (ok {:password? (u/has-password? ctx (:id current-user))})
                  )

             (POST "/edit/password" []
                   :return {:status (s/enum "success" "error")
                            :message (s/maybe s/Str)}
                   :body [user-data schemas/EditUserPassword]
                   :summary "Get user information for editing"
                   :auth-rules access/signed
                   :current-user current-user
                   (ok (u/edit-user-password ctx user-data (:id current-user)))
                   )

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

             (GET "/profile/:userid" []
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
                   :body-params [user_id :- s/Int
                                 accept_terms :- (s/enum "declined" "accepted")]
                   :auth-rules access/signed
                   (ok (u/insert-user-terms ctx user_id accept_terms)))

             (GET "/data/:userid" []
                  :summary "Get everything on user"
                  :path-params [userid :- s/Int]
                  :current-user current-user
                  (ok (md/all-user-data ctx userid (:id current-user))))

             (GET "/export-to-pdf/:userid" []
                  :summary "export user data to pdf"
                  :path-params [userid :- s/Int]
                  :current-user current-user
                  (-> (io/piped-input-stream (md/export-data-to-pdf ctx userid (:id current-user)))
                      ok
                      (header  "Content-Disposition" (str "attachment; filename=\" Copy-of-Mydata.pdf\""))
                      (header "Content-Type" "application/pdf")
                      ))

             (GET "/edit/profile" []
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

             (POST "/profile" []
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
                   :body-params [password :- (:password schemas/User)]
                   :summary "Delete user account"
                   :auth-rules access/signed
                   :current-user current-user
                   (let [result (u/delete-user ctx (:id current-user) password)]
                     (if (= "success" (:status result))
                       (assoc-in (ok result) [:session :identity] nil)
                       (ok result))))

             (GET "/test" []
                  :summary "Test is user authenticated"
                  :auth-rules access/signed
                  (ok))

             (GET "/public-access" []
                  :summary "Test is user authenticated and in private mode"
                  :auth-rules access/signed
                  :current-user current-user
                  (if (:private current-user)
                    (forbidden)
                    (ok {:status "success"}))
                  ))))
