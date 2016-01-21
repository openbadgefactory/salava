(ns salava.user.routes
  (:require [compojure.api.sweet :refer :all]
            [salava.core.layout :as layout]))

(defn route-def [ctx]
  (routes
    (context "/user" []
             (layout/main ctx "/login")
             (layout/main ctx "/account"))))
