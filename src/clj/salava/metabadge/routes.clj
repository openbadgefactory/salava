(ns salava.metabadge.routes
  (:require [compojure.api.sweet :refer :all]
            [ring.util.http-response :refer :all]
            [schema.core :as s]
            [salava.core.access :as access]
            [salava.metabadge.metabadge :as mb]
            [salava.metabadge.db :as db]
            [salava.badge.main :as b]
            [salava.core.layout :as layout]
            [salava.core.middleware :as mw]
            [salava.metabadge.schemas :as schemas]
            [salava.metabadge.cron :as cron]
            [salava.core.restructure]))

(defn route-def [ctx]
  (routes
   (context "/badge" []
            (layout/main ctx "/metabadges"))

   (context "/obpv1/metabadge" []
            :tags ["metabadge"]
            (GET "/" []
                 :return schemas/AllMetabadges
                 :summary "get all metabadges"
                 :auth-rules access/signed
                 :current-user current-user
                 (ok (mb/all-metabadges ctx (:id current-user))))

            (GET "/info/:user_badge_id" []
                 :return schemas/BadgeMetabadge
                 :path-params [user_badge_id :- Long]
                 :summary "get metabadge info via user-badge-id"
                 :current-user current-user
                 (ok (mb/get-metabadge ctx user_badge_id (:id current-user))))

            (GET "/assertion/info/:assertion_url" []
                 :return schemas/BadgeMetabadge
                 :path-params [assertion_url :- (s/maybe s/Str)]
                 :summary "get metabadge info via assertion url"
                 :current-user current-user
                 (ok (mb/check-metabadge ctx assertion_url)))

            (POST "/update_status/:badgeid" []
                  :return {:status (s/enum "success" "error") :id s/Int (s/optional-key :message) (s/maybe s/Str)}
                  :path-params [badgeid :- String]
                  :summary "change badge status from declined to accepted"
                  :auth-rules access/authenticated
                  :current-user current-user
                  (ok (b/set-status! ctx badgeid "accepted" (:id current-user)))))

   (context "/obpv1/factory" []
            :tags ["factory"]

            (PUT "/metabadge" []
                 :no-doc true
                 :return {:success Boolean}
                 :body  [data schemas/MetabadgeUpdate]
                 :middleware [#(mw/wrap-factory-auth % ctx)]
                 (ok (db/metabadge-update ctx data))))))
