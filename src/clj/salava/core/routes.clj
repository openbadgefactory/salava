(ns salava.core.routes
  (:require [compojure.api.sweet :refer :all]
            [salava.core.layout :as layout]
            [ring.util.response :as r]))


(def not-found (constantly (-> (r/response "404 Not Found")
                               (r/status 404)
                               (r/header "Content-type" "text/plain; charset=\"UTF-8\""))))


(defroutes* route-def
  (layout/main "/")
  (ANY* "*" [] not-found))
