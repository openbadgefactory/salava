(ns salava.mobile.routes
  (:require [clojure.tools.logging :as log]
            [compojure.api.sweet :refer :all]
            [ring.util.http-response :refer :all]
            [ring.util.response :refer [redirect]]
            [schema.core :as s]
            [salava.mobile.schemas :as schemas] ;cljc
            [salava.mobile.db :as db]
            [salava.core.access :as access]
            [salava.core.middleware :as mw]
            [salava.core.util :as u]
            salava.core.restructure))



(defn route-def [ctx]
  (routes
    (context "/obpv1/mobile" []
             :tags ["mobile"]

             (GET "/badge" []
                  :return schemas/user-badges-m
                  :summary "Get the list badges for logged-in user"
                  :auth-rules access/signed
                  :current-user current-user
                  (ok (db/user-badges-all ctx (:id current-user))))

             (GET "/badge/:user-badge-id" []
                  :return schemas/user-badge-content-m
                  :path-params [user-badge-id :- Long]
                  :summary "Get badge content"
                  :auth-rules access/signed
                  :current-user current-user
                  (ok (db/user-badge ctx user-badge-id (:id current-user))))


             (GET "/pending_badges_first" []
                  :summary "Check and return first of user's pending badges"
                  :return schemas/pending-badges-first-m
                  :auth-rules access/signed
                  :current-user current-user
                  (ok (db/pending-badges-first ctx (:id current-user))))


             (GET "/badge/:user-badge-id/endorsements" []
                  :return schemas/user-badge-endorsements-m
                  :path-params [user-badge-id :- Long]
                  :summary "Get all badge endorsements"
                  :auth-rules access/signed
                  :current-user current-user
                  (ok (db/user-badge-endorsements ctx user-badge-id (:id current-user))))


             (GET "/badge/:user-badge-id/congratulations" []
                  :return schemas/congratulations-m
                  :path-params [user-badge-id :- Long]
                  :summary "badge congratulations"
                  :auth-rules access/signed
                  :current-user current-user
                  (ok (db/user-badge-congratulations ctx user-badge-id (:id current-user))))


             (GET "/gallery/badge/:gallery-id/:badge-id" []
                  :return schemas/gallery-badge-m
                  :path-params [gallery-id :- s/Int
                                badge-id :- s/Str]
                  :summary "Get gallery badge content"
                  :auth-rules access/signed
                  :current-user current-user
                  (ok (db/gallery-badge ctx gallery-id badge-id)))



             )))
