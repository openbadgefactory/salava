(ns salava.badge.endorsement-test
 (:require [salava.core.migrator :as migrator]
           [salava.core.test-utils :as t]
           [clojure.test :refer :all]))

(t/deftest-ctx endorsement-test [ctx]
  (testing "Endorsements"
    (testing "Endorse")
    (testing "Accept endorsment")
    (testing "Reject endorsment")
    (testing "Remove endorsement")))


