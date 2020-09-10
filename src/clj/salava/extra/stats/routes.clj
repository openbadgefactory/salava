(ns salava.extra.stats.routes
  (:require
    [compojure.api.sweet :refer :all]
    [ring.util.http-response :refer :all]
    [salava.extra.stats.db :as stats]
    [salava.core.access :as access]
    [schema.core :as s]))

(defn route-def [ctx]
  (routes
   (context "/obpv1/stats" []
            :tags ["social_media_stats"]
            :no-doc true

            (GET "/social_media" []
                 :summary "Get latest social media stats"
                 :auth-rules access/space-admin
                 :current-user current-user
                 (ok (stats/social-media-stats-latest ctx)))

            (GET "/social_media/:timestamp" []
                 :summary "Get social media stats since timestamp"
                 :path-params [timestamp :- s/Int]
                 :auth-rules access/space-admin
                 :current-user current-user
                 (ok (stats/social-media-stats-ts ctx timestamp)))

            (GET "/social_media/:timestamp/:space_id" []
                 :summary "Get space's social media stats since timestamp"
                 :path-params [space_id :- s/Int
                               timestamp :- (s/maybe s/Int)]
                 :auth-rules access/space-admin
                 :current-user current-user
                 (ok (stats/space-social-media-stats ctx space_id timestamp))))))
