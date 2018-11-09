(ns salava.metabadge.routes
  (:require [compojure.api.sweet :refer :all]
            [ring.util.http-response :refer :all]
            [salava.metabadge.metabadge :as mb]
            salava.core.restructure))

(defn route-def [ctx]
  (routes
    (context "/obpv1/metabadge" []
             :tags ["metabadge"]
             (GET "/info" [assertion_url]
                  :summary "get metabadge info via assertion url"
                  :current-user current-user
                  (ok (mb/get-data ctx assertion_url))))))
