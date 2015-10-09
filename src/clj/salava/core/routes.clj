(ns salava.core.routes
  (:require [compojure.api.sweet :refer :all]
            [compojure.route :as route]
            [salava.core.layout :as layout]))

(defroutes* route-def
  (layout/main "/")
  (route/not-found "404 Not found"))
