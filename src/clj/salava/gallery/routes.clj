(ns salava.gallery.routes
  (:require [compojure.api.sweet :refer :all]
            [salava.core.layout :as layout]))

(defroutes* route-def
  (context* "/gallery" []
            (layout/main "/")
            (layout/main "/badges")
            (layout/main "/pages")
            (layout/main "/profiles")
            (layout/main "/getbadge")))
