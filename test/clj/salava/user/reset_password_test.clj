(ns salava.user.reset-password-test
  (:require [midje.sweet :refer :all]
            [salava.core.migrator :as migrator]
            [salava.test-utils :refer [test-api-request login! logout! test-user-credentials]]))

(def expired-activation-data
  {:password "newpassword"
   :password_verify "newpassword"
   :code "8cd3a052-5575-42a3-ad6b-f4b57ee9ebb4"
   :user_id 2})

(def valid-activation-data
  {:password "testtest"
   :password_verify "testtest"
   :code "94c36436-d2ce-4337-a05a-64a6b849f144"
   :user_id 3})

(facts "about requesting new password"
       (fact "email address must be valid"
             (:status (test-api-request :post "/user/reset" {})) => 400
             (:status (test-api-request :post "/user/reset" {:email nil})) => 400
             (:status (test-api-request :post "/user/reset" {:email ""})) => 400
             (:status (test-api-request :post "/user/reset" {:email "not-valid-email-address"})) => 400
             (:status (test-api-request :post "/user/reset" {:email "not-valid-email-address@either"})) => 400
             (:status (test-api-request :post "/user/reset" {:email (str (apply str (repeat 252 "a")) "@a.a")})) => 400)

       (fact "user must exist"
             (let [{:keys [status body]} (test-api-request :post "/user/reset" {:email "not.exists@example.com"})]
               status => 200
               (:status body) => "error"))

       (fact "email address must be user's primary email address"
             (let [{:keys [status body]} (test-api-request :post "/user/reset" {:email "secondary.email@example.com"})]
               status => 200
               (:status body) => "error"))

       (fact "email is sent to user, if address is correct"
             (let [{:keys [status body]} (test-api-request :post "/user/reset" {:email "test.user@example.com"})]
               status => 200
               (:status body) => "success")))

(facts "about resetting the password"
       (fact "user can not reset password if code is expired"
             (let [{:keys [status body]} (test-api-request :post "/user/activate" expired-activation-data)]
               status => 200
               (:status body) => "error"))
       (fact "user can reset password"
             (let [{:keys [status body]} (test-api-request :post "/user/activate" valid-activation-data)]
               status => 200
               (:status body) => "success")))

(migrator/reset-seeds (migrator/test-config))