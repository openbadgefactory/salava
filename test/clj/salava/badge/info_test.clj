(ns salava.badge.info-test
  (:require [midje.sweet :refer :all]
            [salava.test-utils :refer [test-api-request]]))

(def badge-id 1)

(fact "Badge exists and has certain name, visibility and status"
      (let [{:keys [status body]} (test-api-request :get (str "/badge/info/" badge-id))]
        status => 200
        (contains? body :name) => true
        (contains? body :visibility) => true
        (contains? body :status) => true))

(fact "Badge does not exist"
      (let [{:keys [status body]} (test-api-request :get "/badge/info/99")]
        status => 200
        body => nil))

(fact "Badge visibility can be updated. Value must be: private, internal or public"
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

(fact "Badge owner's name can be set visible/hidden"
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