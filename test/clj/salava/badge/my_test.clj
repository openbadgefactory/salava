(ns salava.badge.my-test
  (:require [midje.sweet :refer :all]
            [salava.test-utils :refer [test-api-request]]))

(def user-id 1)

(def badge-id 1)

(fact "User has two badges"
      (let [{:keys [status body]} (test-api-request :get (str "/badge/" user-id))]
        status => 200
        (count body) => 2))

(fact "User has no badges"
      (let [{:keys [status body]} (test-api-request :get "/badge/99")]
        status => 200
        body => []))

(fact "Badge can be accepted"
      (let [{:keys [status body]} (test-api-request :post (str "/badge/set_status/" badge-id) {:status "accepted"})]
        status => 200
        body => 1)
      (let [{:keys [status body]} (test-api-request :get (str "/badge/info/" badge-id))]
        status => 200
        (:status body) => "accepted"))
