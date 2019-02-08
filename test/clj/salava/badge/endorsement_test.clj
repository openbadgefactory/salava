(ns salava.badge.endorsement-test
  (:require [salava.core.migrator :as migrator]
            [salava.core.test-utils :as t]
            [clojure.test :refer :all]
            [salava.badge.endorsement :as e]))

(def user {:id 1 :role "user" :private false :activated true})
(def user-2 (assoc user :id 4))
(def user-3 (assoc user :id 5))

(def badge 2)
(def badge-2 7)

(t/deftest-ctx endorsement-test [ctx]
  (testing "Endorsements"
      (testing "Endorse own badge"
        (let [endorsement (e/endorse! ctx badge-2 (:id user-2) "i endorse this dude")]
          (is (= (:status endorsement) "error"))))
      (testing "Endorse badge"
        (let [endorsement (e/endorse! ctx badge-2 (:id user) "i endorse this dude")
              badge-endorsements (e/badge-endorsements ctx badge-2)]
          (is (= (:status endorsement) "success"))
          (is (= 1 (count badge-endorsements)))

          (testing "Delete endorsement you do not own"
            (let [connect (e/delete! ctx badge-2 (:id endorsement) (:id user-3))]
              (is (= "error" (:status connect)))))

          (testing "Delete own endorsement"
            (let [connect (e/delete! ctx badge-2 (:id endorsement) (:id user))]
              (is (= "success" (:status connect)))))
          ))

    (testing "Accept endorsment"
      (let [endorsement (e/endorse! ctx badge-2 (:id user) "i endorse this dude")
            connect (e/update-status! ctx (:id user-2) badge-2 (:id endorsement) "accepted")]
        (is (= (:status endorsement) "success"))
        (is (= (:status connect) "success"))
        (let [check (e/badge-endorsements ctx badge-2)]
          (is (= 1 (count check)))
          (is (= "pending" (->> check first :status))))))

    (testing "Reject endorsment")
    (testing "Remove endorsement")))

(migrator/reset-seeds (migrator/test-config))

