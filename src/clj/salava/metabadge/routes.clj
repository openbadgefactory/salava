(ns salava.metabadge.routes
  (:require [compojure.api.sweet :refer :all]
            [ring.util.http-response :refer :all]
            [salava.core.access :as access]
            [salava.metabadge.metabadge :as mb]
            [salava.metabadge.db :as db]
            [salava.badge.main :as b]
            [salava.core.layout :as layout]
            [salava.metabadge.schemas :as schemas]
            [salava.metabadge.cron :as cron]
            salava.core.restructure))

(defn route-def [ctx]
  (routes
    (context "/badge" []
             (layout/main ctx "/metabadges"))

    (context "/obpv1/metabadge" []
             :tags ["metabadge"]
             (GET "/" []
                  ;:return [schemas/Allmetabadges]
                  :summary "get all metabadges"
                  :auth-rules access/signed
                  :current-user current-user
                  (cron/every-hour ctx)
                  ;(salava.metabadge.db/get-metabadges-in-progress ctx (:id current-user))
                  (ok (mb/all-metabadges ctx current-user)))

             (GET "/info" [user_badge_id]
                  ;:return schemas/Metabadge
                  :summary "get metabadge info via assertion url"
                  :current-user current-user
                  (ok (db/get-metabadge ctx user_badge_id (:id current-user)) #_(mb/check-metabadge ctx assertion_url)))

             (GET "/badge/info" [user_badge_id]
                  ;:return schemas/Milestone?
                  :summary "check if badge is a metabadge"
                  :current-user current-user
                  (ok (db/is-metabadge? ctx user_badge_id))
                  #_(ok (mb/get-user-badge-data ctx (:id current-user) user_badge_id )))

             (POST "/update_status/:badgeid" []
                   :path-params [badgeid :- String]
                   :summary "change badge status from declined to accepted"
                   :auth-rules access/authenticated
                   :current-user current-user
                   (ok (b/set-status! ctx badgeid "accepted"(:id current-user) )))
             )))
