(ns salava.file.routes
  (:require [compojure.api.sweet :refer :all]
            [salava.core.layout :as layout]))

(defroutes* route-def
  (context* "/pages" []
            (layout/main "/files")))
