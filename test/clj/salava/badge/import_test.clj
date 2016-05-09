(ns salava.badge.import-test
  (:require [midje.sweet :refer :all]
            [salava.core.migrator :as migrator]
            [salava.test-utils :refer [test-api-request login! logout! test-user-credentials]]))

(def user-with-mozilla-account 9)

(def user-with-no-mozilla-account 1)

(facts "about choosing the badges to import from Mozilla Backpack"

       (apply login! (test-user-credentials user-with-no-mozilla-account))

       (fact "user can not import badges if he has no Mozilla Backback account")

       (logout!)

       (apply login! (test-user-credentials user-with-mozilla-account))

       (fact "user must be logged in to choose the badges to import")

       (fact "one of the badges is expired")

       (fact "one of the badges contains an error")

       (logout!))

(facts "about choosing the badges to import from Mozilla Backpack"

       (fact "user must be logged in to import badges")

       (apply login! (test-user-credentials user-with-mozilla-account))

       (fact "'keys' is required parameter to import the badges")

       (fact "user can not import an expired badge")

       (fact "user can not import an erroneous badge")

       (fact "user can import badges successfully")

       (fact "user can not import badges again")

       (logout!))

(migrator/reset-seeds (migrator/test-config))
