(ns salava.badge.evidence-test
  (:require [salava.badge.main :as b]
            [clojure.test :refer :all]
            [salava.core.test-utils :as t]
            [salava.core.migrator :as migrator]
            [salava.core.util :refer [get-full-path]]))

(def test-user {:id 1 :role "user" :private false :activated true})
(def test-evidence {:name "test-evidence"
                    :narrative "Just testing"
                    :url "http://example.com"})
(def badge-id 2)

(t/deftest-ctx evidence-test [ctx]
  (testing "Evidences"
    (testing "evidence count"
      (let [connect (b/badge-evidences ctx badge-id)]
        (is (= 1 (count connect)))
        ))
    (testing "new evidence"
      (let [id (b/save-new-evidence! ctx (:id test-user) badge-id test-evidence)]
        (testing "get evidences"
          (let [evidences (b/badge-evidences ctx badge-id)]
            (is (= 2 (count evidences)))
            (is (= (:name test-evidence) (:name (last evidences))))
            ))
        (testing "update evidences"
          (let [connect (b/update-evidence! ctx badge-id id (assoc test-evidence :narrative "Testing"))
                evidences (b/badge-evidences ctx badge-id)]
            (is (= connect id))
            (is (= "Testing") (:name (last evidences)))
            ))
        (testing "delete evidence"
          (let [connect (b/delete-evidence! ctx id badge-id (:id test-user))
                evidences (b/badge-evidences ctx badge-id)]
            (is (= "success" (:status connect)))
            (is (= 1 (count evidences)))
            ))
        )
      )

    (testing "evidence properties"
      (let [id (b/save-new-evidence! ctx (:id test-user) badge-id (assoc test-evidence :resource_type "url"))]
        (testing "evidence property"
          (let [connect (b/badge-evidences ctx badge-id (:id test-user))
                evidence (last connect)]
            (is (= "url" (get-in evidence [:properties :resource_type])))
            (is (= false (get-in evidence [:properties :hidden])))))

        (testing "hide evidence"
          (let [connect (b/toggle-show-evidence! ctx badge-id id true (:id test-user))
                evidences (b/badge-evidences ctx badge-id (:id test-user))]
            (is (= "success" (:status connect)))
            (is (= true (get-in (last evidences) [:properties :hidden])))
            ))
        (testing "show evidence"
          (let [connect (b/toggle-show-evidence! ctx badge-id id false (:id test-user))
                evidences (b/badge-evidences ctx badge-id (:id test-user))]
            (is (= "success" (:status connect)))
            (is (= false (get-in (last evidences) [:properties :hidden])))
            ))
        (testing "delete evidence"
          (let [connect (b/delete-evidence! ctx id badge-id (:id test-user))
                evidences (b/badge-evidences ctx badge-id)]
            (is (= "success" (:status connect)))
            (is (= 1 (count evidences)))
            ))
        )
      )
    (testing "resource is evidence"
      (let [full-path (get-full-path ctx)
            page-url (str full-path "/page/view/" 1)
            id (b/save-new-evidence! ctx (:id test-user) badge-id (assoc test-evidence :resource_type "page" :url page-url :resource_id 1))]
        (testing "is-evidence?"
          (let [connect (b/is-evidence? ctx (:id test-user) {:id 1 :resource_type "page"})]
            (is (= true connect))
            ))

        (testing "delete evidence"
          (let [connect (b/delete-evidence! ctx id badge-id (:id test-user))
                evidences (b/badge-evidences ctx badge-id)]
            (is (= "success" (:status connect)))
            (is (= 1 (count evidences)))
            ))

        )
      )
    )
  )

(migrator/reset-seeds (migrator/test-config))
