(ns salava.user.activation-test
  (:require [midje.sweet :refer :all]
            [salava.core.migrator :as migrator]
            [salava.test-utils :refer [test-api-request login! logout! test-user-credentials]]))

(def activation-data
  {:password "testtest"
   :password_verify "testtest"
   :code "a0372e4f-c634-4ccb-afe3-1622ae551bcf"
   :user_id 6})

(def user-id-to-activate 6)

(facts "login with unactivated user accouct should fail"
       (let [{:keys [status body]} (apply login! (test-user-credentials user-id-to-activate))]
         status => 200
         (:status body) => "error"))

(facts "about activating user account"
       (fact "password must be valid"
             (:status (test-api-request :post "/user/activate" (dissoc activation-data :password))) => 400
             (:status (test-api-request :post "/user/activate" (assoc activation-data :password nil))) => 400
             (:status (test-api-request :post "/user/activate" (assoc activation-data :password ""))) =>
             (:status (test-api-request :post "/user/activate" (assoc activation-data :password "short"))) => 400
             (:status (test-api-request :post "/user/activate" (assoc activation-data :password (apply str (repeat 51 "a"))))) => 400)

       (fact "password verification must be valid"
             (:status (test-api-request :post "/user/activate" (dissoc activation-data :password_verify))) => 400
             (:status (test-api-request :post "/user/activate" (assoc activation-data :password_verify nil))) => 400
             (:status (test-api-request :post "/user/activate" (assoc activation-data :password_verify ""))) =>
             (:status (test-api-request :post "/user/activate" (assoc activation-data :password_verify "short"))) => 400
             (:status (test-api-request :post "/user/activate" (assoc activation-data :password_verify (apply str (repeat 51 "a"))))) => 400)

       (fact "password and password verification must match"
             (let [{:keys [status body]} (test-api-request :post "/user/activate" (assoc activation-data :password_verify "not-matching"))]
               status => 200
               (:status body) => "error"))

       (fact "verification key must be valid"
             (:status (test-api-request :post "/user/activate" (dissoc activation-data :code))) => 400
             (:status (test-api-request :post "/user/activate" (assoc activation-data :code nil))) => 400)

       (fact "user-id must be integer"
             (:status (test-api-request :post "/user/activate" (dissoc activation-data :user_id))) => 400
             (:status (test-api-request :post "/user/activate" (assoc activation-data :user_id nil))) => 400
             (:status (test-api-request :post "/user/activate" (assoc activation-data :user_id ""))) => 400)

       (fact "user-id and verication key must be correct"
             (let [{:keys [status body]} (test-api-request :post "/user/activate" (assoc activation-data :user_id 99))]
               status => 200
               (:status body) => "error")
             (let [{:keys [status body]} (test-api-request :post "/user/activate" (assoc activation-data :code "not-valid"))]
               status => 200
               (:status body) => "error"))

       (fact "user can activate account with valid activation data"
             (let [{:keys [status body]} (test-api-request :post "/user/activate" activation-data)]
               status => 200
               (:status body) => "success"))

       (fact "user can not activate account again"
             (let [{:keys [status body]} (test-api-request :post "/user/activate" activation-data)]
               status => 200
               (:status body) => "error")))

(facts "login with recently activated user accouct should succeed"
       (let [{:keys [status body]} (apply login! (test-user-credentials user-id-to-activate))]
         status => 200
         (:status body) => "success")
       (logout!))

(migrator/reset-seeds (migrator/test-config))
