(ns salava.user.routes
  (:require [compojure.api.sweet :refer :all]
            [ring.util.http-response :refer :all]
            [ring.util.response :refer [redirect]]
            [salava.core.layout :as layout]
            [schema.core :as s]
            [salava.user.schemas :as schemas]
            [salava.user.db :as u]))

(defn route-def [ctx]
  (routes
    (context "/user" []
             (layout/main ctx "/login")
             (layout/main ctx "/login/:next-url")
             (layout/main ctx "/register")
             (layout/main ctx "/account")
             (layout/main ctx "/activate/:userid/:timestamp/:code"))

    (context "/obpv1/user" []
      (POST "/login" []
            ;:return ""
            :body [login-content schemas/LoginUser]
            :summary "User logs in"
            (let [{:keys [email password]} login-content
                 login-status (u/login-user ctx email password)]
              (if (= "success" (:status login-status))
                (assoc-in (ok login-status) [:session :identity] (select-keys login-status [:id :fullname]))
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
              (ok (u/set-password-and-activate ctx user_id code password password_verify)))))))