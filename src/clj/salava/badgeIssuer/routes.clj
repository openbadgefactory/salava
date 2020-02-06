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

    (context "/obpv1/selfie" []
             :tags ["selfie"]

      (GET "/" []
           :summary "Get user selfie badges"
           :auth-rules access/authenticated
           :current-user current-user
           (ok {:badges (bdb/user-selfie-badges ctx (:id current-user))}))

      (GET "/_/assertion/:user-badge-id" []
           :summary "Get hosted badge assertion"
           :path-params [user-badge-id :- s/Int]
           (ok (bm/badge-assertion ctx user-badge-id)))

      (GET "/_/badge/:user-badge-id" []
            :summary "Get hosted badge information"
            :path-params [user-badge-id :- s/Int]
            (ok (bm/get-badge ctx user-badge-id)))

      (GET "/criteria/:id" []
            :summary "Get criteria information"
            :path-params [id :- s/Str]
            (ok (bm/badge-criteria ctx id)))

      (GET "/_/issuer/:id" []
            :summary "Get issuer information"
            :path-params [id :- s/Str]
            (ok (bm/badge-issuer ctx id)))

      (GET "/create" []
           :no-doc true
           :summary "Initialize badge creator"
           :auth-rules access/authenticated
           :current-user current-user
           (ok (bm/initialize ctx current-user)))

      (GET "/create/:id" []
            :summary "Edit selfie badge!"
            :path-params [id :- s/Str]
            :auth-rules access/authenticated
            :current-user current-user
            ;(bm/issue-selfie-badge ctx id (:id current-user) [12])
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
