(ns salava.extra.socialuser.routes-test
  (:require [salava.extra.socialuser.db :as db]
            [clojure.test :refer :all]
            [salava.core.migrator :as migrator]
            [salava.core.test-utils :as t]))

(def test-user {:id 1 :role "user" :private false})
(def user-with-pending-request {:id 3 :role "user" :private false})
(def user-with-default-accepting {:id 4 :role "user" :private false})
(def user-with-accept-accepting {:id 5 :role "user" :private false})
(def user-with-pending-accepting {:id 10 :role "user" :private false})
(def user-with-declined-accepting {:id 11 :role "user" :private false})


(t/deftest-ctx routes-test [ctx]
  (testing "GET /accepted-connections"
    (testing "try to get without login "
      (let [response (t/test-api-request ctx :get "/app/obpv1/socialuser/accepted-connections")]
        (is (= 401 (:status response)))))
    
    (testing "with login"
      (let [{:keys [status body]} (t/test-api-request ctx :get "/app/obpv1/socialuser/accepted-connections" {:user test-user})]
        (is (= 200 status))
        (is (= 2 (count body))))))

  (testing "Create and delete user connection "
    (testing "Create"
      (let [{:keys [status body]} (t/test-api-request ctx :post (str "/app/obpv1/socialuser/user-connection/" (:id user-with-accept-accepting)) {:user test-user})]
        (is (= 200 status))
        (is (= "success" (:status body)))))

    (testing "Check if status is accepted"
      (let [{:keys [status body]} (t/test-api-request ctx :get (str "/app/obpv1/socialuser/user-connection/" (:id user-with-accept-accepting)) {:user test-user})]
        (is (= 200 status))
        (is (= "accepted" (:status body)))))

    (testing "Delete"
      (let [{:keys [status body]} (t/test-api-request ctx :delete (str "/app/obpv1/socialuser/user-connection/" (:id user-with-accept-accepting)) {:user test-user})]
        (is (= 200 status))
        (is (= "success" (:status body)))))
    
    (testing "Check if status is nil"
      (let [{:keys [status body]} (t/test-api-request ctx :get (str "/app/obpv1/socialuser/user-connection/" (:id user-with-accept-accepting)) {:user test-user})]
        (is (= 200 status))
        (is (= nil (:status body))))))

  (testing "User connection configs"
    (testing "get current state of configs"
      (let [{:keys [status body]} (t/test-api-request ctx :get "/app/obpv1/socialuser/user-connection-config" {:user test-user})]
        (is (= 200 status))
        (is (= "pending" body))))
    
    (testing "change configs to accepted"
      (let [{:keys [status body]} (t/test-api-request ctx :post (str "/app/obpv1/socialuser/user-connection-config/" "accepted") {:user test-user})]
        (is (= 200 status))
        (is (= "success" (:status body)))))

    (testing "get current state of configs"
      (let [{:keys [status body]} (t/test-api-request ctx :get "/app/obpv1/socialuser/user-connection-config" {:user test-user})]
        (is (= 200 status))
        (is (= "accepted" body))))
    
    (testing "change configs to accepted"
      (let [{:keys [status body]} (t/test-api-request ctx :post (str "/app/obpv1/socialuser/user-connection-config/" "pending") {:user test-user})]
        (is (= 200 status))
        (is (= "success" (:status body))))))

(testing "get pending requests"
      (let [{:keys [status body]} (t/test-api-request ctx :get "/app/obpv1/socialuser/user-pending-requests" {:user user-with-pending-request})
            pending-user (first body)]
        (is (= 200 status))
        (is (= 1 (count body)))
        
        (testing "change request to accepted"
          (let [{:keys [status body]} (t/test-api-request ctx :post (str "/app/obpv1/socialuser/user-pending-requests/" (:owner_id pending-user) "/" "accepted")
                                                          {:user user-with-pending-request})]
            (is (= 200 status))
            (is (= "success" (:status body)))))
        
        (testing "get pending requests"
          (let [{:keys [status body]} (t/test-api-request ctx :get "/app/obpv1/socialuser/user-pending-requests"
                                                          {:user user-with-pending-request})
                pending-user (first body)]
            (is (= 200 status))
            (is (= 0 (count body)))))

        (testing "change back to pending"
          (db/set-pending-request ctx (:owner_id pending-user) (:id user-with-pending-request) "pending")))))

  
;(migrator/reset-seeds (migrator/test-config))

