(ns salava.extra.application.routes
  (:require [compojure.api.sweet :refer :all]
            [ring.util.http-response :refer :all]
            [schema.core :as s]
            [salava.core.helper :refer [dump]]
            [salava.core.layout :as layout]
            [salava.extra.application.db :as a]
            [salava.core.access :as access]
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
                  :summary "Get badge adverts"
                  :current-user current-user
                  :auth-rules access/authenticated
                  (let [applications (a/get-badge-adverts ctx country tags name issuer order)
                        countries (a/badge-adverts-countries ctx (:id current-user))
                        current-country (if (empty? country)
                                          (:user-country countries)
                                          country)]
                    (ok (into {:applications applications} countries))))
             
             (GET "/public_badge_advert_content/:id" []
                  ;:return [{:iframe s/Str :language s/Str}]
                  :summary "Get badge advert"
                  :current-user current-user
                  :path-params [id :- s/Int]
                  :auth-rules access/authenticated
                  (ok (a/get-badge-advert ctx id)))

             (GET "/autocomplete" [country]
                  ;:return [{:iframe s/Str :language s/Str}]
                  :summary "Get autocomplete data"
                  :current-user current-user
                  :auth-rules access/authenticated
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
