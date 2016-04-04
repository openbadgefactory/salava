(ns salava.badge.info-test
  (:require [midje.sweet :refer :all]
            [salava.core.migrator :as migrator]
            [salava.test-utils :refer [test-api-request login! logout! test-user-credentials]]))

(def test-user 1)

(def private-badge-id 1)

(def another-users-private-badge 4)

(def public-badge-id 5)

(def internal-badge-id 7)

(facts "about viewing a badge"

       (fact "user must be logged in to view private or internal badge"
             (:status (test-api-request :get (str "/badge/info/" private-badge-id))) => 401
             (:status (test-api-request :get (str "/badge/info/" internal-badge-id))) => 401
             (:status (test-api-request :post (str "/badge/set_visibility/" private-badge-id) {:visibility "public"})) => 401
             (:status (test-api-request :post (str "/badge/toggle_recipient_name/" private-badge-id) {:show_recipient_name true})) => 401)

       (fact "anonymous user can view public badges"
             (let [{:keys [status body]} (test-api-request :get (str "/badge/info/" public-badge-id))]
               status => 200
               (map? body) => true))

       (apply login! (test-user-credentials test-user))

       (fact "logged in user can view internal badges"
             (let [{:keys [status body]} (test-api-request :get (str "/badge/info/" internal-badge-id))]
               status => 200
               (map? body) => true))

       (fact "logged in user can not view another user's private badges"
             (:status (test-api-request :get (str "/badge/info/" another-users-private-badge))) => 401)

       (fact "badge exists and has name, visibility and status"
             (let [{:keys [status body]} (test-api-request :get (str "/badge/info/" private-badge-id))]
               status => 200
               (keys body) => (contains [:name :visibility :status :show_evidence] :in-any-order :gaps-ok)))

       (fact "badge does not exist"
             (let [{:keys [status body]} (test-api-request :get "/badge/info/99")]
               status => 401
               body => ""))

       (fact "badge visibility can be updated. Value must be: private, internal or public"
             (let [{:keys [status body]} (test-api-request :post (str "/badge/set_visibility/" private-badge-id) {})]
               status => 400
               body => "{\"errors\":{\"visibility\":\"missing-required-key\"}}")
             (let [{:keys [status body]} (test-api-request :post (str "/badge/set_visibility/" private-badge-id) {:visibility "hidden"})]
               status => 400
               body => "{\"errors\":{\"visibility\":\"(not (#{\\\"private\\\" \\\"public\\\"} \\\"hidden\\\"))\"}}")
             (let [{:keys [status body]} (test-api-request :post (str "/badge/set_visibility/" private-badge-id) {:visibility "public"})]
               status => 200
               body => 1)
             (let [{:keys [status body]} (test-api-request :get (str "/badge/info/" private-badge-id))]
               status => 200
               (:visibility body) => "public"))

       (fact "badge owner's name can be set visible/hidden"
             (let [{:keys [status body]} (test-api-request :post (str "/badge/toggle_recipient_name/" private-badge-id) {:show_recipient_name "show"})]
               status => 400
               body => "{\"errors\":{\"show_recipient_name\":\"(not (#{true false} \\\"show\\\"))\"}}")
             (let [{:keys [status body]} (test-api-request :post (str "/badge/toggle_recipient_name/" private-badge-id) {:show-recipient-name false})]
               status => 400
               body => "{\"errors\":{\"show_recipient_name\":\"missing-required-key\",\"show-recipient-name\":\"disallowed-key\"}}")
             (let [{:keys [status body]} (test-api-request :post (str "/badge/toggle_recipient_name/" private-badge-id) {:show_recipient_name true})]
               status => 200
               body => 1)
             (let [{:keys [status body]} (test-api-request :get (str "/badge/info/" private-badge-id))]
               status => 200
               (:show_recipient_name body) => true))

       (fact "badge evidence visibility can be set visible/hidden"
             (let [{:keys [status body]} (test-api-request :post (str "/badge/toggle_evidence/" private-badge-id) {:show_evidence nil})]
               status => 400
               body => "{\"errors\":{\"show_evidence\":\"(not (#{true false} nil))\"}}")
             (let [{:keys [status body]} (test-api-request :post (str "/badge/toggle_evidence/" private-badge-id) {:show-evidence true})]
               status => 400
               body => "{\"errors\":{\"show_evidence\":\"missing-required-key\",\"show-evidence\":\"disallowed-key\"}}")
             (let [{:keys [status body]} (test-api-request :post (str "/badge/toggle_evidence/" private-badge-id) {:show_evidence true})]
               status => 200
               body => 1)
             (let [{:keys [status body]} (test-api-request :get (str "/badge/info/" private-badge-id))]
               status => 200
               (:show_evidence body) => true))

       (logout!))

(facts "about congratulating badge owner"

       (fact "user must be logged in to congratulate"
             (:status (test-api-request :post (str "/badge/congratulate/" public-badge-id))) => 401)

       (apply login! (test-user-credentials test-user))

       (fact "user cannot congratulate himself"
             (let [{:keys [status body]} (test-api-request :post (str "/badge/congratulate/" private-badge-id))]
               status => 200
               (:status body) => "error"))

       (fact "user can congratulate abother user for receiving a badge"
             (let [{:keys [status body]} (test-api-request :post (str "/badge/congratulate/" public-badge-id))]
               status => 200
               (:status body) => "success"))

       (fact "user cannot congratulate another user for receiving a badge more than once"
             (let [{:keys [status body]} (test-api-request :post (str "/badge/congratulate/" public-badge-id))]
               status => 200
               (:status body) => "error"))

       (logout!))

(migrator/reset-seeds (migrator/test-config))