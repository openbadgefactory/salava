(ns salava.file.my-test
  (:require [clojure.java.io :as io]
            [slingshot.slingshot :refer :all]
            [midje.sweet :refer :all]
            [salava.test-utils :refer [test-api-request test-upload]]
            [salava.core.i18n :refer [t]]))

(def user-id 1)

(defn file-exists? [path]
  (try+
    (-> path
        io/resource
        io/as-file
        .exists)
    (catch Exception _)))

(facts "about user's files"
       (let [{:keys [status body]} (test-api-request :get (str "/file/" user-id))]
         (fact "user has two files"
               status => 200
               (count body) => 2)
         (fact "user file has valid attributes"
               (keys (first body)) => (just [:id :name :path :mime_type :size :ctime :mtime :tags] :in-any-order)))

       (fact "user has no files"
             (let [{:keys [status body]} (test-api-request :get "/file/99")]
               status => 200
               body => [])))

(facts "about uploading a file"
       (fact "file must be provided"
             (let [{:keys [status body]} (test-upload (str "/file/upload/" user-id) [])]
               status => 400
               body =>  "{\"errors\":{\"file\":\"missing-required-key\"}}"))

       (fact "file can't be too big"
             (let [upload-data {:part-name "file"
                                :name "too-large.pdf"
                                :mime-type "application/pdf"
                                :content (clojure.java.io/file (io/resource "test/too-large.pdf"))}
                   {:keys [status body]} (test-upload (str "/file/upload/" user-id) [upload-data])]
               status => 200
               (:status body) => "error"
               (:message body) => (t :file/Errorwhileuploading)))

       (fact "file type must be valid"
             (let [upload-data {:part-name "file"
                                :name "invalid-filetype.bmp"
                                :mime-type "image/bmp"
                                :content (clojure.java.io/file (io/resource "test/invalid-filetype.bmp"))}
                   {:keys [status body]} (test-upload (str "/file/upload/" user-id) [upload-data])]
               status => 200
               (:status body) => "error"
               (:message body) => (t :file/Errorwhileuploading)))

       (fact "user can upload a valid file"
             (let [upload-data {:part-name "file"
                                :name      "sample.doc"
                                :mime-type "application/msword"
                                :content   (clojure.java.io/file (io/resource "test/sample.doc"))}
                   {:keys [status body]} (test-upload (str "/file/upload/" user-id) [upload-data])
                   {:keys [id name path mime_type size] :as upload-data} (:data body)]
               status => 200
               (:status body) => "success"
               (keys upload-data) => (just [:id :name :path :mime_type :size :ctime :mtime :tags] :in-any-order)
               id => 3
               name => "sample.doc"
               path => "file/3/9/f/c/39fcd778de26be5d2c95a0adc8e945354003379b964f31dca5d5d0edd927128.doc"
               mime_type => "application/msword"
               size => 9216)))

(facts "about deleting a file"
       (fact "file id must be valid"
             (let [{:keys [body status]} (test-api-request :delete "/file/not-valid")]
               status => 400
               body => "{\"errors\":{\"fileid\":\"(not (instance? java.lang.Long \\\"not-valid\\\"))\"}}"))

       (fact "file exists"
             (let [{:keys [body status]} (test-api-request :delete "/file/99")]
               status => 200
               (:status body) => "error"
               (:message body) => (t :file/Errorwhiledeleting)))

       (fact "file instance can be deleted"
             (let [{:keys [body status]} (test-api-request :delete "/file/3")]
               status => 200
               (:status body) => "success"
               (:message body) => (t :file/Filedeleted)))

       (fact "file still exists"
             (file-exists? "public/file/3/9/f/c/39fcd778de26be5d2c95a0adc8e945354003379b964f31dca5d5d0edd927128.doc") => true)

       (fact "another file instance can be deleted"
             (let [{:keys [body status]} (test-api-request :delete "/file/2")]
               status => 200
               (:status body) => "success"
               (:message body) => (t :file/Filedeleted)))

       (fact "file is actually deleted"
             (file-exists? "public/file/3/9/f/c/39fcd778de26be5d2c95a0adc8e945354003379b964f31dca5d5d0edd927128.doc") => nil))