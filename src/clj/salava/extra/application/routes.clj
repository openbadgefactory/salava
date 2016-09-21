(ns salava.extra.application.routes
  (:require [compojure.api.sweet :refer :all]
            [ring.util.http-response :refer :all]
            [schema.core :as s]
            [salava.core.helper :refer [dump]]
            [salava.core.layout :as layout]
            [clojure.string :refer [split]]))

(defn route-def [ctx]
  (routes
    (context "/gallery" []
             (layout/main ctx "/application"))

    (context "/obpv1/application" []
             :tags ["application"]
             
             (GET "/" []
                  :return [{:iframe s/Str :language s/Str}]
                  :summary "Get public badge data"
                  :current-user current-user
                  (let [applications (get-in ctx [:config :extra/application])]
                    (ok applications))))))
