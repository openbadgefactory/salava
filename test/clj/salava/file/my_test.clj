(ns salava.file.my-test
  (:require [clojure.java.io :as io]
            [slingshot.slingshot :refer :all]
            [midje.sweet :refer :all]
            [salava.test-utils :refer [test-api-request test-upload login! logout! test-user-credentials]]
            [salava.core.i18n :refer [t]]))

(def test-user 1)

(defn file-exists? [path]
  (try+
    (-> path
        io/resource
        io/as-file
        .exists)
    (catch Exception _)))

(facts "about accessin user's files"
       (fact "user must be logged in to access files"
             (:status (test-api-request :get (str "/file"))) => 401)

       (apply login! (test-user-credentials test-user))

       (let [{:keys [status body]} (test-api-request :get (str "/file"))]
         (fact "user has two files"
               status => 200
               (count body) => 2)
         (fact "user file has valid attributes"
               (keys (first body)) => (just [:id :name :path :mime_type :size :ctime :mtime :tags] :in-any-order)))

       (logout!))

(facts "about uploading a file"
       (fact "user must be logged in to upload a file"
             (let [upload-data {:part-name "file"
                                :name      "sample.doc"
                                :mime-type "application/msword"
                                :content   (clojure.java.io/file (io/resource "test/sample.doc"))}
                   {:keys [status]} (test-upload (str "/file/upload") [upload-data])]
               status => 401))

       (apply login! (test-user-credentials test-user))

       (fact "file must be provided"
             (let [{:keys [status body]} (test-upload (str "/file/upload") [])]
               status => 400
               body =>  "{\"errors\":{\"file\":\"missing-required-key\"}}"))

       (fact "file can't be too big"
             (let [upload-data {:part-name "file"
                                :name "too-large.pdf"
                                :mime-type "application/pdf"
                                :content (clojure.java.io/file (io/resource "test/too-large.pdf"))}
                   {:keys [status body]} (test-upload (str "/file/upload") [upload-data])]
               status => 200
               (:status body) => "error"
               (:message body) => (t :file/Errorwhileuploading)))

       (fact "file type must be valid"
             (let [upload-data {:part-name "file"
                                :name "invalid-filetype.bmp"
                                :mime-type "image/bmp"
                                :content (clojure.java.io/file (io/resource "test/invalid-filetype.bmp"))}
                   {:keys [status body]} (test-upload (str "/file/upload") [upload-data])]
               status => 200
               (:status body) => "error"
               (:message body) => (t :file/Errorwhileuploading)))

       (fact "user can upload a valid file"
             (let [upload-data {:part-name "file"
                                :name      "sample.doc"
                                :mime-type "application/msword"
                                :content   (clojure.java.io/file (io/resource "test/sample.doc"))}
                   {:keys [status body]} (test-upload (str "/file/upload") [upload-data])
                   {:keys [id name path mime_type size] :as upload-data} (:data body)]
               status => 200
               (:status body) => "success"
               (keys upload-data) => (just [:id :name :path :mime_type :size :ctime :mtime :tags] :in-any-order)
               id => 4
               name => "sample.doc"
               path => "file/3/9/f/c/39fcd778de26be5d2c95a0adc8e945354003379b964f31dca5d5d0edd927128.doc"
               mime_type => "application/msword"
               size => 9216))

       (logout!))

(facts "about deleting a file"
       (fact "user must be logged in to delete a file"
             (:status (test-api-request :delete "/file/3")) => 401)

       (apply login! (test-user-credentials test-user))

       (fact "file id must be valid"
             (let [{:keys [body status]} (test-api-request :delete "/file/not-valid")]
               status => 400
               body => "{\"errors\":{\"fileid\":\"(not (instance? java.lang.Long \\\"not-valid\\\"))\"}}"))

       (fact "file exists"
             (let [{:keys [body status]} (test-api-request :delete "/file/99")]
               status => 200
               (:status body) => "error"
               (:message body) => (t :file/Errorwhiledeleting)))

       (fact "user must be the owner of the file"
             (let [{:keys [body status]} (test-api-request :delete "/file/3")]
               status => 200
               (:status body) => "error"
               (:message body) => (t :file/Errorwhiledeleting)))

       (fact "file instance can be deleted"
             (let [{:keys [body status]} (test-api-request :delete "/file/4")]
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
             (file-exists? "public/file/3/9/f/c/39fcd778de26be5d2c95a0adc8e945354003379b964f31dca5d5d0edd927128.doc") => nil)

       (logout!))