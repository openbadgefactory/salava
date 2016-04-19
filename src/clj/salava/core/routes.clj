(ns salava.core.routes
  (:require [compojure.api.sweet :refer :all]
            [ring.util.http-response :refer :all]
            [salava.core.layout :as layout]
            [salava.core.util :refer [get-base-path]]
            salava.core.restructure))

(defn route-def [ctx]
  (GET "/" []
       :current-user current-user
       (if current-user
         (temporary-redirect (str (get-base-path ctx) "/badge"))
         (temporary-redirect (str (get-base-path ctx) "/user/login")))))
