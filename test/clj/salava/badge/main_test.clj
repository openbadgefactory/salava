(ns salava.badge.main-test
  (:require [salava.badge.main :as b]
            [clojure.test :refer :all]
            [salava.core.test-utils :as t]))

(t/deftest-ctx main-test [ctx]
  (testing "with correct values"
    (is (= "http://localhost:5000/badge/info/1" (b/badge-url ctx 1)))))

