(ns salava.file.routes
  (:require [compojure.api.sweet :refer :all]
            [ring.util.http-response :refer :all]
            [ring.swagger.upload :as upload]
            [salava.core.layout :as layout]
            [schema.core :as s]
            [salava.file.db :as f]
            [salava.file.upload :as u]
            [salava.file.schemas :as schemas]
            [salava.core.util :refer [get-base-path]]
            [salava.core.access :as access]
            [salava.core.schema-helper :as h]
            [clojure.java.io :as io]
            [pantomime.mime :refer [mime-type-of]]
            salava.core.restructure))

(defn route-def [ctx]
  (routes
    #_(context "/page" []
             (layout/main ctx "/files"))

    (context "/file" []
             (layout/main ctx "/upload")
             (layout/main ctx "/browser/:editor/:callback/:lang")

             (GET "/browser" []
                  :summary "View user files"
                  :query-params [CKEditor :- String
                                 CKEditorFuncNum :- String
                                 langCode :- String]
                  (temporary-redirect (str (get-base-path ctx) "/file/browser/" CKEditor"/" CKEditorFuncNum"/" langCode)))

             )

    (context "/obpv1/file" []
             :tags ["file"]
             (GET "/" []
                  :return {:files [schemas/File]
                           :max-size (s/maybe s/Str) }
                  :summary "Get user's all files"
                  :auth-rules access/signed
                  :current-user current-user
                  (ok (f/user-files-all ctx (:id current-user))))

             (GET "/as-png" []
                  :query-params [image :- s/Str]
                  :summary "Convert svg image to png"
                  :auth-rules access/authenticated
                  :current-user current-user
                  (let [bytes (f/svg->png ctx image)]
                    (if bytes
                      {:status 200
                       :headers {"Content-Type" "image/png"}
                       :body bytes}
                      (not-found))))


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
                   :return {:status (s/enum "success" "error")}
                   :path-params [fileid :- Long]
                   :body-params [tags :- (s/both [s/Str] (s/pred seq))]
                   :summary "Save file tags"
                   :auth-rules access/authenticated
                   :current-user current-user
                   (ok (f/save-file-tags! ctx fileid (:id current-user) tags))))))
