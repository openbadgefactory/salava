(ns salava.social.routes
  (:require [compojure.api.sweet :refer :all]
            [ring.util.http-response :refer :all]
            [salava.core.layout :as layout]
            [schema.core :as s]
            [salava.core.access :as access]
            salava.core.restructure))




(defn route-def [ctx]
  (routes
    (context "/social" []
             (layout/main ctx "/"))

    (context "/obpv1/social" []
             :tags ["social"]
             (GET "/" []
                  :return (s/enum "success" "error")
                  :summary ""
                  :auth-rules access/authenticated
                  :current-user current-user
                  (ok "success"))
             
             )))
