(ns salava.badge.endorsement-test
  (:require [salava.core.migrator :as migrator]
            [salava.core.test-utils :as t]
            [clojure.test :refer :all]
            [salava.badge.endorsement :as e]))

(def user {:id 1 :role "user" :private false :activated true})
(def user-2 (assoc user :id 4))
(def user-3 (assoc user :id 5))

(def user-badge {:id 2})
(def user-badge-2 {:id 7})

(t/deftest-ctx endorsement-test [ctx]
  (testing "Endorsements"
    (testing "Endorse own badge"
      (let [endorsement (e/endorse! ctx (:id user-badge-2) (:id user-2) "i endorse this dude")]
        (is (= (:status endorsement) "error"))))
    (testing "Endorse badge"
      (let [endorsement (e/endorse! ctx (:id user-badge-2) (:id user) "i endorse this dude")
            badge-endorsements (e/user-badge-endorsements ctx (:id user-badge-2))]
        (is (= (:status endorsement) "success"))
        (is (= 1 (count badge-endorsements)))

        (testing "Delete endorsement you do not own"
          (let [connect (e/delete! ctx (:id user-badge-2) (:id endorsement) (:id user-3))]
            (is (= "error" (:status connect)))))

        (testing "Delete own endorsement"
          (let [connect (e/delete! ctx (:id user-badge-2) (:id endorsement) (:id user))]
            (is (= "success" (:status connect)))))
        ))

    (testing "Accept endorsement"
      (let [endorsement (e/endorse! ctx (:id user-badge-2) (:id user) "i endorse this dude")
            connect (e/update-status! ctx (:id user-2) (:id user-badge-2) (:id endorsement) "accepted")]
        (is (= (:status endorsement) "success"))
        (is (= (:status connect) "success"))
        (let [check (e/user-badge-endorsements ctx (:id user-badge-2))]
          (is (= 1 (count check)))
          (is (= "accepted" (->> check first :status))))
        (testing "Delete own endorsement"
          (let [connect (e/delete! ctx (:id user-badge-2) (:id endorsement) (:id user))]
            (is (= "success" (:status connect)))))
        ))

    (testing "Decline endorsement"
      (let [endorsement (e/endorse! ctx (:id user-badge) (:id user-3) "This guy rocks")
            badge-endorsements (e/user-badge-endorsements ctx (:id user-badge))]
        (is (= (:status endorsement) "success"))
        (is (= 1 (count badge-endorsements)))

        (let [connect (e/update-status! ctx (:id user) (:id user-badge) (:id endorsement) "declined")
              check (e/user-badge-endorsements ctx (:id user-badge))]
          (is (= "success" (:status connect)))
          (is (= nil (seq check))))
        ))

    (testing "Edit endorsement"
      (let [endorsement (e/endorse! ctx (:id user-badge) (:id user-3) "This guy rocks")
            _ (e/update-status! ctx (:id user) (:id user-badge) (:id endorsement) "accepted")
            badge-endorsements (e/user-badge-endorsements ctx (:id user-badge))]
        (is (= (:status endorsement) "success"))
        (is (= 1 (count badge-endorsements)))
        (is (= (->> badge-endorsements first :status) "accepted"))

        (let [connect (e/edit! ctx (:id user-badge) (:id endorsement) "This guy sucks!" (:id user-3))
              badge-endorsements (e/user-badge-endorsements ctx (:id user-badge))]
          (is (= (:status connect) "success"))
          (is (= (->> badge-endorsements first :status) "pending"))
          #_(is (= (->> badge-endorsements first :content) "This guy sucks!"))
          )

        )
      )))

(migrator/reset-seeds (migrator/test-config))

