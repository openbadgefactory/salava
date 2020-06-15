(ns salava.extra.spaces.routes
  (:require [compojure.api.sweet :refer :all]
            [ring.util.http-response :refer :all]
            [ring.swagger.upload :as upload]
            [schema.core :as s]
            [salava.core.helper :refer [dump]]
            [salava.core.layout :as layout]
            [salava.extra.spaces.space :as space]
            [salava.extra.spaces.db :as db]
            [salava.extra.spaces.util :as util]
            [salava.core.access :as access]
            [salava.extra.spaces.schemas :as schemas] ;cljc
            [clojure.string :refer [split]]
            [salava.extra.spaces.stats :as stats]))

(defn route-def [ctx]
  (routes
   (context "/admin" []
            (layout/main ctx "/spaces")
            (layout/main ctx "/spaces/creator")
            (layout/main ctx "/spaces/user/admin"))

   (context "/connections" []
            (layout/main ctx "/spaces"))

   (context "/gallery" []
            (layout/main ctx "/spaces"))

   (context "/space" []
            (layout/main ctx "/admin")
            (layout/main ctx "/stats")
            (layout/main ctx "/manage")
            (layout/main ctx "/users"))


   (context "/obpv1/spaces" []
            :tags ["spaces"]

            (GET "/" []
                  :auth-rules access/admin
                  :summary "Get all spaces"
                  :current-user current-user
                  (ok (db/all-spaces ctx)))

            (GET "/:id" []
                  :auth-rules access/authenticated
                  :summary "Get space"
                  :path-params [id :- s/Int]
                  :current-user current-user
                  (ok (space/get-space ctx id)))

            (GET "/gallery/all" []
                   :auth-rules access/authenticated
                   :summary "Get all open and controlled spaces"
                   :query [params {:name s/Str
                                   :order  (s/enum "member_count" "mtime" "name")
                                   :page_count s/Int}]
                   :current-user current-user
                   (let [{:keys [name order page_count]} params]
                     (ok (db/get-gallery-spaces ctx name order page_count))))

            (POST "/check_membership/:id" []
                   :auth-rules access/authenticated
                   :summary "Check if user is a member of the organization"
                   :path-params [id :- s/Int]
                   :current-user current-user
                   (ok (space/is-member? ctx id (:id current-user))))

            (POST "/stats/:id" []
                   :auth-rules access/space-admin
                   :summary "Get space stats"
                   :path-params [id :- s/Int]
                   :current-user current-user
                   (ok (stats/space-stats ctx id (:last-visited current-user))))

            (POST "/userlist/:id" []
                   :auth-rules access/space-admin
                   :summary "Get the members of an organization"
                   :path-params [id :- s/Int]
                   :current-user current-user
                   (ok (space/members ctx id)))

            (POST "/remove_user/:id/:user_id" []
                    :return {:status (s/enum "success" "error")}
                    :auth-rules access/space-admin
                    :summary "Remove user from organization"
                    :path-params [id :- s/Int
                                  user_id :- s/Int]
                    :current-user current-user
                    ;(when (= user_id (:id current-user)))
                    (ok (space/leave! ctx id user_id)))

            (POST "/accept/:id/:user_id" []
                    :return {:status (s/enum "success" "error")}
                    :auth-rules access/space-admin
                    :summary "accept user into space"
                    :path-params [id :- s/Int
                                  user_id :- s/Int]
                    :current-user current-user
                    (ok (space/accept! ctx id user_id)))

            (POST "/user" []
                   :auth-rules access/authenticated
                   :summary "Get all spaces user belongs to"
                   :current-user current-user
                   (ok (db/get-user-spaces ctx (:id current-user))))

            (POST "/user/join/:id" []
                   :return {:status (s/enum "success" "error")}
                   :summary "Join space"
                   :path-params [id :- s/Int]
                   :current-user current-user
                   (ok (space/join! ctx id (:id current-user))))

            (POST "/user/leave/:id" []
                   :return {:status (s/enum "success" "error")}
                   :auth-rules access/authenticated
                   :summary "Leave space"
                   :path-params [id :- s/Int]
                   :current-user current-user
                   (ok (space/leave! ctx id (:id current-user))))

            (POST "/user/default/:id" []
                   :return {:status (s/enum "success" "error")}
                   :auth-rules access/authenticated
                   :summary "Set space as default"
                   :path-params [id :- s/Int]
                   :current-user current-user
                   (ok (db/set-default-space ctx id (:id current-user))))

            (POST "/add_admin/:id" []
                  :return {:status (s/enum  "success" "error") (s/optional-key :message) s/Str}
                  :auth-rules access/admin
                  :summary "Add admin to space"
                  :path-params [id :- s/Int]
                  :body-params [admins :- [s/Int]]
                  :current-user current-user
                  (ok (db/add-space-admins ctx id admins)))

            (POST "/switch/:id" req
                  ;:return {:status (s/enum "success" "error")}
                  :auth-rules access/authenticated
                  :summary "Switch space"
                  :path-params [id :- s/Int]
                  :current-user current-user
                  (space/switch! ctx (ok {:status "success"}) current-user id))

            (POST "/create" []
                  :return {:status (s/enum  "success" "error") (s/optional-key :message) s/Str}
                  :body [space schemas/create-space]
                  :auth-rules access/admin
                  :summary "Create new space"
                  :current-user current-user
                  (ok (space/create! ctx space)))

            (POST "/downgrade/:id/:admin-id" []
                  :return {:status (s/enum "success" "error")}
                  :path-params [id :- s/Int
                                admin-id :- s/Int]
                  :summary "downgrade admin to space member"
                  :auth-rules access/space-admin
                  :current-user current-user
                  (ok (space/downgrade! ctx id admin-id)))

            (POST "/upgrade/:id/:admin-id" []
                  :return {:status (s/enum "success" "error")}
                  :path-params [id :- s/Int
                                admin-id :- s/Int]
                  :summary "upgrade member to space admin"
                  :auth-rules access/space-admin
                  :current-user current-user
                  (ok (space/upgrade! ctx id admin-id)))

            (POST "/edit/:id" []
                  :return {:status (s/enum  "success" "error") (s/optional-key :message) s/Str}
                  :auth-rules access/admin
                  :path-params [id :- s/Str]
                  :body [space schemas/edit-space]
                  :summary "Edit space"
                  :current-user current-user
                  (ok (space/edit! ctx id space (:id current-user))))

            (POST "/update_status/:id" []
                  :return {:status (s/enum "success" "error")}
                  :auth-rules access/admin
                  :path-params [id :- s/Str]
                  :body-params [status :- (s/enum "active" "deleted" "suspended")]
                  :summary "Update space status"
                  :current-user current-user
                  (ok (space/update-status! ctx id status (:id current-user))))

            (POST "/update_visibility/:id" []
                  :return {:status (s/enum "success" "error")}
                  :auth-rules access/space-admin
                  :path-params [id :- s/Str]
                  :body-params [visibility :- (s/enum "private" "open" "controlled")]
                  :summary "Update space visibility"
                  :current-user current-user
                  (ok (space/update-visibility! ctx id visibility (:id current-user))))

            #_(POST "/suspend/:id" []
                    :return {:success s/Bool}
                    :summary "Suspend space"
                    :path-params [id :- s/Str]
                    :current-user current-user
                    (ok (space/suspend! ctx id (:id current-user))))

            (POST "/upload_image/:kind" []
                  :return {:status (s/enum "success" "error") :url s/Str (s/optional-key :message) (s/maybe s/Str)}
                  :multipart-params [file :- upload/TempFileUpload]
                  :path-params [kind :- (s/enum "logo" "banner")]
                  :middleware [upload/wrap-multipart-params]
                  :summary "Upload badge image (PNG)"
                  :auth-rules access/authenticated
                  :current-user current-user
                  (ok (util/upload-image ctx current-user file kind)))

            (DELETE "/delete/:id" []
                    :return {:status (s/enum "success" "error")}
                    :summary "Delete space"
                    :path-params [id :- s/Str]
                    :auth-rules access/admin
                    :current-user current-user
                    (ok (space/delete! ctx id (:id current-user)))))))
