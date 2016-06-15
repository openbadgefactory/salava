(ns salava.displayer.routes
  (:require [compojure.api.sweet :refer :all]
            [ring.util.http-response :refer :all]
            [schema.core :as s]
            [salava.displayer.db :as d]
            [salava.displayer.schemas :as schemas]))

(defn route-def [ctx]
  (routes
    (context "/displayer" []
             :tags  ["displayer"]
             (POST "/convert/email" req
                   :return schemas/DisplayerEmail
                   :summary "Return user-id by email-address"
                   (let [email (get-in req [:params :email] "missing-email")
                         user-id (d/convert-email ctx email)]
                     (if user-id
                       (ok {:userId user-id :email email :status "okay"})
                       (not-found {:error (str "Could not find the user by the email address: " email) :status "missing"}))))

             (GET "/:userid/groups.json" []
                  :return schemas/DisplayerGroups
                  :path-params [userid :- s/Int]
                  :summary "Return public badge groups by user-id"
                  (let [groups (d/displayer-groups ctx userid)]
                    (if groups
                      (ok {:userId userid :groups groups})
                      (not-found {:httpStatus 404 :status "missing"}))))

             (GET "/:userid/group/:groupid" []
                  :return schemas/DisplayerBadges
                  :path-params [userid :- s/Int
                                groupid :- s/Str]
                  :summary "Return public badges by user-id and group-id"
                  (let [[badges tag-id] (d/displayer-badges ctx userid groupid)]
                    (if badges
                      (ok {:userId userid :groupId tag-id :badges badges})
                      (not-found {:httpStatus 404 :status "missing"})))))))
