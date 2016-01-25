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
            salava.core.restructure))

(defn route-def [ctx]
  (routes
    (context "/page" []
             (layout/main ctx "/files"))

    (context "/file" []
             (layout/main ctx "/upload"))

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
                     (ok (f/remove-file! ctx fileid (:id current-user))))

             (POST "/upload" []
                   :return schemas/Upload
                   :multipart-params [file :- upload/TempFileUpload]
                   :middleware [upload/wrap-multipart-params]
                   :summary "Reveive file upload"
                   :auth-rules access/authenticated
                   :current-user current-user
                   (ok (u/upload-file ctx (:id current-user) file)))

             (POST "/save_tags/:fileid" []
                   :path-params [fileid :- Long]
                   :body-params [tags :- [s/Str]]
                   :summary "Save file tags"
                   :auth-rules access/authenticated
                   :current-user current-user
                   (ok (str (f/save-file-tags! ctx fileid (:id current-user) tags)))))))
