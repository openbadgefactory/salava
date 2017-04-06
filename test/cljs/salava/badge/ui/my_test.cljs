(ns salava.badge.ui.my_test
  (:require [cljs.test :refer-macros [deftest is testing run-tests]]))

(deftest test-numbers
  (is (= 1 1)))

(enable-console-print!)

(run-tests)
