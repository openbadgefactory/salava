(ns salava.badge.settings-test
  (:require [midje.sweet :refer :all]
            [salava.core.migrator :as migrator]
            [salava.test-utils :refer [test-api-request login! logout! test-user-credentials]]))

(def test-user 1)

(def badge-settings
  {:tags ["Sample tag" "Second tag"]
   :rating 5
   :evidence-url "http://example.com/evidence"
   :visibility "internal"})

(def badge-id 2)

(def badge-not-owned-by-user 3)

(facts "about editing badge settings"
       (fact "user must be logged in to access and edit badge settings"
             (:status (test-api-request :get (str "/badge/settings/" badge-id))) => 401
             (:status (test-api-request :post (str "/badge/save_settings/" badge-id) (assoc badge-settings :rating 0))) => 401)

       (apply login! (test-user-credentials test-user))

       (fact "badge settings can be accessed"
             (let [{:keys [status body]} (test-api-request :get (str "/badge/settings/" badge-id))]
               status => 200
               (:rating body) => nil
               (:evidence-url body) => nil
               (:visibility body) => "private"))

       (fact "user does not have an access to other users badge settings"
             (let [{:keys [status body]} (test-api-request :get (str "/badge/settings/" badge-not-owned-by-user))]
               status => 200
               body => nil))

       (fact "when editing badge settings: tags, rating, evidence URL and visibility are required"
             (let [{:keys [status body]} (test-api-request :post (str "/badge/save_settings/" badge-id) {})]
               status => 400
               body => "{\"errors\":{\"visibility\":\"missing-required-key\",\"evidence-url\":\"missing-required-key\",\"rating\":\"missing-required-key\",\"tags\":\"missing-required-key\"}}"))

       (fact "badge rating must be between 0-50"
             (let [{:keys [status body]} (test-api-request :post (str "/badge/save_settings/" badge-id) (assoc badge-settings :rating 0))]
               status => 400
               body => "{\"errors\":{\"rating\":\"(not (#{20 15 50 40 25 35 5 45 30 10} 0))\"}}")
             (let [{:keys [status body]} (test-api-request :post (str "/badge/save_settings/" badge-id) (assoc badge-settings :rating 60))]
               status => 400
               body => "{\"errors\":{\"rating\":\"(not (#{20 15 50 40 25 35 5 45 30 10} 60))\"}}")
             (let [{:keys [status]} (test-api-request :post (str "/badge/save_settings/" badge-id) (assoc badge-settings :rating nil))]
               status => 200)
             (let [{:keys [status]} (test-api-request :post (str "/badge/save_settings/" badge-id) (assoc badge-settings :rating 35))]
               status => 200))

       (fact "badge visibility value must be: public, internal or private"
             (let [{:keys [status body]} (test-api-request :post (str "/badge/save_settings/" badge-id) (assoc badge-settings :visibility "hidden"))]
               status => 400
               body => "{\"errors\":{\"visibility\":\"(not (#{\\\"internal\\\" \\\"private\\\" \\\"public\\\"} \\\"hidden\\\"))\"}}")
             (let [{:keys [status]} (test-api-request :post (str "/badge/save_settings/" badge-id) (assoc badge-settings :visibility "internal"))]
               status => 200))

       (fact "badges can be tagged. Tags value must be collection of strings or nil."
             (let [{:keys [status body]} (test-api-request :post (str "/badge/save_settings/" badge-id) (assoc badge-settings :tags "tag"))]
               status => 400
               body => "{\"errors\":{\"tags\":\"(not (sequential? \\\"tag\\\"))\"}}")
             (let [{:keys [status]} (test-api-request :post (str "/badge/save_settings/" badge-id) (assoc badge-settings :tags nil))]
               status => 200)
             (let [{:keys [status]} (test-api-request :post (str "/badge/save_settings/" badge-id) (assoc badge-settings :tags []))]
               status => 200)
             (let [{:keys [status]} (test-api-request :post (str "/badge/save_settings/" badge-id) (assoc badge-settings :tags ["first tag" "second tag"]))]
               status => 200))

       (logout!))

(migrator/reset-seeds (migrator/test-config))