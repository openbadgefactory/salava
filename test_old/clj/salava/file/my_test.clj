(ns salava.file.my-test
  (:require [clojure.java.io :as io]
            [slingshot.slingshot :refer :all]
            [midje.sweet :refer :all]
            [salava.core.migrator :as migrator]
            [salava.test-utils :refer [test-api-request test-upload login! logout! test-user-credentials]]
            [salava.core.i18n :refer [t]]))

(def test-user 1)

(def another-users-file 3)

(def file-id 1)

(def file-tags {:tags ["some tag" "another tag"]})

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
               path => "file/0/3/9/f/039fcd778de26be5d2c95a0adc8e945354003379b964f31dca5d5d0edd927128.doc"
               mime_type => "application/msword"
               size => 9216))
       (logout!))

(facts "about uploading image"
       (fact "user must be logged in to upload a image"
             (let [upload-data {:part-name "file"
                                :name      "sample.jpg"
                                :mime-type "application/jpeg"
                                :content   (clojure.java.io/file (io/resource "test/sample.jpg"))}
                   {:keys [status]} (test-upload (str "/file/upload_image") [upload-data])]
               status => 401))

       (apply login! (test-user-credentials test-user))

       (fact "file type must be image"
             (let [upload-data {:part-name "file"
                                :name "sample.doc"
                                :mime-type "application/msword"
                                :content (clojure.java.io/file (io/resource "test/sample.doc"))}
                   {:keys [status body]} (test-upload (str "/file/upload_image") [upload-data])]
               status => 200
               (:status body) => "error"
               (:message body) => (t :file/Errorwhileuploading)))

       (fact "user can upload an image"
             (let [upload-data {:part-name "file"
                                :name      "sample.jpg"
                                :mime-type "application/jpeg"
                                :content   (clojure.java.io/file (io/resource "test/sample.jpg"))}
                   {:keys [status body]} (test-upload (str "/file/upload_image") [upload-data])
                   {:keys [id name path mime_type size] :as upload-data} (:data body)]
               status => 200
               (:status body) => "success"
               (keys upload-data) => (just [:id :name :path :mime_type :size :ctime :mtime :tags] :in-any-order)
               id => 5
               name => "sample.jpg"
               path => "file/1/9/8/3/1983bd8127df711e199325d8459b4fe3827f04c6cb5c229393502726c01782e5.jpg"
               mime_type => "image/jpeg"
               size => 7507))

       (logout!))

(facts "about adding tags to file"
       (fact "user must be logged in to add tags"
             (:status (test-api-request :post (str "/file/save_tags/" file-id) file-tags)) => 401)

       (apply login! (test-user-credentials test-user))

       (fact "file-id must be integer"
             (let [{:keys [body status]} (test-api-request :post "/file/save_tags/not-an-integer" file-tags)]
               status => 400
               body => "{\"errors\":{\"fileid\":\"(not (instance? java.lang.Long \\\"not-an-integer\\\"))\"}}"))

       (fact "file must exist"
             (let [{:keys [body status]} (test-api-request :post (str "/file/save_tags/99") file-tags)]
               status => 200
               (:status body) => "error"))

       (fact "user must be the owner of the file"
             (let [{:keys [body status]} (test-api-request :post (str "/file/save_tags/" another-users-file) file-tags)]
               status => 200
               (:status body) => "error"))

       (fact "one or more tags must be set"
             (let [{:keys [body status]} (test-api-request :post (str "/file/save_tags/" file-id) {})]
               status => 400
               body => "{\"errors\":{\"tags\":\"missing-required-key\"}}")

             (let [{:keys [body status]} (test-api-request :post (str "/file/save_tags/" file-id) {:tags nil})]
               status => 400
               body => (contains "{\"errors\":"))

             (let [{:keys [body status]} (test-api-request :post (str "/file/save_tags/" file-id) {:tags []})]
               status => 400
               body => (contains "{\"errors\":")))

       (fact "user can add tags successfully"
             (let [{:keys [body status]} (test-api-request :post (str "/file/save_tags/" file-id) file-tags)]
               status => 200
               (:status body) "success"))

       (fact "recently added tags exist"
             (let [{:keys [body status]} (test-api-request :get "/file")
                   tags (->> body (filter #(= (:id %) file-id)) first :tags)]
               status => 200
               (:tags file-tags) => (just tags :in-any-order)))

       (logout!))

(facts "about deleting a file"
       (fact "user must be logged in to delete a file"
             (:status (test-api-request :delete (str "/file/" file-id))) => 401)

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
             (let [{:keys [body status]} (test-api-request :delete (str "/file/" another-users-file))]
               status => 200
               (:status body) => "error"
               (:message body) => (t :file/Errorwhiledeleting)))

       (fact "file instance can be deleted"
             (let [{:keys [body status]} (test-api-request :delete (str "/file/" file-id))]
               status => 200
               (:status body) => "success"
               (:message body) => (t :file/Filedeleted)))

       (fact "another file instance can be deleted"
             (let [{:keys [body status]} (test-api-request :delete "/file/2")]
               status => 200
               (:status body) => "success"
               (:message body) => (t :file/Filedeleted)))

       (logout!))

(migrator/reset-seeds (migrator/test-config))