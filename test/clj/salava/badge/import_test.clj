(ns salava.badge.import-test
  (:require [midje.sweet :refer :all]
            [salava.test-utils :refer [test-api-request]]))

(fact "User cannot import badges because there are no valid email address"
      (let [{:keys [status body]} (test-api-request :get "/badge/import/1")]
        status => 200
        (:status body) => "error"
        (:badges body) => []
        (:error body) => "User does not have any email addresses"))