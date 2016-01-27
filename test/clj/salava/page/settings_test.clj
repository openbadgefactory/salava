(ns salava.page.settings-test
  (:require [midje.sweet :refer :all]
            [salava.test-utils :refer [test-api-request login! logout! test-user-credentials]]
            [salava.core.helper :refer [dump]]))

(def test-user 1)

(def page-id 1)

(def page-owned-by-another-user 3)

(facts "about editing page settings"
       (fact "user must be logged in to view page settings"
             (:status (test-api-request :get (str "/page/settings/" page-id))) => 401)

       (apply login! (test-user-credentials test-user))

       (fact "user cannot access the settings of the page owned by another user"
             (let [{:keys [status body]} (test-api-request :get (str "/page/settings/" page-owned-by-another-user))]
               status => 500
               body => "{\"errors\":\"(not (map? nil))\"}"))

       (let [{:keys [status body]} (test-api-request :get (str "/page/settings/" page-id))]
         (fact "page settings can be fetched for editing"
               status => 200)
         (fact "page has valid attributes"
               (keys body) => (just [:description :tags :first_name :password :name :visible_before :visible_after :theme :ctime :id :padding :last_name :user_id :border :visibility :mtime] :in-any-order)))

       (logout!))

(facts "about saving page settings"
       (fact "user must be logged in to save page settings"
             (:status (test-api-request :post (str "/page/save_settings/" page-id) {:password "" :tags [] :visibility "private"})) => 401)

       (apply login! (test-user-credentials test-user))

       (fact "user must be owner of the page"
             (let [{:keys [status body]} (test-api-request :post (str "/page/save_settings/" page-owned-by-another-user) {:password "" :tags [] :visibility "private"})]
               status => 200
               (:status body) => "error"))

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
               (:visibility body) => "private"))

       (logout!))