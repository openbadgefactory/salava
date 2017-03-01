(ns salava.badge.main-test
  (:require [salava.badge.main :as b]
            [clojure.test :refer :all]
            [salava.test-utils :as ts ]))

(def system (ts/get-system))

(def ctx (get-in system [:handler :ctx]))

(deftest badge-url
  
  (testing "with correct values"
    (is (= "http://localhost:3000/app/badge/info/1" (b/badge-url ctx 1))))) 


(ts/stop-system system)

