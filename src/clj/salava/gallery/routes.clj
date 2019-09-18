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

             (GET "/badges" []
                  :return schemas/Badgesgallery
                  :summary "Get badges, countries and tags"
                  :query [params schemas/BadgeQuery]
                  :current-user current-user
                  :auth-rules access/signed
                  (ok (g/gallery-badges ctx params)))

             (GET "/badge_tags" []
                  :return schemas/BadgesgalleryTags
                  :summary "Get all tags in public badges"
                  :current-user current-user
                  :auth-rules access/signed
                  (ok (g/badge-tags ctx)))

             (GET "/badge_countries" []
                  :return schemas/BadgesgalleryCountries
                  :summary "Get public badge countries"
                  :current-user current-user
                  :auth-rules access/signed
                  (ok (g/badge-countries ctx)))


             (GET "/public_badge_content/:badge-id" []
;;                   :return schemas/BadgeContent
                  :path-params [badge-id :- s/Str]
                  :summary "Get public badge data"
                  :current-user current-user
                  (ok (g/public-multilanguage-badge-content ctx badge-id (:id current-user))))

             (GET "/public_badge_content/:gallery-id/:badge-id" []
                  ;;                   :return schemas/BadgeContent
                  :path-params [gallery-id :- s/Int
                                badge-id :- s/Str]
                  :summary "Get public gallery badge data"
                  :current-user current-user
                  (if (pos? gallery-id)
                    (ok (g/public-multilanguage-badge-content ctx  badge-id (:id current-user) gallery-id))
                    (ok (g/public-multilanguage-badge-content ctx  badge-id (:id current-user)))))

             (GET "/badge_gallery_id/:badge-id" []
                  :path-params [badge-id :- s/Str]
                  :summary "Get gallery id of a badge"
                  (ok (g/badge-gallery-id ctx badge-id)))

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
                  (ok (g/gallery-stats ctx (:last-visited current-user) (:id current-user))))

             (GET "/recent" [userid kind]
                  :summary "get user's recent badges or pages"
                  :current-user current-user
                  (ok (g/public-by-user ctx kind userid (:id current-user))))

             (GET "/recipients/:gallery_id" []
                   :summary "Get user badge stats about recipients, ratings"
                   :path-params [gallery_id :- Long]
                   :auth-rules access/authenticated
                   :current-user current-user
                   (ok (g/badge-recipients ctx (:id current-user) gallery_id))))))
