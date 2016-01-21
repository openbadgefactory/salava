(ns salava.user.routes
  (:require [compojure.api.sweet :refer :all]
            [ring.util.http-response :refer :all]
            [ring.util.response :refer [redirect]]
            [salava.core.layout :as layout]
            [schema.core :as s]
            [salava.user.schemas :as schemas]
            [salava.user.db :as u]))

(defroutes* route-def
  (context* "/user" []
            (layout/main "/login")
            (layout/main "/login/:next-url")
            (layout/main "/register")
            (layout/main "/account")
            (layout/main "/activate/:userid/:timestamp/:code"))

  (context* "/obpv1/user" []
    (POST* "/login" []
           ;:return ""
           :body [login-content schemas/LoginUser]
           :summary "User logs in"
           :components [context]
           (let [{:keys [email password]} login-content
                 login-status (u/login-user context email password)]
             (if (= "success" (:status login-status))
               (assoc-in (ok login-status) [:session :identity] (select-keys login-status [:user-id :fullname]))
               (ok login-status))))

    (POST* "/logout" []
           (assoc-in (ok) [:session :identity] nil))

    (POST* "/register" []
           :return {:status (s/enum "success" "error")
                    :message (s/maybe s/Str)}
           :body [form-content schemas/RegisterUser]
           :summary "Create new user account"
           :components [context]
           (let [{:keys [email first_name last_name country]} form-content]
             (ok (u/register-user context email first_name last_name country))))

    (POST* "/activate" []
           :return {:status (s/enum "success" "error")
                    :message (s/maybe s/Str)}
           :body [activation-data schemas/ActivateUser]
           :summary "Set password and activate user account"
           :components [context]
           (let [{:keys [user_id code password password_verify]} activation-data]
             (ok (u/set-password-and-activate context user_id code password password_verify))))))
