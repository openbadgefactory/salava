(ns salava.extra.socialuser.db-test
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

(t/deftest-ctx main-test [ctx]
  (testing "get user connections"
    (let [connections (db/get-user-accepted-connections-user ctx (:id test-user))]
      (is (=  2 (count connections)))
      ;order by first_name
      (is (= "Another"  (:first_name  (first connections))))))

  (testing "insert new user connect without user_properties"
    (testing " create connect"
      (let [response (db/create-connections-user! ctx (:id test-user) (:id user-with-default-accepting))]
        (is (= "success" (:status response)))))
    
    (testing " check status is pending"
      (let [response (db/get-connections-user ctx (:id test-user) (:id user-with-default-accepting))]
        (is (= "pending" (:status response)))))
    
    (testing "try to create same connect again"
      (let [response (db/create-connections-user! ctx (:id test-user) (:id  user-with-default-accepting))]
        (is (= "error" (:status response)))
        (is (= "Duplicate entry '1-4' for key 'PRIMARY'" (:message response)))))
    
    (testing "delete connections"
      (let [response (db/delete-connections-user ctx (:id test-user) (:id  user-with-default-accepting))]
        (is (=  "success" (:status response))))))

  
  (testing "insert new user connections with user_properties accepted"
    (testing " create connections"
      (let [response (db/create-connections-user! ctx (:id test-user) (:id user-with-accept-accepting))]
        (is (= "success" (:status response)))))
    
    (testing " check status is accepted"
      (let [response (db/get-connections-user ctx (:id test-user) (:id user-with-accept-accepting))]
        (is (= "accepted" (:status response)))))
    
    (testing "delete connections"
      (let [response (db/delete-connections-user ctx (:id test-user) (:id  user-with-accept-accepting))]
        (is (=  "success" (:status response))))))

  
  (testing "insert new user connections with user_properties pending"
    (testing " create connections"
      (let [response (db/create-connections-user! ctx (:id test-user) (:id user-with-pending-accepting))]
        (is (= "success" (:status response)))))
    
    (testing " check status is pending"
      (let [response (db/get-connections-user ctx (:id test-user) (:id user-with-pending-accepting))]
        (is (= "pending" (:status response)))))
    
    (testing "delete connections"
      (let [response (db/delete-connections-user ctx (:id test-user) (:id  user-with-pending-accepting))]
        (is (=  "success" (:status response))))))

  
  (testing "insert new user connections with user_properties decline"
    (testing " create connections"
      (let [response (db/create-connections-user! ctx (:id test-user) (:id user-with-declined-accepting))]
        (is (= "success" (:status response)))))

    (testing " check status is decline"
      (let [response (db/get-connections-user ctx (:id test-user) (:id user-with-declined-accepting))]
        (is (= "declined" (:status response)))))

    (testing "delete connections"
      (let [response (db/delete-connections-user ctx (:id test-user) (:id  user-with-declined-accepting))]
        (is (=  "success" (:status response))))))
  
  (testing " Set user_properties"
    
    (testing "set from default to accepted"
      (let [response (db/set-user-connections-accepting ctx (:id test-user) "accepted")
            connect-state (db/get-user-connections-accepting ctx (:id test-user))]
        (is (= "success" (:status response)))
        (is (= "accepted" connect-state))))

    (testing "set from accepted to pending"
      (let [response (db/set-user-connections-accepting ctx (:id test-user) "pending")
            connect-state (db/get-user-connections-accepting ctx (:id test-user))]
        (is (= "success" (:status response)))
        (is (= "pending" connect-state))))

    (testing "set from pending to declined"
      (testing "set from accepted to pending"
      (let [response (db/set-user-connections-accepting ctx (:id test-user) "declined")
            connect-state (db/get-user-connections-accepting ctx (:id test-user))]
        (is (= "success" (:status response)))
        (is (= "declined" connect-state))))))

  
  (testing "Set user_properties to accept and connections to user"
    (testing "set user-with-default-accepting to accepted"
      (let [response (db/set-user-connections-accepting ctx (:id user-with-pending-accepting) "accepted")
            connect-state (db/get-user-connections-accepting ctx (:id user-with-pending-accepting))]
        (is (= "success" (:status response)))
        (is (= "accepted" connect-state))))
    
    (testing " create connections"
      (let [response (db/create-connections-user! ctx (:id test-user) (:id user-with-pending-accepting))]
        (is (= "success" (:status response)))))
    
    (testing " check status is accepted"
      (let [response (db/get-connections-user ctx (:id test-user) (:id user-with-pending-accepting))]
        (is (= "accepted" (:status response)))))
    
    (testing "delete connections"
      (let [response (db/delete-connections-user ctx (:id test-user) (:id  user-with-pending-accepting))]
        (is (=  "success" (:status response)))))
    (testing "set user-with-default-accepting to "
      (let [response (db/set-user-connections-accepting ctx (:id user-with-pending-accepting) "pending")
            connect-state (db/get-user-connections-accepting ctx (:id user-with-pending-accepting))]
        (is (= "success" (:status response)))
        (is (= "pending" connect-state)))))


  
  (testing "Get all pending user connects"
    (let [connects (db/get-pending-requests ctx (:id user-with-pending-request))]
      (is (=  1 (count connects)))
      ;order by first_name
      (is (= "Test"  (:first_name  (first connects)))))
    

    (testing "accept pending user"
      (testing "get first user in pending list and accept request"
        (let [pending-user (first (db/get-pending-requests ctx (:id user-with-pending-request)))]
          (is (=  1  (:owner_id pending-user)))
          (db/set-pending-request ctx (:owner_id pending-user) (:id user-with-pending-request) "accepted"))
        
        (let [pending-users (db/get-pending-requests ctx (:id user-with-pending-request))
              connections   (db/get-user-accepted-connections-user ctx (:id test-user))]
          (is (=  0 (count pending-users)))
          (is (=  2 (count connections)))))
      (testing "try to create connect again with already accepted connect it should fail"
        (let [response (db/create-connections-user! ctx (:id test-user) (:id user-with-pending-request))]
          (is (= "error" (:status response)))))
      (testing "set back connection to pending status"
        (db/set-pending-request ctx (:id test-user) (:id user-with-pending-request) "pending")))

    (testing "decline pending user"
      (testing "get first user in pending list and decline request"
        (let [pending-user (first (db/get-pending-requests ctx (:id user-with-pending-request)))]
          (is (=  1  (:owner_id pending-user)))


          (db/set-pending-request ctx (:owner_id pending-user) (:id user-with-pending-request) "declined"))

        (let [pending-users (db/get-pending-requests ctx (:id user-with-pending-request))
              connections   (db/get-user-accepted-connections-user ctx (:id test-user))]
          (is (=  0 (count pending-users)))
          (is (=  1 (count connections)))))

      (testing "try to create connection again and it success "
        (let [response (db/create-connections-user! ctx (:id test-user) (:id user-with-pending-request))]
          (is (= "success" (:status response))))))))
;(migrator/run-test-reset)
;(migrator/reset-seeds (migrator/test-config))


