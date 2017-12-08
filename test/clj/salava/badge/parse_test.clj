(ns salava.badge.parse-test
  (:require [salava.badge.parse :as p]
            [clojure.test :refer :all]
            [clojure.java.io :as io]
            [salava.core.test-utils :as t])
  (:import (ar.com.hjg.pngj PngjInputException)
           (org.xml.sax SAXParseException)))
(def test-user {:id 1 :role "user" :private false})
(def invalid-file {:content-type "text/plain" :tempfile (io/as-file (io/resource "test/not-an-image.txt"))})
(def invalid-image {:content-type "image/bmp" :tempfile (io/as-file (io/resource "test/invalid-filetype.bmp"))})

(def invalid-png-1 {:content-type "image/png" :tempfile (io/as-file (io/resource "test/not-an-image.txt"))})
(def invalid-png-2 {:content-type "image/png" :tempfile (io/as-file (io/resource "test/no-metadata.png"))})
(def invalid-png-3 {:content-type "image/png" :tempfile (io/as-file (io/resource "test/invalid-metadata.png"))})

(def invalid-svg {:content-type "image/svg+xml" :tempfile (io/as-file (io/resource "test/not-an-image.txt"))})

#_(def valid-svg   {:content-type "image/svg+xml" :tempfile (io/as-file (io/resource "test/"))})
#_(def valid-png   {:content-type "image/png" :tempfile (io/as-file (io/resource "test/"))})

#_(t/deftest-ctx parse-test [ctx]
  (testing "file->assertion"
    (testing "with invalid file"
      (is (thrown? IllegalArgumentException (p/file->assertion invalid-file)))
      (is (thrown? IllegalArgumentException (p/file->assertion invalid-image)))
      (is (thrown? PngjInputException       (p/file->assertion invalid-png-1)))
      (is (thrown? IllegalArgumentException (p/file->assertion invalid-png-2)))
      (is (thrown? IllegalArgumentException (p/file->assertion invalid-png-3)))
      (is (thrown? SAXParseException        (p/file->assertion invalid-svg))))

    (testing "with valid png file"

      )
    (testing "with valid svg file"

      )
  )

  (testing "str->assertion"
    (testing "with invalid url"

      )
    (testing "with valid url"

      )
    (testing "with invalid jws"

      )
    (testing "with valid jws"

      )
  )
)
