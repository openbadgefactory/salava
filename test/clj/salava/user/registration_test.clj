(ns salava.user.registration-test
  (:require [midje.sweet :refer :all]
            [salava.core.migrator :as migrator]
            [salava.test-utils :refer [test-api-request]]))

(def registration-data
  {:first_name "Testing"
   :last_name "Registration"
   :email "test.registration@example.com"
   :country "US"})

(facts "about user account registration"
       (fact "email address must be valid"
             (:status (test-api-request :post "/user/register" (dissoc registration-data :email))) => 400
             (:status (test-api-request :post "/user/register" (assoc registration-data :email nil))) => 400
             (:status (test-api-request :post "/user/register" (assoc registration-data :email ""))) => 400
             (:status (test-api-request :post "/user/register" (assoc registration-data :email "not-valid-email-address"))) => 400
             (:status (test-api-request :post "/user/register" (assoc registration-data :email "not-valid-email-address@either"))) => 400
             (:status (test-api-request :post "/user/register" (assoc registration-data :email (str (apply str (repeat 252 "a")) "@a.a")))) => 400)

       (fact "first name must be valid"
             (:status (test-api-request :post "/user/register" (dissoc registration-data :first_name))) => 400
             (:status (test-api-request :post "/user/register" (assoc registration-data :first_name nil))) => 400
             (:status (test-api-request :post "/user/register" (assoc registration-data :first_name ""))) => 400
             (:status (test-api-request :post "/user/register" (assoc registration-data :first_name (apply str (repeat 256 "a"))))) => 400)

       (fact "last name must be valid"
             (:status (test-api-request :post "/user/register" (dissoc registration-data :last_name))) => 400
             (:status (test-api-request :post "/user/register" (assoc registration-data :last_name nil))) => 400
             (:status (test-api-request :post "/user/register" (assoc registration-data :last_name ""))) => 400
             (:status (test-api-request :post "/user/register" (assoc registration-data :last_name (apply str (repeat 256 "a"))))) => 400)

       (fact "country must be valid"
             (:status (test-api-request :post "/user/register" (dissoc registration-data :country))) => 400
             (:status (test-api-request :post "/user/register" (assoc registration-data :country nil))) => 400
             (:status (test-api-request :post "/user/register" (assoc registration-data :country ""))) => 400
             (:status (test-api-request :post "/user/register" (assoc registration-data :country "XX"))) => 400)

       (fact "user can create an account"
             (let [{:keys [status body]} (test-api-request :post "/user/register" registration-data)]
               status => 200
               (:status body) => "success"))

       (fact "user can not create account, if email address is already taken"
             (let [{:keys [status body]} (test-api-request :post "/user/register" registration-data)]
               status => 200
               (:status body) => "error")))

(migrator/reset-seeds (migrator/test-config))