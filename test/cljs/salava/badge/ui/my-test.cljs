(ns salava.badge.ui.my-test
  (:require [cljs.test :refer-macros [deftest is testing run-tests]]))

(deftest test-numbers
  (is (= 1 1)))

(enable-console-print!)

(run-tests)
