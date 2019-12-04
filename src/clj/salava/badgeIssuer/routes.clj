(ns salava.badgeIssuer.routes
  (:require
    [compojure.api.sweet :refer :all]
    [ring.util.http-response :refer :all]
    [ring.util.io :as io]
    [salava.badgeIssuer.creator :as bic]
    [salava.core.access :as access]
    [schema.core :as s]
    salava.core.restructure))

(defn route-def [ctx]
  (routes
    (context "/obpv1/badgeIssuer" []
             :tags ["badge issuer"]

      (GET "/generate_image" []
           :return s/Str
           :summary "Generate random badge image"
           :auth-rules access/signed
           :current-user current-user
           (ok (bic/generate-image ctx current-user))))))
