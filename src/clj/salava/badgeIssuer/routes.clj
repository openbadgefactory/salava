(ns salava.badgeIssuer.routes
  (:require
    [compojure.api.sweet :refer :all]
    [ring.swagger.upload :as upload]
    [ring.util.http-response :refer :all]
    [ring.util.io :as io]
    [salava.badgeIssuer.creator :as bic]
    [salava.badgeIssuer.upload :as biu]
    [salava.core.access :as access]
    [salava.core.layout :as layout]
    [schema.core :as s]
    salava.core.restructure))

(defn route-def [ctx]
  (routes
    (context "/badge" []
              (layout/main ctx "/issuer"))

    (context "/obpv1/issuer" []
             :tags ["badge issuer"]

      (GET "/" []
           :no-doc true
           :summary "Initialize badge creator"
           :auth-rules access/authenticated
           :current-user current-user
           (ok (bic/initialize ctx current-user)))

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
