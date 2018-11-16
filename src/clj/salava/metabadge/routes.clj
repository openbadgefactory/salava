(ns salava.metabadge.routes
  (:require [compojure.api.sweet :refer :all]
            [ring.util.http-response :refer :all]
            [salava.core.access :as access]
            [salava.metabadge.metabadge :as mb]
            [salava.core.layout :as layout]
            salava.core.restructure))

(defn route-def [ctx]
  (routes
    (context "/badge" []
             (layout/main ctx "/metabadges"))

    (context "/obpv1/metabadge" []
             :tags ["metabadge"]
             (GET "/" []
                  :summary "get all metabadges"
                  :auth-rules access/signed
                  :current-user current-user
                  (ok (mb/all-metabadges ctx current-user))
                  )
             (GET "/info" [assertion_url]
                  :summary "get metabadge info via assertion url"
                  :current-user current-user
                  (mb/all-metabadges ctx current-user)
                  #_(prn (map #(-> %
                                 :test
                                 :metabadge
                                 :badge
                                 )(mb/all-metabadges ctx current-user)))
                  (ok (mb/check-metabadge ctx assertion_url)))

             (GET "/badge/info" [assertion_url]
                  :summary "check if badge is a metabadge"
                  :current-user current-user
                  (ok (mb/milestone? ctx assertion_url )))
             )))
