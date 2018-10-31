(ns salava.metabadge.routes)

#_(defn route-def [ctx]
  (context "/obpv1/metabadge" []
           :tags ["metabadge"]
           (GET "/metabadge/:badge_id"
                :path-params [badge_id :- String]
                :summary "check metabadge")
           )
  )
