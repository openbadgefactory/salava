(ns salava.extra.spaces.routes-test
  (:require [salava.extra.spaces.db :refer [all-spaces]]
            [salava.extra.spaces.space :refer [uuid delete! create!]]
            [clojure.test :refer :all]
            [salava.core.migrator :as migrator]
            [salava.core.test-utils :as t]))

(def user-email1 "third.user@example.com")
(def user-email2 "another.user@example.com")
(def user-email3 "another.user+test100@example.com")

(def space1 {:uuid (uuid)
             :name "Hpass"
             :description ""
             :status "active"
             :visibility "open"
             :logo "https://techcrunch.com/wp-content/uploads/2018/07/logo-2.png?w=300"
             :banner ""
             :admin (vector user-email1  user-email2)})

(def space2 {:uuid (uuid)
               :name "msftembo"
               :status "active"
               :visibility "open"
               :logo "https://brandmark.io/logo-rank/random/pepsi.png"
               :banner ""
               :admin [user-email3]})

(t/deftest-ctx routes-test [ctx]
 (testing "Create space"
  (let [{:keys [status body]} (t/test-api-request ctx :post (str "/obpv1/spaces/create") space2)
        spaces (all-spaces ctx)]
    (is (= 200 status))
    (is (= "success" (:status body))))))
