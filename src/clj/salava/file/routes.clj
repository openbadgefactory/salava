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
            [salava.core.schema-helper :as h]
            [clojure.java.io :as io]
            [pantomime.mime :refer [mime-type-of]]
            salava.core.restructure))

(defn route-def [ctx]
  (routes
    (context "/page" []
             (layout/main ctx "/files"))

    (context "/file" []
             (layout/main ctx "/upload")
             (layout/main ctx "/browser/:editor/:callback/:lang")

             (GET "/browser" []
                  :summary "View user files"
                  :query-params [CKEditor :- String
                                 CKEditorFuncNum :- String
                                 langCode :- String]
                  (temporary-redirect (str "/file/browser/" CKEditor"/" CKEditorFuncNum"/" langCode)))

             (GET "/:folder1/:folder2/:folder3/:folder4/:filename" []
                  :path-params [folder1 :- (s/constrained s/Str #(and (= (count %) 1) (h/letters-nums? %)))
                                folder2 :- (s/constrained s/Str #(and (= (count %) 1) (h/letters-nums? %)))
                                folder3 :- (s/constrained s/Str #(and (= (count %) 1) (h/letters-nums? %)))
                                folder4 :- (s/constrained s/Str #(and (= (count %) 1) (h/letters-nums? %)))
                                filename :- (s/constrained s/Str #(and (string? %) (re-matches #"(\w+)(\.\w+)?" %)))]
                  (let [path (str "file/" folder1 "/" folder2 "/" folder3 "/"folder4 "/" filename)
                        data-dir (get-in ctx [:config :core :data-dir])
                        full-path (-> (str data-dir "/" path) java.net.URI. (.normalize) (.getPath))
                        file (io/as-file full-path)]
                    (if (and (re-find (re-pattern (str "^" data-dir)) full-path) (.exists file) (.canWrite file))
                      (let [mime-type (mime-type-of file)]
                        (-> full-path
                            file-response
                            (content-type mime-type)))
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
                   :return {:status (s/enum "success" "error")}
                   :path-params [fileid :- Long]
                   :body-params [tags :- (s/both [s/Str] (s/pred seq))]
                   :summary "Save file tags"
                   :auth-rules access/authenticated
                   :current-user current-user
                   (ok (f/save-file-tags! ctx fileid (:id current-user) tags))))))