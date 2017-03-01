(ns salava.badge.import-test
  (:require [midje.sweet :refer :all]
            [salava.core.migrator :as migrator]
            [salava.test-utils :refer [test-api-request login! logout! test-user-credentials]]))

(def user-with-mozilla-account 9)

(def user-with-no-mozilla-account 1)

(def test-data-with-empty-keys
  {:keys []})

(def test-data-without-keys
  {})

(def test-data-with-expired-key
  {:keys["6b81c60552ac4b55dc649724935998858af9abda1bd96a9c2be645c0e544a6cf"]})

(def test-data-with-ok-key
  {:keys ["2af150efc6cb3f9ee0963a9b4d2f92536d1d28b8328c48ea886290464c839e96"]})

(defn expired? [badge]
       (and (= (:status badge) "invalid")
            (= (:message badge) "Badge is expired")))

(facts "about choosing the badges to import from Mozilla Backpack"

       (apply login! (test-user-credentials user-with-no-mozilla-account))

       (fact "user can not import badges if he has no Mozilla Backback account"
         (let [{:keys [status body]} (test-api-request :get (str "/badge/import"))]
           status => 200
           (empty? (:badges body)) => true))

       (logout!)

       (fact "user must be logged in to choose the badges to import"
         (let [{:keys [status body]} (test-api-request :get (str "/badge/import"))]
           status => 401))

       (apply login! (test-user-credentials user-with-mozilla-account))

       (fact "one of the badges is expired"
         (let [{:keys [status body]} (test-api-request :get (str "/badge/import"))
               badges (:badges body)]
           status => 200
           (some #(expired? %) badges) => true))

       (fact "one of the badges contains an error")

       (logout!))


(facts "about choosing the badges to import from Mozilla Backpack"

       (fact "user must be logged in to import badges"
           (:status (test-api-request :post (str "/badge/import_selected") test-data-with-empty-keys)) => 401)

       (apply login! (test-user-credentials user-with-mozilla-account))

       (fact "'keys' is required parameter to import the badges"
         (let [{:keys [status body]} (test-api-request :post (str "/badge/import_selected") test-data-without-keys)]
            status => 400
            body => "{\"errors\":{\"keys\":\"missing-required-key\"}}"))

       (fact "user can not import an expired badge"
         (let [{:keys [status body]} (test-api-request :post (str "/badge/import_selected") test-data-with-expired-key)]
            status => 200
            (and (== 0 (:saved-count body))(== 1(:error-count body))) => true))

       (fact "user can not import an erroneous badge")

       (fact "user can import badges successfully"
         (let [{:keys [status body]} (test-api-request :post (str "/badge/import_selected") test-data-with-ok-key)]
            status => 200
            (and (== 1 (:saved-count body))(== 0 (:error-count body))) => true))

       (fact "user can not import badges again"
         (let [{:keys [status body]} (test-api-request :post (str "/badge/import_selected") test-data-with-expired-key)]
            status => 200
            (and (== 0 (:saved-count body))(== 1(:error-count body))) => true))

       (logout!))

(migrator/reset-seeds (migrator/test-config))
