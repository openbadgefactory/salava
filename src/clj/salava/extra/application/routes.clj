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
             (GET "/" [country tags name issuer order id followed]
                  :return schemas/BadgeAdverts
                  :summary "Get badge adverts"
                  :current-user current-user
                  :auth-rules access/authenticated
                  (let [applications (a/get-badge-adverts ctx country tags name issuer order id (:id current-user) (if (= "true" followed) true false))
                        countries (a/badge-adverts-countries ctx (:id current-user))
                        current-country (if (empty? country)
                                          (:user-country countries)
                                          country)]
                    (ok (into {:applications applications} countries))))
             
             (GET "/public_badge_advert_content/:id" []
                  :return schemas/BadgeAdvertModal
                  :summary "Get badge advert"
                  :current-user current-user
                  :path-params [id :- s/Int]
                  :auth-rules access/authenticated
                  (ok (a/get-badge-advert ctx id (:id current-user))))

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

             (POST "/create_connection_badge_application/:badge_advert_id" []
                   :return {:status (s/enum "success" "error") }
                   :summary "Add badge advert to follow"
                   :path-params [badge_advert_id :- s/Int]
                   :auth-rules access/authenticated
                   :current-user current-user
                   (ok (a/create-connection-badge-advert! ctx (:id current-user) badge_advert_id))
                   )

             (DELETE "/delete_connection_badge_application/:badge_advert_id" []
                   :return {:status (s/enum "success" "error")}
                   :summary "Delete badge advert from follow"
                   :path-params [badge_advert_id :- s/Int]
                   :auth-rules access/authenticated
                   :current-user current-user
                   (ok (a/delete-connection-badge-advert! ctx (:id current-user) badge_advert_id))
                   )
             (PUT "/unpublish_badge/:apikey/:remoteid" []
                  :return {:success s/Bool}
                  :body  [data schemas/BadgeAdvertUnpublish]
                  (ok (a/unpublish-badge ctx data)))
             )
    
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
