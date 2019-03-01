(ns salava.connections.routes
  (:require [compojure.api.sweet :refer :all]
            [ring.util.http-response :refer :all]
            [salava.core.access :as access]
            [salava.core.layout :as layout]
            [salava.social.db :as so]
            salava.core.restructure))

(defn route-def [ctx]
  (routes
    (context "/connections" []
             (layout/main ctx "/")
             (layout/main ctx "/connections")
             (layout/main ctx "/badge")
             (layout/main ctx "/stream")
             (layout/main ctx "/stats")
             (layout/main ctx "/endorsement"))
    (context "/obpv1/connections" []
             :tags ["connections"]
             (GET "/connections_badge" []
                  :summary "Return users all badge connections"
                  :auth-rules access/signed
                  :current-user current-user
                  (ok (so/get-connections-badge ctx (:id current-user))))

             (GET "/connections_issuer" []
                  :summary "Return all user issuer connection"
                  :auth-rules access/authenticated
                  :current-user current-user
                  (ok (so/get-user-issuer-connections ctx (:id current-user)))))))
