(ns salava.registerlink.routes
  (:require [compojure.api.sweet :refer :all]
            [ring.util.http-response :refer :all]
            [ring.util.response :refer [redirect]]
            [salava.core.layout :as layout]
            [schema.core :as s]
            [salava.core.util :refer [get-base-path]]
            [salava.core.helper :refer [dump]]
            [salava.core.access :as access]
            salava.core.restructure))


(defn route-def [ctx]
  (dump "JEE")
  (routes
   (context "/admin" []
            (layout/main ctx "/register-link"))
   (context "/user" []
            (layout/main ctx "/register/token/:token"))

   (context "/obpv1/registerlink" []
            :tags ["admin"]
            (GET "/register/:token" []
                 :summary "Get languages"
                 :path-params [token :- s/Str]
                 (dump token)
                 (if (= token "jeejooo")
                   (ok {:languages (get-in ctx [:config :core :languages])})
                   (forbidden)))
            
             )
   
    ))
