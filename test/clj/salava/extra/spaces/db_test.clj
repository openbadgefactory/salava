(ns salava.extra.spaces.db-test
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

#_(def space2 [{:uuid (uuid)
                :name "msftembo"
                :status "active"
                :visibility "open"
                :logo "https://brandmark.io/logo-rank/random/pepsi.png"
                :banner ""
                :admin [user-email3]}])

(def space-without-admin {:uuid (uuid)
                          :name "Family guy space"
                          :status "active"
                          :visibility "open"
                          :logo "https://brandmark.io/logo-rank/random/pepsi.png"
                          :banner ""
                          :admin []})

(def test-spaces [{:uuid (uuid)
                   :name "Hpass"
                   :description ""
                   :status "active"
                   :visibility "open"
                   :logo "https://techcrunch.com/wp-content/uploads/2018/07/logo-2.png?w=300"
                   :banner ""
                   :admin (vector user-email1  user-email2)}
                  {:uuid (uuid)
                   :name "msftembo"
                   :status "active"
                   :visibility "open"
                   :logo "https://brandmark.io/logo-rank/random/pepsi.png"
                   :banner ""
                   :admin [user-email3]}])

(t/deftest-ctx spaces-test [ctx]
               (testing "delete spaces"
                 (let [spaces (all-spaces ctx)]
                   (delete! ctx (map :id spaces))
                   (is (= 0 (count (all-spaces ctx))))));)


               (testing "create space"
                 (let [response (create! ctx space1)
                       spaces (all-spaces ctx)]
                   (is (=  (count spaces)))
                   (is (= "success" (:status response)))))

               (testing "create existing space"
                 (let [response (create! ctx space1)
                       spaces (all-spaces ctx)]
                   (is (= "error" (:status response)))
                   (is (= 1 (count spaces)))))

               (testing "create space with no admin defined"
                 (let [response (create! ctx space-without-admin)
                       spaces (all-spaces ctx)]
                   (is (= "error" (:status response)))
                   (is (= 1 (count spaces))))))

;(migrator/reset-seeds (migrator/test-config))
