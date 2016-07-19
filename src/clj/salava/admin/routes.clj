(ns salava.admin.routes
  (:require [compojure.api.sweet :refer :all]
            [ring.util.http-response :refer :all]
            [ring.util.response :refer [redirect]]
            [salava.core.layout :as layout]
            [schema.core :as s]
            [salava.core.util :refer [get-base-path]]
            [salava.admin.db :as a]
            [salava.core.access :as access]
            [salava.admin.schemas :as schemas]
            salava.core.restructure))


(defn route-def [ctx]
  (routes
    (context "/admin" []
             (layout/main ctx "/")
             (layout/main ctx "/tickets")
             (layout/main ctx "/statistics"))

    (context "/obpv1/admin" []
             :tags ["admin"]

             (GET "/stats" []
                   :return schemas/Stats
                   :summary "Get statistics"
                   :auth-rules access/admin
                   :current-user current-user
                   (do
                     (ok (a/get-stats ctx))))

             (POST "/private_badge/:id" []
                   :return (s/enum "success" "error")
                   :path-params [id :- s/Int]
                   :summary "Set badge to private"
                   :auth-rules access/admin
                   :current-user current-user
                   (ok (a/private-badge! ctx id)))

             (POST "/private_badges/:id" []
                   :return (s/enum "success" "error")
                   :path-params [id :- s/Str]
                   :summary "Set badges from gallery to private"
                   :auth-rules access/admin
                   :current-user current-user
                   (ok (a/private-badges! ctx id)))
             
             (POST "/private_page/:id" []
                   :return (s/enum "success" "error")
                   :path-params [id :- s/Int]
                   :summary "Set page to private"
                   :auth-rules access/admin
                   :current-user current-user
                   (ok (a/private-page! ctx id)))
             
             (POST "/private_user/:id" []
                   :return (s/enum "success" "error")
                   :path-params [id :- s/Int]
                   :summary "Set user to private"
                   :auth-rules access/admin
                   :current-user current-user
                   (ok (a/private-user! ctx id)))
             
             )))
