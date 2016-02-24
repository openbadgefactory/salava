(ns salava.user.login-test
  (:require [midje.sweet :refer :all]
            [salava.test-utils :refer [test-api-request login! logout! test-user-credentials]]))

(def test-user 1)
(def not-activated-user 7)
(def not-verified-user 8)

(defn login-data [user-id]
  (let [[email password] (test-user-credentials user-id)]
    {:email email :password password}))

(facts "about logging in"
       (fact "email address must be valid"
             (:status (test-api-request :post "/user/login" (dissoc (login-data test-user) :email))) => 400
             (:status (test-api-request :post "/user/login" (assoc (login-data test-user) :email nil))) => 400
             (:status (test-api-request :post "/user/login" (assoc (login-data test-user) :email ""))) => 400
             (:status (test-api-request :post "/user/login" (assoc (login-data test-user) :email "not-valid-email-address"))) => 400
             (:status (test-api-request :post "/user/login" (assoc (login-data test-user) :email "not-valid-email-address@either"))) => 400
             (:status (test-api-request :post "/user/login" (assoc (login-data test-user) :email (str (apply str (repeat 252 "a")) "@a.a")))) => 400)

       (fact "password must be valid"
             (:status (test-api-request :post "/user/login" (dissoc (login-data test-user) :password))) => 400
             (:status (test-api-request :post "/user/login" (assoc (login-data test-user) :password nil))) => 400
             (:status (test-api-request :post "/user/login" (assoc (login-data test-user) :password ""))) =>
             (:status (test-api-request :post "/user/login" (assoc (login-data test-user) :password "short"))) => 400
             (:status (test-api-request :post "/user/login" (assoc (login-data test-user) :password (apply str (repeat 51 "a"))))) => 400)

       (fact "user must be existing user"
             (let [{:keys [status body]} (test-api-request :post "/user/login" (assoc (login-data test-user) :email "totally.fake@email.com"))]
               status => 200
               (:status body) => "error"))

       (fact "password must be correct"
             (let [{:keys [status body]} (test-api-request :post "/user/login" (assoc (login-data test-user) :password "not-correct"))]
               status => 200
               (:status body) => "error"))

       (fact "user account must be activated"
             (let [{:keys [status body]} (test-api-request :post "/user/login" (login-data not-activated-user))]
               status => 200
               (:status body) => "error"))

       (fact "email address must be verified"
             (let [{:keys [status body]} (test-api-request :post "/user/login" (login-data not-verified-user))]
               status => 200
               (:status body) => "error"))

       (fact "email address must be a primary address"
             (let [{:keys [status body]} (test-api-request :post "/user/login" (assoc (login-data test-user) :email "secondary.email@example.com"))]
               status => 200
               (:status body) => "error"))

       (fact "user can login with valid credentials"
             (let [{:keys [status body cookies]} (apply login! (test-user-credentials test-user))]
               status = 200
               (:status body) => "success"
               (boolean cookies) => true))

       (fact "user can access resource which requires authentication"
             (:status (test-api-request :get "/user/test")) => 200)

       (fact "user can logout"
             (:status (logout!)) => 200)

       (fact "user can not access resource which requires authentication"
             (:status (test-api-request :get "/user/test")) => 401))