(ns salava.badge.routes
  (:require [compojure.api.sweet :refer :all]
            [salava.core.layout :as layout]))

(defroutes* route-def
  (context* "/badge" []
            (layout/main "/")
            (layout/main "/show/:id")
            (layout/main "/import/")
            (layout/main "/upload/")
            (layout/main "/stats/")))
