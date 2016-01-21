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
             (GET "/:userid" []
                  :return [schemas/File]
                  :path-params [userid :- Long]
                  :summary "Get user's all files"
                  :auth-rules access/authenticated
                  (ok (f/user-files-all ctx userid)))

             (DELETE "/:fileid" []
                     :path-params [fileid :- Long]
                     :summary "Delete file by id"
                     :auth-rules access/authenticated
                     (ok (f/remove-file! ctx fileid)))

             (POST "/upload/:userid" []
                   :return schemas/Upload
                   :path-params [userid :- Long]
                   :multipart-params [file :- upload/TempFileUpload]
                   :middlewares [upload/wrap-multipart-params]
                   :summary "Upload badge PNG-file"
                   :auth-rules access/authenticated
                   (ok (u/upload-file ctx userid file)))

             (POST "/save_tags/:fileid" []
                   :path-params [fileid :- Long]
                   :body-params [tags :- [s/Str]]
                   :summary "Save file tags"
                   :auth-rules access/authenticated
                   (ok (str (f/save-file-tags! ctx fileid tags)))))))
