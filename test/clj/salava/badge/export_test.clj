(ns salava.badge.export-test
  (:require [midje.sweet :refer :all]
            [salava.test-utils :refer [test-api-request login! logout! test-user-credentials]]))

(def test-user 1)

(def user-with-no-badges 3)

(facts "about exporting badges"

       (fact "user must be logged in to export badges"
             (:status (test-api-request :get "/badge/export")) => 401)

       (apply login! (test-user-credentials test-user))

       (fact "user has a badge which can be exported"
             (let [{:keys [status body]} (test-api-request :get "/badge/export")]
               status => 200
               (count body) => 1))

       (logout!)


       (apply login! (test-user-credentials user-with-no-badges))

       (fact "user does not have a badge which can be exported"
             (let [{:keys [status body]} (test-api-request :get "/badge/export")]
               status => 200
               body => []))

       (logout!))