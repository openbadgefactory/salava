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

             (PUT "/self" []
                  :summary "Set location for current user. Requires authenticated user."
                  :return ls/success
                  :body [body ls/lat-lng]
                  :auth-rules access/signed
                  :current-user current-user
                  (ok (l/set-user-location ctx (:id current-user) (:lat body) (:lng body))))

             (PUT "/self/public" []
                  :summary "Set public location status for current user. Requires authenticated user."
                  :return ls/success
                  :body-params [public :- s/Bool]
                  :auth-rules access/signed
                  :current-user current-user
                  (ok (l/set-location-public ctx (:id current-user) public)))

             (PUT "/self/reset" []
                  :summary "Remove locations from current user's profile and all badges. Requires authenticated user."
                  :return ls/success
                  :auth-rules access/signed
                  :current-user current-user
                  (ok (l/set-location-reset ctx (:id current-user))))

             (PUT "/user_badge/:badge" []
                  :summary "Set location for a single badge. Requires authenticated user."
                  :return ls/success
                  :path-params [badge :- s/Int]
                  :body [body ls/lat-lng]
                  :auth-rules access/signed
                  :current-user current-user
                  (ok (l/set-user-badge-location ctx (:id current-user) badge (:lat body) (:lng body))))


             (GET "/user_badge/:badge" []
                  :summary "Get location of a single badge."
                  :return ls/lat-lng
                  :path-params  [badge :- s/Int]
                  :current-user current-user
                  (ok (l/user-badge-location ctx (:id current-user) badge)))

             (GET "/user/:user" []
                  :summary "Get published location of a user."
                  :return ls/lat-lng
                  :path-params  [user :- s/Int]
                  :current-user current-user
                  (ok (l/user-enabled-location ctx user (some-> current-user :id pos?))))

             (GET "/self" []
                  :summary "Get location of current user. Requires authenticated user."
                  :return ls/self-location
                  :auth-rules access/signed
                  :current-user current-user
                  (ok (l/user-location ctx (:id current-user))))

             (GET "/explore/badge/:badge" []
                  :summary "Get single badge location for gallery. Requires authenticated user."
                  :return ls/explore-badges
                  :path-params  [badge :- s/Str]
                  :auth-rules access/signed
                  :current-user current-user
                  (ok (l/explore-badge ctx badge)))

             (GET "/explore/users" []
                  :summary "Get public user locations for gallery. Requires bounding map box (South-West and North-East coordinates). Results can be filtered by user name."
                  :return ls/explore-users
                  :query [params ls/explore-user-query]
                  :current-user current-user
                  (ok (l/explore-list-users ctx (some-> current-user :id pos?) params)))

             (GET "/explore/badges" []
                  :summary "Get public badge locations for gallery. Requires bounding map box (South-West and North-East coordinates). Results can be filtered by badge or issuer name and tags."
                  :return ls/explore-badges-ex
                  :query [params ls/explore-badge-query]
                  :current-user current-user
                  (ok (l/explore-list-badges ctx (some-> current-user :id pos?) params)))

             (GET "/explore/filters" []
                  :summary "Get list of tags, badge and issuer names available for public badges."
                  :return ls/explore-filters
                  :current-user current-user
                  (ok (l/explore-filters ctx (some-> current-user :id pos?))))
             )))
