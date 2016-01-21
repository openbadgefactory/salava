(ns salava.gallery.routes
  (:require [compojure.api.sweet :refer :all]
            [ring.util.http-response :refer :all]
            [schema.core :as s]
            [salava.core.layout :as layout]
            [salava.gallery.db :as g]))

(defn route-def [ctx]
  (routes
    (context "/gallery" []
             (layout/main ctx "/")
             (layout/main ctx "/badges")
             (layout/main ctx "/badges/:user-id")
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
                   (let [countries (g/badge-countries ctx 1) ;TODO set current user id
                         current-country (if (empty? country)
                                           (:user-country countries)
                                           country)]
                     (ok (into {:badges (g/public-badges ctx current-country badge issuer recipient)} countries))))
             (POST "/badges/:userid" []
                   ;:return []
                   :path-params [userid :- s/Int]
                   :summary "Get user's public badges."
                   (ok (hash-map :badges (g/public-badges-by-user ctx userid))))

             (GET "/public_badge_content/:badge-content-id" []
                  :return {:badge        {:name           s/Str
                                          :image_file     (s/maybe s/Str)
                                          :description    (s/maybe s/Str)
                                          :average_rating (s/maybe s/Num)
                                          :rating_count   (s/maybe s/Int)
                                          :recipient      (s/maybe s/Int)
                                          :issuer_name    (s/maybe s/Str)
                                          :issuer_url     (s/maybe s/Str)
                                          :issuer_contact (s/maybe s/Str)
                                          :html_content   (s/maybe s/Str)
                                          :criteria_url   (s/maybe s/Str)}
                           :public_users (s/maybe [{:id s/Int
                                                    :first_name s/Str
                                                    :last_name s/Str}])
                           :private_user_count (s/maybe s/Int)}
                  :path-params [badge-content-id :- (s/constrained s/Str #(= (count %) 64))]
                  :summary "Get public badge data"
                  (ok (g/public-badge-content ctx badge-content-id 1))) ;TODO set current user id

             (POST "/pages" []
                   :body-params [country :- (s/maybe s/Str)
                                 owner :- (s/maybe s/Str)]
                   :summary "Get public pages"
                   (let [countries (g/page-countries ctx 1) ;TODO set current user id
                         current-country (if (empty? country)
                                           (:user-country countries)
                                           country)]
                     (ok (into {:pages (g/public-pages ctx current-country owner)} countries))))

             (POST "/pages/:userid" []
                   :path-params [userid :- s/Int]
                   :summary "Get user's public badges."
                   (ok (hash-map :pages (g/public-pages-by-user ctx userid)))))))
