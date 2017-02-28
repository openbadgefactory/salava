(ns salava.badge.my-test
  (:require [midje.sweet :refer :all]
            [salava.core.migrator :as migrator]
            [salava.test-utils :refer [test-api-request test-user-credentials login! logout!]]))

(def test-user 1)
(def user-with-no-badges 3)
(def badge-id 1)

(facts "about user's badges"

       (fact "user must be logged in"
             (:status (test-api-request :get (str "/badge"))) => 401)

       (apply login! (test-user-credentials test-user))

       (fact "user has two badges"
             (let [{:keys [status body]} (test-api-request :get (str "/badge"))]
               status => 200
               (count body) => 2))

       (fact "badge can be accepted"
             (let [{:keys [status body]} (test-api-request :post (str "/badge/set_status/" badge-id) {:status "accepted"})]
               status => 200
               body => 1)
             (let [{:keys [status body]} (test-api-request :get (str "/badge/info/" badge-id))]
               status => 200
               (:status body) => "accepted"))

       (logout!)


       (apply login! (test-user-credentials user-with-no-badges))

       (fact "another user has no badges"
             (let [{:keys [status body]} (test-api-request :get (str "/badge"))]
               status => 200
               (count body) => 0))
       (logout!))

(migrator/reset-seeds (migrator/test-config))