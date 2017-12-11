(ns salava.badge.event-test
  (:require [salava.badge.event :as event]
            [clojure.test :refer :all]
            [salava.core.migrator :as migrator]
            [salava.core.test-utils :as t]))

(def test-user {:id 2 :role "user" :private false})

(t/deftest-ctx main-test [ctx]

  (testing "Get Badge events"
    (let [connects (event/events ctx (:id test-user))]
      (is (= 1 (count connects)))

      )))
;(migrator/run-test-reset)
(migrator/reset-seeds (migrator/test-config))
