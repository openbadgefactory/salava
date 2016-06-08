(ns salava.gallery.routes
  (:require [compojure.api.sweet :refer :all]
            [ring.util.http-response :refer :all]
            [schema.core :as s]
            [salava.gallery.schemas :as schemas]
            [salava.core.layout :as layout]
            [salava.gallery.db :as g]
            [salava.core.access :as access]
            salava.core.restructure))

(defn route-def [ctx]
  (routes
    (context "/gallery" []
             (layout/main ctx "/")
             (layout/main ctx "/badges")
             (layout/main ctx "/badges/:user-id")
             (layout/main ctx "/badgeview/:badge-id")
             (layout/main ctx "/pages")
             (layout/main ctx "/pages/:user-id")
             (layout/main ctx "/profiles")
             (layout/main ctx "/getbadge"))

    (context "/obpv1/gallery" []
             :tags ["gallery"]
             (POST "/badges" []
                   ;:return [}
                   :body-params [country :- (s/maybe s/Str)
                                 badge :- (s/maybe s/Str)
                                 issuer :- (s/maybe s/Str)
                                 recipient :- (s/maybe s/Str)]
                   :summary "Get public badges"
                   :auth-rules access/authenticated
                   :current-user current-user
                   (let [countries (g/badge-countries ctx (:id current-user))
                         current-country (if (empty? country)
                                           (:user-country countries)
                                           country)]
                     (ok (into {:badges (g/public-badges ctx current-country badge issuer recipient)} countries))))
             (POST "/badges/:userid" []
                   ;:return []
                   :path-params [userid :- s/Int]
                   :summary "Get user's public badges."
                   :current-user current-user
                   (ok (hash-map :badges (g/public-badges-by-user ctx userid (if current-user "internal" "public")))))

             (GET "/public_badge_content/:badge-content-id" []
                  :return {:badge              {:name                s/Str
                                                :image_file          (s/maybe s/Str)
                                                :description         (s/maybe s/Str)
                                                :average_rating      (s/maybe s/Num)
                                                :rating_count        (s/maybe s/Int)
                                                :issuer_content_name (s/maybe s/Str)
                                                :issuer_content_url  (s/maybe s/Str)
                                                :issuer_contact      (s/maybe s/Str)
                                                :issuer_image        (s/maybe s/Str)
                                                :creator_name        (s/maybe s/Str)
                                                :creator_url         (s/maybe s/Str)
                                                :creator_email       (s/maybe s/Str)
                                                :creator_image       (s/maybe s/Str)
                                                :html_content        (s/maybe s/Str)
                                                :criteria_url        (s/maybe s/Str)
                                                :badge_url           (s/maybe s/Str)
                                                :verified_by_obf     s/Bool
                                                :issued_by_obf       s/Bool
                                                :issuer_verified     (s/maybe s/Bool)
                                                :obf_url             s/Str}
                           :public_users       (s/maybe [{:id         s/Int
                                                          :first_name s/Str
                                                          :last_name  s/Str
                                                          :profile_picture (s/maybe s/Str)}])
                           :private_user_count (s/maybe s/Int)}
                  :path-params [badge-content-id :- s/Str]
                  :summary "Get public badge data"
                  :current-user current-user
                  (ok (g/public-badge-content ctx badge-content-id (:id current-user))))

             (POST "/pages" []
                   :body-params [country :- (s/maybe s/Str)
                                 owner :- (s/maybe s/Str)]
                   :summary "Get public pages"
                   :auth-rules access/authenticated
                   :current-user current-user
                   (let [countries (g/page-countries ctx (:id current-user))
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
                   :return {:users [schemas/UserProfiles]
                            :countries [schemas/Countries]}
                   :body [search-params schemas/UserSearch]
                   :summary "Get public user profiles"
                   :auth-rules access/authenticated
                   :current-user current-user
                   (ok {:users     (g/public-profiles ctx search-params (:id current-user))
                        :countries (g/profile-countries ctx (:id current-user))})))))
