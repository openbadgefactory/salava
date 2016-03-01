(ns salava.core.routes
  (:require [compojure.api.sweet :refer :all]
            [ring.util.http-response :refer :all]
            [salava.core.layout :as layout]
            salava.core.restructure))

(defn route-def [ctx]
  (GET "/" []
       :current-user current-user
       (if current-user
         (temporary-redirect "/badge")
         (temporary-redirect "/user/login"))))
