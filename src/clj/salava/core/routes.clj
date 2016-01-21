(ns salava.core.routes
  (:require [compojure.api.sweet :refer :all]
            [compojure.route :as route]
            [salava.core.layout :as layout]))

(defn route-def [ctx]
  (layout/main ctx "/")
  (route/not-found "404 Not found"))
