(ns salava.file.routes
  (:require [compojure.api.sweet :refer :all]
            [ring.util.http-response :refer :all]
            [ring.swagger.upload :as upload]
            [salava.core.layout :as layout]
            [schema.core :as s]
            [salava.file.db :as f]
            [salava.file.upload :as u]
            [salava.file.schemas :as schemas]
            [salava.core.access :as access]
            [clojure.java.io :as io]
            [pantomime.mime :refer [mime-type-of]]
            salava.core.restructure))

(defn route-def [ctx]
  (routes
    (context "/page" []
             (layout/main ctx "/files"))

    (context "/file" []
             (layout/main ctx "/upload")
             (GET "/:folder1/:folder2/:folder3/:folder4/:filename" []
                  :path-params [folder1 :- s/Str, folder2 :- s/Str, folder3 :- s/Str, folder4 :- s/Str, filename :- s/Str]
                  (let [path (str "file/" folder1 "/" folder2 "/" folder3 "/"folder4 "/" filename)
                        mime-type (mime-type-of path)
                        data-dir (get-in ctx [:config :core :data-dir])
                        full-path (str data-dir "/" path)]
                    (if (.exists (io/as-file full-path))
                      (-> full-path
                          file-response
                          (content-type mime-type))
                      (not-found nil)))))

    (context "/obpv1/file" []
             :tags ["file"]
             (GET "/" []
                  :return [schemas/File]
                  :summary "Get user's all files"
                  :auth-rules access/authenticated
                  :current-user current-user
                  (ok (f/user-files-all ctx (:id current-user))))

             (DELETE "/:fileid" []
                     :path-params [fileid :- Long]
                     :summary "Delete file by id"
                     :auth-rules access/authenticated
                     :current-user current-user
                     (ok (f/remove-user-file! ctx fileid (:id current-user))))

             (POST "/upload" []
                   :return schemas/Upload
                   :multipart-params [file :- upload/TempFileUpload]
                   :middleware [upload/wrap-multipart-params]
                   :summary "Reveive file upload"
                   :auth-rules access/authenticated
                   :current-user current-user
                   (ok (u/upload-file ctx (:id current-user) file false)))

             (POST "/upload_image" []
                   :return schemas/Upload
                   :multipart-params [file :- upload/TempFileUpload]
                   :middleware [upload/wrap-multipart-params]
                   :summary "Reveive image upload"
                   :auth-rules access/authenticated
                   :current-user current-user
                   (ok (u/upload-file ctx (:id current-user) file true)))

             (POST "/save_tags/:fileid" []
                   :path-params [fileid :- Long]
                   :body-params [tags :- [s/Str]]
                   :summary "Save file tags"
                   :auth-rules access/authenticated
                   :current-user current-user
                   (ok (str (f/save-file-tags! ctx fileid (:id current-user) tags)))))))