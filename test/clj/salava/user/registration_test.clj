(ns salava.user.registration-test
  (:require [salava.user.db :as db]
            [clojure.test :refer :all]
            [salava.core.migrator :as migrator]
            [salava.core.test-utils :as t]))

(def test-user {:id 1 :role "user" :private false})

(def registration-data
  {:first_name "Testing"
   :last_name "Registration"
   :email "test.registration@example.com"
   :country "US"
   :password "123456"
   :language "fi"
   :password-verify "123456"})

(t/deftest-ctx main-test [ctx]
  
 #_ (testing "register user"
    (let [connect (db/register-user ctx (:email registration-data) (:first_name registration-data) (:last_name registration-data) (:country registration-data) (:language registration-data) (:password registration-data) (:password-verify registration-data))]
      (is (=  "success" (:status connect)))
     (is (=  "" (:message connect)))
     ))
 #_ (testing "register user again with same data"
    (let [connect (db/register-user ctx (:email registration-data) (:first_name registration-data) (:last_name registration-data) (:country registration-data) (:language registration-data) (:password registration-data) (:password-verify registration-data))]
      (is (=  "error" (:status connect)))
     (is (=  "user/Enteredaddressisalready" (:message connect)))
     ))
  (testing "get current state of configs"
    (let [{:keys [status body]} (t/test-api-request ctx :post "/app/obpv1/user/register" {:params registration-data})]
        (is (= 200 status))
        (is (= "accepted" body))))
  )
;(migrator/run-test-reset)
(migrator/reset-seeds (migrator/test-config))
