(ns salava.extra.spaces.db-test
  (:require [salava.extra.spaces.db :as db]
   [salava.extra.spaces.space :as space]
   [clojure.test :refer :all]
   [salava.core.migrator :as migrator]
   [salava.core.test-utils :as t]))

(def test-data [{:uuid (space/uuid)
                 :name "Hpass"
                 :description ""
                 :status "active"
                 :visibility "open"
                 :logo "https://techcrunch.com/wp-content/uploads/2018/07/logo-2.png?w=300"
                 :banner ""
                 :admin ["isaac.ogunlolu@discendum.com", "isaac.ogunlolu+test100000@discendum.com"]}
                {:uuid (space/uuid)
                 :name "msftembo"
                 :status "active"
                 :visibility "open"
                 :logo "https://brandmark.io/logo-rank/random/pepsi.png"
                 :banner ""
                 :admin ["isaac.ogunlolu@discendum.com", "isaac.ogunlolu+test000@discendum.com"]}])

(t/deftest-ctx main-test [ctx]
 (testing "create new space"
  (let [response (space/create! ctx test-data)]
   (is (= "success" (:status response))))))
