(ns salava.badge.export-test
  (:require [midje.sweet :refer :all]
            [salava.test-utils :refer [test-api-request]]))

(fact "User has a badge which can be exported"
      (let [{:keys [status body]} (test-api-request :get "/badge/export/1")]
        status => 200
        (count body) => 1))

(fact "User does not have a badge which can be exported"
      (let [{:keys [status body]} (test-api-request :get "/badge/export/99")]
        status => 200
        body  => []))