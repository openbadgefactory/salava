(ns salava.user.routes
  (:require [compojure.api.sweet :refer :all]
            [salava.core.layout :as layout]))

(defroutes* route-def
  (context* "/user" []
            (layout/main "/login/")
            (layout/main "/account/")))
