(ns salava.badge.routes-test
  (:require [clojure.test :refer :all]
            [midje.sweet :refer :all]
            [salava.test-utils :as ts]))

(def system (ts/get-system))

(def ctx (get-in system [:handler :ctx]))


(def test-user 1)
(def user-with-no-badges 3)
(def badge-id 1)


(deftest routes
  
  (testing "GET /export:"
    
    (testing "user must be logged in to export badges"
      (let [response (ts/test-api-request system :get "/app/obpv1/badge/export")]
        (is (= 401 (:status response)))))

    (testing "user has a badge which can be exported"
      (let [login-user (ts/login-with-user system test-user)
            {:keys [status body]} (ts/test-api-request system :get "/app/obpv1/badge/export" {} (:cookie login-user))]
        (is (= 200 status))
        (is (= [] (:badges body))))))
  
  (testing "GET /badge"
    
    (testing "user must be logged in"
      (let [response (ts/test-api-request system :get "/app/obpv1/badge")]
        (is (= 401 (:status response)))))

    (testing "user has two badges"
      (let [login-user (ts/login-with-user system test-user)
            {:keys [status body]} (ts/test-api-request system :get "/app/obpv1/badge" {} (:cookie login-user))]
        (is (= 200 status))
        (is (= 2 (count body)))))
    
    (testing "badge can be accepted"
      (let [login-user (ts/login-with-user system test-user)
            {:keys [status body]} (ts/test-api-request system :post  (str "/app/obpv1/badge/set_status/" badge-id) {:status "accepted"} (:cookie login-user))]
        (is (= 200 status))
        (is (= 1 body))))))

(ts/stop-system system)
