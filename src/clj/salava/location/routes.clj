(ns salava.location.routes
  (:require [clojure.tools.logging :as log]
            [compojure.api.sweet :refer :all]
            [ring.util.http-response :refer :all]
            [ring.util.response :refer [redirect]]
            [schema.core :as s]
            [salava.core.layout :as layout]
            [salava.core.util :as u]
            [salava.location.db :as l]))

(defn route-def [ctx]
  (routes
    (context "/gallery" []
             (layout/main ctx "/map"))

    (context "/obpv1/location" []
             :tags ["location"]

             (POST "/user_badge/:badge" [badge]
                   :summary "Set location of a single badge"
                   :body-params [lat :- s/Num
                                 lng :- s/Num]
                   :current-user current-user
                   (ok (l/set-user-badge-location ctx (:id current-user) badge lat lng)))

             (POST "/user" []
                   :summary "Set location of current user"
                   :body-params [lat :- s/Num
                                 lng :- s/Num]
                   :current-user current-user
                   (ok (l/set-user-location ctx (:id current-user) lat lng)))

             (POST "/public" []
                   :summary "Set location of current user"
                   :body-params [public :- s/Bool]
                   :current-user current-user
                   (ok (l/set-location-public ctx (:id current-user) public)))


             (GET "/user_badge/:badge" [badge]
                   :summary "Get location of a single badge"
                   :current-user current-user
                  (ok (l/user-badge-location ctx (:id current-user) badge)))

             (GET "/user" []
                   :summary "Get location of current user"
                   :current-user current-user
                  (ok (l/user-location ctx (:id current-user))))

             (GET "/explore" []
                   :summary "Get badge and user locations for gallery"
                   :current-user current-user
                  (ok (l/explore-list ctx (:id current-user))))
             )))
