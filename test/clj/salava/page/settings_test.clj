(ns salava.page.settings-test
  (:require [midje.sweet :refer :all]
            [salava.test-utils :refer [test-api-request]]
            [salava.core.helper :refer [dump]]))

(def page-id 1)

(facts "about editing page settings"
       (let [{:keys [status body]} (test-api-request :get (str "/page/settings/" page-id))]
         (fact "page settings can be fetched for editing"
               status => 200)
         (fact "page has valid attributes"
               (keys body) => (just [:description :tags :first_name :password :name :visible_before :visible_after :theme :ctime :id :padding :last_name :user_id :border :visibility :mtime] :in-any-order))))

(facts "about saving page settings"
       (fact "visibility, password and tags are required"
             (let [{:keys [status body]} (test-api-request :post (str "/page/save_settings/" page-id) {})]
               status => 400
               body => "{\"errors\":{\"password\":\"missing-required-key\",\"visibility\":\"missing-required-key\",\"tags\":\"missing-required-key\"}}"))

       (fact "visibility must be valid"
             (let [{:keys [status body]} (test-api-request :post (str "/page/save_settings/" page-id) {:password "" :tags [] :visibility "not-valid"})]
               status => 400
               body => "{\"errors\":{\"visibility\":\"(not (#{\\\"internal\\\" \\\"private\\\" \\\"password\\\" \\\"public\\\"} \\\"not-valid\\\"))\"}}")
             (:status (test-api-request :post (str "/page/save_settings/" page-id) {:password "" :tags [] :visibility "private"})) => 200)

       (fact "tags must be a collection of strings"
             (let [{:keys [status body]} (test-api-request :post (str "/page/save_settings/" page-id) {:password "" :tags "not-valid" :visibility "private"})]
               status => 400
               body => "{\"errors\":{\"tags\":\"(not (sequential? \\\"not-valid\\\"))\"}}")
             (let [{:keys [status body]} (test-api-request :post (str "/page/save_settings/" page-id) {:password "" :tags [0 1] :visibility "private"})]
               status => 400
               body =>  "{\"errors\":{\"tags\":[\"(not (instance? java.lang.String 0))\",\"(not (instance? java.lang.String 1))\"]}}")
             (:status (test-api-request :post (str "/page/save_settings/" page-id) {:password "" :tags [] :visibility "private"})) => 200
             (:status (test-api-request :post (str "/page/save_settings/" page-id) {:password "secret" :tags ["tag" "another tag"] :visibility "password"})) => 200)

       (fact "password must be valid"
             (let [{:keys [status body]} (test-api-request :post (str "/page/save_settings/" page-id) {:password 0 :tags [] :visibility "password"})]
               status => 400
               body => "{\"errors\":{\"password\":\"(not (instance? java.lang.String 0))\"}}")
             (:status (test-api-request :post (str "/page/save_settings/" page-id) {:password "" :tags [] :visibility "private"})) => 200
             (:status (test-api-request :post (str "/page/save_settings/" page-id) {:password "secret" :tags [] :visibility "password"})) => 200

             (:status (test-api-request :post (str "/page/save_settings/" page-id) {:password "not-saved" :tags [] :visibility "private"}))
             (let [{:keys [body]} (test-api-request :get (str "/page/settings/" page-id))]
               (:password body) => "")

             (:status (test-api-request :post (str "/page/save_settings/" page-id) {:password "" :tags [] :visibility "password"}))
             (let [{:keys [body]} (test-api-request :get (str "/page/settings/" page-id))]
               (:visibility body) => "private")))