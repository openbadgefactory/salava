(ns salava.core.routes
  (:require [compojure.api.sweet :refer :all]
            [ring.util.http-response :refer :all]
            [salava.core.layout :as layout]))

;(macroexpand  '(layout/main "/"))

(defroutes* route-def
  (layout/main "/")
  ;(GET* "/" [] :components [context] (ok context))
  (ANY* "*" [] (not-found "404 Not found")))
