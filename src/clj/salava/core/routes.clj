(ns salava.core.routes
  (:require [compojure.api.sweet :refer :all]
            [ring.util.http-response :refer :all]
            [salava.core.layout :as layout]
            [salava.badge.main :as b]
            [schema.core :as s]
            [salava.core.util :refer [get-base-path]]
            salava.core.restructure))

(defn route-def [ctx]
  (GET "/" []
       :current-user current-user
       (if current-user
         (temporary-redirect (str (get-base-path ctx) "/badge"))
         (temporary-redirect (str (get-base-path ctx) "/user/login")))))

(defn legacy-routes [ctx]
  (routes
    (GET "/badges/badge_info/:oldid/:userid" []
         :path-params [oldid :- s/Int
                       userid :- s/Int]
         (let [new-badge-id (b/old-id->id ctx oldid userid)]
           (if new-badge-id
             (temporary-redirect (str (get-base-path ctx) "/badge/info/" new-badge-id))
             (not-found))))

    (GET "/pages/:pageid/view" []
      :path-params [pageid :- s/Int]
      (temporary-redirect (str (get-base-path ctx) "/page/view/" pageid)))

    (GET "/user/:userid" []
      :path-params [userid :- s/Int]
      (temporary-redirect (str (get-base-path ctx) "/user/profile/" userid)))))