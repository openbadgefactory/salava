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

             #_(GET "/badges/space/:space-id" []
                    :return schemas/Badgesgallery
                    :summary "Get badges, countries and tags"
                    :query [params (dissoc schemas/BadgeQuery :country)]
                    :current-user current-user
                    :path-params [space-id :- s/Int]
                    :auth-rules access/signed
                    (ok (g/space-gallery-badges ctx params)))

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
                  :no-doc true
                  :return schemas/BadgeContent
                  :path-params [badge-id :- s/Str]
                  :summary "Get public badge data"
                  :current-user current-user
                  (ok (g/public-multilanguage-badge-content ctx badge-id (:id current-user))))

             (GET "/public_badge_content/:gallery-id/:badge-id" []
                  :no-doc true
                  :return schemas/BadgeContent
                  :path-params [gallery-id :- s/Int
                                badge-id :- s/Str]
                  :summary "Get public gallery badge data"
                  :current-user current-user
                  (if (pos? gallery-id)
                    (ok (g/public-multilanguage-badge-content ctx  badge-id (:id current-user) gallery-id))
                    (ok (g/public-multilanguage-badge-content ctx  badge-id (:id current-user)))))

             (GET "/p/public_badge_content/:gallery-id/:badge-id" []
                  :return schemas/badge-content-p
                  :path-params [gallery-id :- schemas/gallery-id
                                badge-id :- s/Str]
                  :summary "Get public gallery badge data"
                  :current-user current-user
                  (if (pos? gallery-id)
                    (ok (g/public-multilanguage-badge-content-p ctx  badge-id (:id current-user) gallery-id))
                    (ok (g/public-multilanguage-badge-content-p ctx  badge-id (:id current-user)))))

             (GET "/badge_gallery_id/:badge-id" []
                  :return (s/maybe s/Int)
                  :path-params [badge-id :- s/Str]
                  :summary "Get gallery id of a badge"
                  (ok (g/badge-gallery-id ctx badge-id)))

             (POST "/pages" []
                   :no-doc true
                   :return schemas/gallery-pages
                   :body [params schemas/pages-search]
                   :summary "Get public pages"
                   :auth-rules access/signed
                   :current-user current-user
                   (ok (g/gallery-pages ctx params (:id current-user))))

             (POST "/p/pages" []
                   :return schemas/gallery-pages-p
                   :body [params schemas/pages-search]
                   :summary "Get public pages"
                   :auth-rules access/signed
                   :current-user current-user
                   (let [{:keys [country owner]} params
                         countries       (g/page-countries ctx (:id current-user))
                         current-country (if (empty? country)
                                           (:user-country countries)
                                           country)]
                     (ok (into {:pages (g/public-pages-p ctx current-country owner)} countries))))

             (POST "/pages/:userid" []
                   :no-doc true
                   :path-params [userid :- s/Int]
                   :summary "Get user's public pages."
                   :current-user current-user
                   (ok (hash-map :pages (g/public-pages-by-user ctx userid (if current-user "internal" "public")))))

             #_(POST "/profiles" []
                     :return {:users     [schemas/UserProfiles]
                              :countries [schemas/Countries]}
                     :body [search-params schemas/UserSearch]
                     :summary "Get public user profiles"
                     :auth-rules access/signed
                     :current-user current-user
                     (ok {:users     (g/public-profiles ctx search-params (:id current-user))
                          :countries (g/profile-countries ctx (:id current-user))}))

             (POST "/profiles" []
                   :return {:users     [schemas/UserProfiles]
                            :countries [schemas/Countries]}
                   :body [search-params schemas/UserSearch]
                   :summary "Get public user profiles"
                   :auth-rules access/signed
                   :current-user current-user
                   (ok (g/profiles-all ctx search-params (:id current-user) false)))

             (GET "/stats" []
                  :no-doc true
                  :summary "Get gallery stats"
                  :auth-rules access/signed
                  :current-user current-user
                  (ok (g/gallery-stats ctx (:last-visited current-user) (:id current-user))))

             (GET "/recent" [userid kind]
                  :no-doc true
                  :summary "get user's recent badges or pages"
                  :current-user current-user
                  (ok (g/public-by-user ctx kind userid (:id current-user))))

             (GET "/recipients/:gallery_id" []
                   :return schemas/recipients
                   :summary "Get user badge stats about recipients"
                   :path-params [gallery_id :- Long]
                   :auth-rules access/authenticated
                   :current-user current-user
                   (ok (g/badge-recipients ctx (:id current-user) gallery_id)))

            (GET "/rating/:gallery_id" []
                  :return {:average_rating (s/maybe s/Num) :rating_count (s/maybe s/Int)}
                  :summary "Get badge rating"
                  :path-params [gallery_id :- Long]
                  :auth-rules access/authenticated
                  :current-user current-user
                  (ok (g/badge-rating ctx (:id current-user) gallery_id)))

            (POST "/profiles/:user_badge_id/:context" []
                  :no-doc true
                  :return {:users     [schemas/UserProfiles]
                           :countries [schemas/Countries]}
                  :path-params [user_badge_id :- s/Int
                                context :- (s/enum "endorsement")]
                  :body [search-params schemas/UserSearch]
                  :summary "Get public user profiles, add optional information like endorsement status, endorsement requests etc based on context"
                  :auth-rules access/signed
                  :current-user current-user
                  (ok {:users     (g/public-profiles-context ctx search-params (:id current-user) user_badge_id context)
                       :countries (g/profile-countries ctx (:id current-user))}))

            (POST "/profiles/all/filter/:space_id" []
                  :no-doc true
                  #_:return #_{:users     [schemas/UserProfiles]
                               :countries [schemas/Countries]
                               :users_count s/Int}
                  :body [search-params schemas/UserSearch]
                  :path-params [space_id :- s/Int]
                  :summary "Get profiles with pagination, filter out existing space members"
                  :auth-rules access/admin
                  :current-user current-user
                  (ok (g/profiles-all ctx (assoc search-params :space-id space_id) (:id current-user)  true)))

            (GET "/user_owns_badge/:badge_id" []
                 :return s/Bool
                 :path-params [badge_id :- s/Str]
                 :summary "Check if user owns gallery badge"
                 :current-user current-user
                 :auth-rules access/signed
                 (ok (g/user-owns-badge? ctx (:id current-user) badge_id))))))
