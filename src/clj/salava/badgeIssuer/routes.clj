(ns salava.badgeIssuer.routes
  (:require
    [compojure.api.sweet :refer :all]
    [ring.util.http-response :refer :all]
    [ring.util.io :as io]
    [salava.badgeIssuer.creator :as bic]
    [salava.core.access :as access]
    [salava.core.layout :as layout]
    [schema.core :as s]
    salava.core.restructure))

(defn route-def [ctx]
  (routes
    (context "/issuer" []
              (layout/main ctx "/"))

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
           (ok (bic/generate-image ctx current-user))))))
