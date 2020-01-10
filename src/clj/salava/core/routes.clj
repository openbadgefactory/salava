(ns salava.core.routes
  (:require [compojure.api.sweet :refer :all]
            [ring.util.http-response :refer :all]
            [salava.core.layout :as layout]
            [salava.badge.main :as b]
            [schema.core :as s]
            [salava.core.util :refer [get-base-path]]
            salava.core.restructure))

(defn route-def [ctx]
  (routes
   (context "/" []
            (layout/main ctx "/error/:status"))

   (context "/" []
            (GET "/" []
                 :no-doc true
                 :current-user current-user
                 (if current-user
                   (temporary-redirect (str (get-base-path ctx) "/social"))
                   (temporary-redirect (str (get-base-path ctx) "/user/login")))))))


(defn legacy-routes [ctx]
  (routes
    (GET "/badges/badge_info/:oldid/:userid" []
         :no-doc true
         :path-params [oldid :- s/Int
                       userid :- s/Int]
         (let [new-badge-id (b/old-id->id ctx oldid userid)]
           (if new-badge-id
             (temporary-redirect (str (get-base-path ctx) "/badge/info/" new-badge-id))
             (not-found))))

    (GET "/pages/:pageid/view" []
      :no-doc true
      :path-params [pageid :- s/Int]
      (temporary-redirect (str (get-base-path ctx) "/page/view/" pageid)))

    (GET "/user/:userid" []
      :no-doc true
      :path-params [userid :- s/Int]
      (temporary-redirect (str (get-base-path ctx) "/user/profile/" userid)))

    (GET "/gallery/badgeview/:oldid" []
      :no-doc true
      :path-params [oldid :- s/Int]
      (let [badge-content-id (b/old-id->id ctx oldid nil)]
        (if badge-content-id
          (temporary-redirect (str (get-base-path ctx) "/gallery/badgeview/" badge-content-id))
          (not-found))))

    (GET (str (get-base-path ctx) "/user/profile/:id") []
         :no-doc true
         :path-params [id :- s/Int]
         (temporary-redirect (str (get-base-path ctx) "/profile/" id)))

    (GET (str (get-base-path ctx) "/page/view/:id") []
         :no-doc true
         :path-params [id :- s/Int]
         (temporary-redirect (str (get-base-path ctx) "/profile/page/view/" id)))))
