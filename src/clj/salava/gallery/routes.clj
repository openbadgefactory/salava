(ns salava.gallery.routes
  (:require [compojure.api.sweet :refer :all]
            [ring.util.http-response :refer :all]
            [schema.core :as s]
            [salava.gallery.schemas :as schemas]
            [salava.core.layout :as layout]
            [salava.gallery.db :as g]
            [salava.core.helper :refer [dump string->number]]
            [salava.core.access :as access]
            salava.core.restructure))

(defn route-def [ctx]
  (routes
    (context "/gallery" []
             (layout/main ctx "/")
             (layout/main ctx "/badges")
             (layout/main ctx "/badges/:user-id")
             (layout/main ctx "/badges/:user-id/:badge_content_id")
             (layout/main-meta ctx "/badgeview/:id" :gallery)
             (layout/main ctx "/pages")
             (layout/main ctx "/pages/:user-id")
             (layout/main ctx "/profiles")
             (layout/main ctx "/getbadge"))

    (context "/obpv1/gallery" []
             :tags ["gallery"]
             (GET "/badges" [country tags badge-name issuer-name order recipient-name tags-ids page_count]
                  :return schemas/Badgesgallery
                  :summary "Get badges, countries,tags and user-country"
                  :current-user current-user
                  :auth-rules access/signed
                  (let [badges-and-tags (g/get-gallery-badges ctx country tags badge-name issuer-name order recipient-name tags-ids (string->number page_count))
                        countries       (g/badge-countries ctx (:id current-user))
                        current-country (if (empty? country)
                                          (:user-country countries)
                                          country)]
                    (ok (into badges-and-tags countries))))


             (GET "/public_badge_content/:badge-id" []
;;                   :return schemas/BadgeContent
                  :path-params [badge-id :- s/Str]
                  :summary "Get public badge data"
                  :current-user current-user
                  (ok (g/public-multilanguage-badge-content ctx badge-id (:id current-user))))

             (POST "/pages" []
                   :body-params [country :- (s/maybe s/Str)
                                 owner :- (s/maybe s/Str)]
                   :summary "Get public pages"
                   :auth-rules access/signed
                   :current-user current-user
                   (let [countries       (g/page-countries ctx (:id current-user))
                         current-country (if (empty? country)
                                           (:user-country countries)
                                           country)]
                     (ok (into {:pages (g/public-pages ctx current-country owner)} countries))))

             (POST "/pages/:userid" []
                   :path-params [userid :- s/Int]
                   :summary "Get user's public pages."
                   :current-user current-user
                   (ok (hash-map :pages (g/public-pages-by-user ctx userid (if current-user "internal" "public")))))

             (POST "/profiles" []
                   :return {:users     [schemas/UserProfiles]
                            :countries [schemas/Countries]}
                   :body [search-params schemas/UserSearch]
                   :summary "Get public user profiles"
                   :auth-rules access/signed
                   :current-user current-user
                   (ok {:users     (g/public-profiles ctx search-params (:id current-user))
                        :countries (g/profile-countries ctx (:id current-user))}))
             (GET "/stats" []
                  :summary "Get gallery stats"
                  :auth-rules access/signed
                  :current-user current-user
                  (ok (g/gallery-stats ctx))))))
