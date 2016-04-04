(ns salava.badge.info-test
  (:require [midje.sweet :refer :all]
            [salava.core.migrator :as migrator]
            [salava.test-utils :refer [test-api-request login! logout! test-user-credentials]]))

(def test-user 1)

(def badge-id 1)

(def badge-not-owned-by-user 3)

(facts "about viewing a badge"

       (fact "user must be logged in to view badge"
             (:status (test-api-request :get (str "/badge/info/" badge-id))) => 401
             (:status (test-api-request :post (str "/badge/set_visibility/" badge-id) {:visibility "public"})) => 401
             (:status (test-api-request :post "/badge/toggle_recipient_name/1" {:show_recipient_name true})) => 401)


       (apply login! (test-user-credentials test-user))

       (fact "badge exists and has name, visibility and status"
             (let [{:keys [status body]} (test-api-request :get (str "/badge/info/" badge-id))]
               status => 200
               (keys body) => (contains [:name :visibility :status] :in-any-order :gaps-ok)))

       (fact "badge does not exist"
             (let [{:keys [status body]} (test-api-request :get "/badge/info/99")]
               status => 401
               body => ""))

       (fact "badge visibility can be updated. Value must be: private, internal or public"
             (let [{:keys [status body]} (test-api-request :post (str "/badge/set_visibility/" badge-id) {})]
               status => 400
               body => "{\"errors\":{\"visibility\":\"missing-required-key\"}}")
             (let [{:keys [status body]} (test-api-request :post (str "/badge/set_visibility/" badge-id) {:visibility "hidden"})]
               status => 400
               body => "{\"errors\":{\"visibility\":\"(not (#{\\\"private\\\" \\\"public\\\"} \\\"hidden\\\"))\"}}")
             (let [{:keys [status body]} (test-api-request :post (str "/badge/set_visibility/" badge-id) {:visibility "public"})]
               status => 200
               body => 1)
             (let [{:keys [status body]} (test-api-request :get (str "/badge/info/" badge-id))]
               status => 200
               (:visibility body) => "public"))

       (fact "badge owner's name can be set visible/hidden"
             (let [{:keys [status body]} (test-api-request :post "/badge/toggle_recipient_name/1" {:show_recipient_name "show"})]
               status => 400
               body => "{\"errors\":{\"show_recipient_name\":\"(not (#{true false} \\\"show\\\"))\"}}")
             (let [{:keys [status body]} (test-api-request :post "/badge/toggle_recipient_name/1" {:show-recipient-name false})]
               status => 400
               body => "{\"errors\":{\"show_recipient_name\":\"missing-required-key\",\"show-recipient-name\":\"disallowed-key\"}}")
             (let [{:keys [status body]} (test-api-request :post "/badge/toggle_recipient_name/1" {:show_recipient_name true})]
               status => 200
               body => 1)
             (let [{:keys [status body]} (test-api-request :get "/badge/info/1")]
               status => 200
               (:show_recipient_name body) => true))

       (logout!))

(migrator/reset-seeds (migrator/test-config))