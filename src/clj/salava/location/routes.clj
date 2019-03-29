(ns salava.location.routes
  (:require [clojure.tools.logging :as log]
            [clojure.pprint :refer [pprint]]
            [compojure.api.sweet :refer :all]
            [ring.util.http-response :refer :all]
            [ring.util.response :refer [redirect]]
            [schema.core :as s]
            [salava.core.layout :as layout]
            [salava.core.access :as access]
            [salava.core.util :as u]
            [salava.location.db :as l]
            [salava.location.schemas :as ls]
            ))

(defn route-def [ctx]
  (routes
    (context "/gallery" []
             (layout/main ctx "/map"))

    (context "/obpv1/location" []
             :tags ["location"]

             (POST "/user_badge/:badge" [badge]
                   :summary "Set location for a single badge"
                   :body-params [lat :- (s/maybe s/Num)
                                 lng :- (s/maybe s/Num)]
                   :auth-rules access/signed
                   :current-user current-user
                   (ok (l/set-user-badge-location ctx (:id current-user) badge lat lng)))

             (POST "/user" []
                   :summary "Set location for current user"
                   :body-params [lat :- (s/maybe s/Num)
                                 lng :- (s/maybe s/Num)]
                   :auth-rules access/signed
                   :current-user current-user
                   (ok (l/set-user-location ctx (:id current-user) lat lng)))

             (POST "/public" []
                   :summary "Set public location status for current user"
                   :body-params [public :- s/Bool]
                   :auth-rules access/signed
                   :current-user current-user
                   (ok (l/set-location-public ctx (:id current-user) public)))


             (GET "/user_badge/:badge" [badge]
                   :summary "Get location of a single badge"
                   :auth-rules access/signed
                   :current-user current-user
                  (ok (l/user-badge-location ctx (:id current-user) badge)))

             (GET "/user/:user" [user]
                   :summary "Get published location of a user"
                   :current-user current-user
                  (ok (l/user-enabled-location ctx user (some-> current-user :id pos?))))

             (GET "/user" []
                   :summary "Get location of current user"
                   :auth-rules access/signed
                   :current-user current-user
                  (ok (l/user-location ctx (:id current-user))))

             (GET "/explore/badge/:badge" [badge]
                   :summary "Get single badge location for gallery"
                   :auth-rules access/signed
                   :current-user current-user
                  (ok (l/explore-badge ctx badge)))

             (GET "/explore/:kind" req
                  :coercion :schema
                  :summary "Get public locations for gallery"
                  :path-params  [kind :- s/Str]
                  :query-params [max_lat :- ls/Lat
                                 max_lng :- ls/Lng
                                 min_lat :- ls/Lat
                                 min_lng :- ls/Lng]
                  :current-user current-user
                  (ok (l/explore-list ctx kind (pos? (:id current-user)) (:params req))))
             )))
