(ns salava.badgeIssuer.routes
  (:require
    [compojure.api.sweet :refer :all]
    [ring.swagger.upload :as upload]
    [ring.util.http-response :refer :all]
    [ring.util.io :as io]
    [salava.badgeIssuer.creator :as bic]
    [salava.badgeIssuer.db :as bdb]
    [salava.badgeIssuer.schemas :as schemas]
    [salava.badgeIssuer.main :as bm]
    [salava.badgeIssuer.upload :as biu]
    [salava.core.access :as access]
    [salava.core.layout :as layout]
    [schema.core :as s]
    salava.core.restructure))

(defn route-def [ctx]
  (routes
    (context "/selfie" []
              (layout/main ctx "/criteria/:id"))

    (context "/badge/selfie" []
              (layout/main ctx "/")
              (layout/main ctx "/criteria/:id")
              (layout/main ctx "/create")
              (layout/main ctx "/create/:id"))

    (context "/obpv1/selfie/_" []
             :tags ["hosted_badge"]

      (GET "/assertion/:user-badge-id" []
           :return schemas/assertion
           :summary "Get hosted badge assertion"
           :path-params [user-badge-id :- s/Int]
           (ok (bm/badge-assertion ctx user-badge-id)))

      (GET "/badge/:user-badge-id" [i]
            :return schemas/badge
            :summary "Get hosted badge information"
            :path-params [user-badge-id :- s/Int]
            :query-params [i :- s/Int]
            (ok (bm/get-badge ctx user-badge-id i)))

      (GET "/issuer" []
            :return schemas/issuer
            :summary "Get issuer information"
            :query-params [cid :- s/Str
                           uid :- s/Int]
            (ok (bm/badge-issuer ctx cid uid))))

    (context "/obpv1/selfie" []
             :tags ["selfie"]
             ;:no-doc true

      (GET "/" []
           :summary "Get user selfie badges"
           :auth-rules access/authenticated
           :current-user current-user
           (ok {:badges (bdb/user-selfie-badges ctx (:id current-user))}))

      (GET "/criteria/:id" []
            :summary "Get criteria information"
            :path-params [id :- s/Str]
            (ok (bm/badge-criteria ctx id)))

      (GET "/create" []
           :no-doc true
           ;:return schemas/initialize-badge
           :summary "Initialize badge creator"
           :auth-rules access/authenticated
           :current-user current-user
           (ok (bm/initialize ctx current-user)))

      (GET "/create/:id" []
            :return schemas/initialize-badge
            :summary "Edit selfie badge!"
            :path-params [id :- s/Str]
            :auth-rules access/authenticated
            :current-user current-user
            (ok (bm/initialize ctx current-user id)))

      (POST "/create" []
            :return {:status (s/enum "success" "error")
                     :id (s/maybe s/Str)
                     (s/optional-key :message) (s/maybe s/Str)}
            :body [data schemas/save-selfie-badge]
            :summary "Create new selfie badge"
            :auth-rules access/authenticated
            :current-user current-user
            (ok (bm/save-selfie-badge ctx data (:id current-user))))

      (POST "/issue" []
             :return {:status (s/enum "success" "error")}
             :body [data schemas/issue-selfie-badge]
             :summary "Issue selfie badge"
             :auth-rules access/authenticated
             :current-user current-user
             (ok (bm/issue-selfie-badge ctx data (:id current-user))))

      (POST "/revoke/:user-badge-id" []
             :return {:status (s/enum "success" "error") (s/optional-key :message) (s/maybe s/Str)}
             :summary "Revoke issued selfie badge"
             :path-params [user-badge-id :- s/Int]
             :auth-rules access/authenticated
             :current-user current-user
             (ok (bm/revoke-selfie-badge! ctx user-badge-id (:id current-user))))

      (GET "/history/:id" []
             :summary "Get selfie badge issuing history"
             :path-params [id :- s/Str]
             :auth-rules access/authenticated
             :current-user current-user
             (ok (bm/issuing-history ctx id (:id current-user))))

      (DELETE "/:id" []
              :return {:status (s/enum "success" "error")}
              :path-params [id :- s/Str]
              :summary "Delete selfie badge"
              :auth-rules access/authenticated
              :current-user current-user
              (ok (bdb/delete-selfie-badge-soft ctx (:id current-user) id)))

      (GET "/generate_image" []
           :return {:status (s/enum "success" "error") :url s/Str (s/optional-key :message) (s/maybe s/Str)}
           :summary "Generate random badge image"
           :auth-rules access/signed
           :current-user current-user
           (ok (bic/generate-image ctx current-user)))

      (POST "/upload_image" []
           :return {:status (s/enum "success" "error") :url s/Str (s/optional-key :message) (s/maybe s/Str)}
           :multipart-params [file :- upload/TempFileUpload]
           :middleware [upload/wrap-multipart-params]
           :summary "Upload badge image (PNG)"
           :auth-rules access/authenticated
           :current-user current-user
           (ok (biu/upload-image ctx current-user file))))))
