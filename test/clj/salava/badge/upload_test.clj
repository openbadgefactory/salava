(ns salava.badge.upload-test
  (:require [clojure.java.io :as io]
            [midje.sweet :refer :all]
            [salava.test-utils :refer [test-api-request test-upload login! logout! test-user-credentials]]))

(def test-user 1)

(facts "about uploading PNG-image containing a badge"

       (fact "user must logged in to upload badge"
             (:status (test-upload (str "/badge/upload")
                                   [{:part-name "file"
                                     :name      "not-an-image.txt"
                                     :mime-type "type/text"
                                     :content   (clojure.java.io/file (io/resource "test/not-an-image.txt"))}])) => 401)

       (apply login! (test-user-credentials test-user))

       (fact "badge upload should accept only PNG images"
             (let [{:keys [status body]} (test-upload (str "/badge/upload")
                                                      [{:part-name "file"
                                                        :name      "not-an-image.txt"
                                                        :mime-type "type/text"
                                                        :content   (clojure.java.io/file (io/resource "test/not-an-image.txt"))}])]
               (:reason body) => "Invalid file type"))
       (fact "uploaded badge should contain metadata"
             (let [{:keys [status body]} (test-upload (str "/badge/upload")
                                                      [{:part-name "file"
                                                        :name      "no-metadata.png"
                                                        :mime-type "image/png"
                                                        :content   (clojure.java.io/file (io/resource "test/no-metadata.png"))}])]
               status => 200
               (:status body) => "error"
               (:message body) => "Error while uploading badge"
               (:reason body) => "Empty metadata"))

       (logout!)
       )
