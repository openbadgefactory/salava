(ns salava.gallery.routes
  (:require [compojure.api.sweet :refer :all]
            [ring.util.http-response :refer :all]
            [schema.core :as s]
            [salava.core.layout :as layout]
            [salava.gallery.db :as g]))

(defroutes* route-def
  (context* "/gallery" []
            (layout/main "/")
            (layout/main "/badges")
            (layout/main "/badges/:user-id")
            (layout/main "/pages")
            (layout/main "/profiles")
            (layout/main "/getbadge"))

  (context* "/obpv1/gallery" []
            (POST* "/badges" []
                   ;:return [}
                   :body-params [country :- (s/maybe s/Str)
                                 badge :- (s/maybe s/Str)
                                 issuer :- (s/maybe s/Str)
                                 recipient :- (s/maybe s/Str)]
                   :components [context]
                   :summary "Get public badges"
                   (let [countries (g/badge-countries context 1) ;TODO set current user id
                         current-country (if (empty? country)
                                           (:country countries)
                                           country)]
                     (ok (into {:badges (g/public-badges context current-country badge issuer recipient)} countries))))
            (POST* "/badges/:userid" []
                   ;:return []
                   :path-params [userid :- (s/maybe Long)]
                   :components [context]
                   :summary "Get user's public badges."
                   (ok (hash-map :badges (g/public-badges-by-user context userid)
                                 :countries [])))))
