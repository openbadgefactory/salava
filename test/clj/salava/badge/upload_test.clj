(ns salava.badge.upload-test
  (:require [clojure.java.io :as io]
            [midje.sweet :refer :all]
            [salava.test-utils :refer [test-api-request test-upload]]))

(def user-id 1)

(fact "Badge upload should accept only PNG images"
      (let [{:keys [status body]} (test-upload (str "/badge/upload/" user-id)
                                               [{:part-name "file"
                                                 :name "not-an-image.txt"
                                                 :mime-type "type/text"
                                                 :content (clojure.java.io/file (io/resource "test/not-an-image.txt"))}])]
        status => 200
        (:status body) => "error"
        (:message body) => "Error while uploading badge"
        (:reason body) => "Invalid file type"))

(fact "Uploaded badge should contain metadata"
      (let [{:keys [status body]} (test-upload (str "/badge/upload/" user-id)
                                               [{:part-name "file"
                                                 :name "no-metadata.png"
                                                 :mime-type "image/png"
                                                 :content (clojure.java.io/file (io/resource "test/no-metadata.png"))}])]
        status => 200
        (:status body) => "error"
        (:message body) => "Error while uploading badge"
        (:reason body) => "Empty metadata"))
