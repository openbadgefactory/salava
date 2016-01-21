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

(defroutes* route-def
  (context* "/page" []
            (layout/main "/files"))
  (context* "/file" []
            (layout/main "/upload"))

  (context* "/obpv1/file" []
            :tags ["file"]
            (GET* "/:userid" []
                  :return [schemas/File]
                  :path-params [userid :- Long]
                  :summary "Get user's all files"
                  :components [context]
                  :auth-rules access/authenticated
                  (ok (f/user-files-all context userid)))

            (DELETE* "/:fileid" []
                     :path-params [fileid :- Long]
                     :summary "Delete file by id"
                     :components [context]
                     :auth-rules access/authenticated
                     (ok (f/remove-file! context fileid)))

            (POST* "/upload/:userid" []
                   :return schemas/Upload
                   :path-params [userid :- Long]
                   :multipart-params [file :- upload/TempFileUpload]
                   :middlewares [upload/wrap-multipart-params]
                   :summary "Upload badge PNG-file"
                   :components [context]
                   :auth-rules access/authenticated
                   (ok (u/upload-file context userid file)))

            (POST* "/save_tags/:fileid" []
                   :path-params [fileid :- Long]
                   :body-params [tags :- [s/Str]]
                   :summary "Save file tags"
                   :components [context]
                   :auth-rules access/authenticated
                   (ok (str (f/save-file-tags! context fileid tags))))))