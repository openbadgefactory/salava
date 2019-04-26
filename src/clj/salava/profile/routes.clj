(ns salava.profile.routes
  (:require [compojure.api.sweet :refer :all]
            [ring.util.http-response :refer :all]
            [salava.core.layout :as layout]
            [salava.core.util :refer [get-base-path]]
            [schema.core :as s]
            [salava.profile.main :as profile]
            salava.core.restructure))

(defn route-def [ctx]
  (routes
    (context "/profile" []
             (layout/main-meta ctx "/:id" :user)
             (layout/main-meta ctx "/:id/embed" :user)
             )
    (context "/obpv1/profile" []
             :tags ["profile"]
             (GET "/:userid" []
                  :summary "Get user information and profile fields"
                  :path-params [userid :- s/Int]
                  :current-user current-user
                  (ok (profile/user-information-and-profile ctx userid (:id current-user)))))))
