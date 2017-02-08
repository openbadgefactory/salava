(ns salava.extra.application.routes
  (:require [compojure.api.sweet :refer :all]
            [ring.util.http-response :refer :all]
            [schema.core :as s]
            [salava.core.helper :refer [dump]]
            [salava.core.layout :as layout]
            [salava.extra.application.db :as a]
            [salava.extra.application.schemas :as schemas] ;cljc
            [clojure.string :refer [split]]))

(defn route-def [ctx]
  (routes
    (context "/gallery" []
             (layout/main ctx "/application"))
    (context "/obpv1/application" []
             :tags ["application"]
             (GET "/" [country tags name issuer order]
                  ;:return [{:iframe s/Str :language s/Str}]
                  :summary "Get public badge data"
                  :current-user current-user
                  (let [applications (a/get-badge-adverts ctx country tags name issuer order)
                        countries (a/badge-adverts-countries ctx (:id current-user))
                        current-country (if (empty? country)
                                          (:user-country countries)
                                          country)]
                    (ok (into {:applications applications} countries))))

             (GET "/autocomplete" [country]
                  ;:return [{:iframe s/Str :language s/Str}]
                  :summary "Get autocomplete data"
                  :current-user current-user
                  (ok (a/get-autocomplete ctx "" country)))
             
             (PUT "/publish_badge/:apikey/:remoteid" []
                  :return {:success s/Bool}
                  :body  [data schemas/BadgeAdvertPublish]
                  (ok (a/publish-badge ctx data)))
             
             (PUT "/unpublish_badge/:apikey/:remoteid" []
                  :return {:success s/Bool}
                  :body  [data schemas/BadgeAdvertUnpublish]
                  (ok (a/unpublish-badge ctx data))))
    
    (context "/obpv1/factory" []
             :tags ["factory"]
            (PUT "/publish_badge/:apikey/:remoteid" []
                 :return {:success s/Bool}
                 :body  [data schemas/BadgeAdvertPublish]
                 (ok (a/publish-badge ctx data)))

            (PUT "/unpublish_badge/:apikey/:remoteid" []
                 :return {:success s/Bool}
                 :body  [data schemas/BadgeAdvertUnpublish]
                 (ok (a/unpublish-badge ctx data))))))
