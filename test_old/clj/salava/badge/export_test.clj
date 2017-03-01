(ns salava.badge.export-test
  (:require [clojure.test :refer :all]
            [midje.sweet :refer :all]
            [salava.test-utils :as ts ]))
;;[get-system stop-system test-api-request login! logout! test-user-credentials]
(def test-user 1)

(def user-with-no-badges 3)

(def system (ts/get-system))

(facts "about exporting badges"

       (fact "user must be logged in to export badges"
             (:status (ts/test-api-request system :get "/app/obpv1/badge/export")) => 401)

       (apply ts/login! (test-user-credentials test-user))

       (fact "user has a badge which can be exported"
             (let [{:keys [status body]} (test-api-request :get "/badge/export")]
               status => 200
               (count (:badges body)) => 0))

       (ts/logout!)

       (apply ts/login! (test-user-credentials user-with-no-badges))

       (fact "user does not have a badge which can be exported"
             (let [{:keys [status body]} (test-api-request :get "/badge/export")]
               status => 200
               (:badges body) => []))

       (ts/logout!))

(ts/stop-system system)
