(ns salava.extra.application.routes
  (:require [compojure.api.sweet :refer :all]
            [ring.util.http-response :refer :all]
            [schema.core :as s]
            [salava.core.helper :refer [dump]]
            [salava.core.layout :as layout]
            [salava.extra.application.db :as a]
            [clojure.string :refer [split]]))

(defn route-def [ctx]
  (routes
    (context "/gallery" []
             (layout/main ctx "/application"))

    (context "/obpv1/application" []
             :tags ["application"]
             
             (GET "/" [country]
                  ;:return [{:iframe s/Str :language s/Str}]
                  :summary "Get public badge data"
                  :current-user current-user
                  (let [applications (a/get-badge-adverts ctx)
                        countries (a/badge-adverts-countries ctx (:id current-user))
                        current-country (if (empty? country)
                                          (:user-country countries)
                                          country)]
                    (ok (into {:applications applications} countries))))

             (GET "/hello" [a b c]
                  (do

                    (ok {:kissa "miu miu"}))))))
