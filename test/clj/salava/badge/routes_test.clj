(ns salava.badge.routes-test
  (:require [clojure.test :refer :all]
            [salava.core.test-utils :as t]
            [salava.core.access :as access]))

(def test-user {:id 1 :role "user" :activated true :private false})
(def user-with-no-badges (assoc test-user :id 3))
(def badge-id 1)

(t/deftest-ctx routes-test [ctx]
  (testing "GET /export:"
    (testing "user must be logged in to export badges"
      (let [response (t/test-api-request ctx :get "/obpv1/badge/export")]
        (is (= 401 (:status response)))))

    (testing "user has a badge which can be exported"
      (let [{:keys [status body]} (t/test-api-request ctx :get "/obpv1/badge/export" {:user test-user})]
        (is (= 200 status))
         (is (= 1 (count (:badges body)))))))
;;         (is (= 1 body)))))

  (testing "GET /badge"
    (testing "user must be logged in"
      (let [response (t/test-api-request ctx :get "/obpv1/badge")]
        (is (= 401 (:status response)))))

    (testing "user has two badges"
      (let [{:keys [status body]} (t/test-api-request ctx :get "/obpv1/badge/" {:user test-user})]
        (is (= 200 status))
        (is (= 2 (count body)))))

    (testing "user has no badges"
      (let [{:keys [status body]} (t/test-api-request ctx :get "/obpv1/badge/" {:user user-with-no-badges})]
        (is (= 200 status))
        (is (= 0 (count body)))))

    (testing "badge can be accepted"
      (let [{:keys [status body]} (t/test-api-request ctx
                                                      :post
                                                      (str "/obpv1/badge/set_status/" badge-id)
                                                      {:user test-user
                                                       :params {:status "accepted"}})]
        (is (= 200 status))
        (is (= "1" body))))))
