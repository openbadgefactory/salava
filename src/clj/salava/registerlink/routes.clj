(ns salava.registerlink.routes
  (:require [compojure.api.sweet :refer :all]
            [ring.util.http-response :refer :all]
            [ring.util.response :refer [redirect]]
            [salava.core.layout :as layout]
            [schema.core :as s]
            [salava.core.util :refer [get-base-path]]
            [salava.core.helper :refer [dump]]
            [salava.core.access :as access]
            [salava.registerlink.db :as rl]
            salava.core.restructure))


(defn route-def [ctx]
  (routes
   (context "/admin" []
            (layout/main ctx "/registerlink"))
   (context "/user" []
            (layout/main ctx "/register/token/:token"))

   (context "/obpv1/registerlink" []
            :tags ["registerlink"]
            (GET "/register/:token" []
                 :summary "Get languages if token is same as saved token"
                 :path-params [token :- s/Str]
                 (let [active-token (rl/get-token-active ctx)]
                   (if (and (:active active-token) (= (:token active-token) token))
                     (ok {:languages (get-in ctx [:config :core :languages])})
                     (forbidden))))

            (POST "/register-token/:token" []
                   :return (s/enum "success" "error")
                   :path-params [token :- s/Str]
                   :summary "Set url-token"
                   :auth-rules access/admin
                   :current-user current-user
                   (ok (rl/create-register-token! ctx token)))

            (POST "/register-active" []
                   :return (s/enum "success" "error")
                   :summary "Set url-token"
                   :body [content {:active s/Bool}]
                   :auth-rules access/admin
                   :current-user current-user
                   (ok (rl/create-register-active! ctx (:active content))))
            
            (GET "/register-token" []
                 :return {:token (s/maybe s/Str)
                          :active (s/maybe s/Bool)}
                 :summary "get url-token"
                 :auth-rules access/admin
                 :current-user current-user
                 (do
                   (ok (rl/get-token-active ctx)))))
   
    ))
