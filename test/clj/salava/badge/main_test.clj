(ns salava.badge.main-test
  (:require [salava.badge.main :refer [badge-url]]
            [clojure.test :refer :all]
            [salava.core.test-utils :as t]
            [salava.core.util :refer [get-site-url get-base-path]]))

(t/deftest-ctx main-test [ctx]
  (testing "with correct values"
    (is (=  (str (get-site-url ctx) (get-base-path ctx) "/badge/info/1")  (badge-url ctx 1)))))
