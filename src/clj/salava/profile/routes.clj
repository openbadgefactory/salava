(ns salava.profile.routes
  (:require [compojure.api.sweet :refer :all]
            [ring.util.http-response :refer :all]
            [salava.core.layout :as layout]
            salava.core.restructure))

(defn route-def [ctx]
  (routes
    (context "/profile" []
             (layout/main-meta ctx "/:id" :user)
             (layout/main-meta ctx "/:id/embed" :user)
             )))
