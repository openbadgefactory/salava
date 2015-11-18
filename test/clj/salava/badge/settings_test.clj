(ns salava.badge.settings-test
  (:require [midje.sweet :refer :all]
            [salava.test-utils :refer [test-api-request]]))

(def badge-settings
  {:tags ["Sample tag" "Second tag"]
   :rating 5
   :evidence-url "http://example.com/evidence"
   :visibility "internal"})

(def badge-id 2)

(fact "Badge settings can be accessed"
      (let [{:keys [status body]} (test-api-request :get (str "/badge/settings/" badge-id))]
        status => 200
        (:rating body) => nil
        (:evidence-url body) => nil
        (:visibility body) => "private"))

(fact "When editing badge settings: tags, rating, evidence URL and visibility are required"
      (let [{:keys [status body]} (test-api-request :post (str "/badge/save_settings/" badge-id) {})]
        status => 400
        body => "{\"errors\":{\"tags\":\"missing-required-key\",\"rating\":\"missing-required-key\",\"evidence-url\":\"missing-required-key\",\"visibility\":\"missing-required-key\"}}"))

(fact "Badge rating must be between 0-5"
      (let [{:keys [status body]} (test-api-request :post (str "/badge/save_settings/" badge-id) (assoc badge-settings :rating 6))]
        status => 400
        body => "{\"errors\":{\"rating\":\"(not (#{0 1 4 3 2 5} 6))\"}}")
      (let [{:keys [status]} (test-api-request :post (str "/badge/save_settings/" badge-id) (assoc badge-settings :rating 0))]
        status => 200))

(fact "Badge visibility value must be: public, internal or private"
      (let [{:keys [status body]} (test-api-request :post (str "/badge/save_settings/" badge-id) (assoc badge-settings :visibility "hidden"))]
        status => 400
        body => "{\"errors\":{\"visibility\":\"(not (#{\\\"internal\\\" \\\"private\\\" \\\"public\\\"} \\\"hidden\\\"))\"}}")
      (let [{:keys [status]} (test-api-request :post (str "/badge/save_settings/" badge-id) (assoc badge-settings :visibility "internal"))]
        status => 200))

(fact "Bags can be tagged. Tags value must be collection of strings or nil"
      (let [{:keys [status body]} (test-api-request :post (str "/badge/save_settings/" badge-id) (assoc badge-settings :tags "tag"))]
        status => 400
        body => "{\"errors\":{\"tags\":\"(not (sequential? \\\"tag\\\"))\"}}")
      (let [{:keys [status]} (test-api-request :post (str "/badge/save_settings/" badge-id) (assoc badge-settings :tags nil))]
        status => 200)
      (let [{:keys [status]} (test-api-request :post (str "/badge/save_settings/" badge-id) (assoc badge-settings :tags []))]
        status => 200)
      (let [{:keys [status]} (test-api-request :post (str "/badge/save_settings/" badge-id) (assoc badge-settings :tags ["first tag" "second tag"]))]
        status => 200))