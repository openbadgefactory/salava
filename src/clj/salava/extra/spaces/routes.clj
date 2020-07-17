
(ns salava.extra.spaces.routes
  (:require [compojure.api.sweet :refer :all]
            [ring.util.http-response :refer :all]
            [ring.swagger.upload :as upload]
            [ring.util.response :refer [redirect]]
            [ring.util.io :as io]
            [schema.core :as s]
            [salava.core.helper :refer [dump]]
            [salava.core.util :as u]
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
            (layout/main ctx "/users")
            (layout/main ctx "/edit")
            (layout/main ctx "/error")
            (layout/main ctx "/report")

            (GET "/member_invite/:uid/:token" req
                  :path-params [uid :- s/Str
                                token :- s/Str]
                  :summary "join space via invitation link"
                  :current-user current-user
                  (let [{:keys [status message id]} (space/invite-user ctx uid token)]

                   (if current-user
                      (if (= status "success")
                        (space/set-space-session ctx id (found (str (u/get-base-path ctx) (str "/connections/spaces"))) current-user)
                        (redirect (str (u/get-base-path ctx) (str "/connections/spaces?error=" true))))
                      (if (= status "success")

                       (-> (redirect (str (u/get-base-path ctx) (str "/user/login?invite_token="token)))
                           (assoc-in [:session] (assoc (get req :session {}) :invitation {:token token :alias uid :id id}))
                           #_(assoc :session (assoc (get req :session {}) :invitation {:token token :alias uid :id id})))
                       (redirect (str (u/get-base-path ctx) (str "/space/error"))))))))
   (context "/obpv1/space" []
             :tags ["space"]

            (GET  "/export_statistics" [id]
                  :summary "Export admin stats to csv format"
                  :auth-rules access/space-admin
                  :current-user current-user
                  (let [alias (:alias (space/get-space ctx id))]
                    (-> (io/piped-input-stream (stats/export-space-statistics ctx id current-user))
                        ok
                        (header "Content-Disposition" (str "attachment; filename=\""alias"_statistics.csv\""))
                        (header "Content-Type" "text/csv"))))

            (POST "/stats/:id" []
                   :auth-rules access/space-admin
                   :summary "Get space stats"
                   :path-params [id :- s/Int]
                   :current-user current-user
                   (ok (stats/space-stats ctx id (:last-visited current-user))))

            (POST "/populate/:id" []
                   :auth-rules access/admin
                   :summary "Populate space with users"
                   :path-params [id :- s/Int]
                   :body-params [users :- [s/Int]]
                   :current-user current-user
                   (ok (space/populate-space ctx id users (:id current-user))))

            (POST "/report" []
                   :auth-rules access/space-admin
                   :summary "Generate report based on filters"
                   :body [filters {:users [(s/maybe s/Int)]
                                   :badges [(s/maybe s/Int)]
                                   :to (s/maybe s/Int)
                                   :from (s/maybe s/Int)
                                   :space-id s/Int}]
                   :current-user current-user
                   (ok (db/report! ctx filters (:id current-user)))))

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

            (POST "/invitelink/:id" []
                   :return {:status s/Bool :token (s/maybe s/Str)}
                   :auth-rules access/space-admin
                   :summary "Get invite link info"
                   :path-params [id :- s/Int]
                   :current-user current-user
                   (ok (space/invite-link-info ctx id)))

            (POST "/invitelink/update_status/:id" []
                   :return {:status (s/enum "success" "error")}
                   :auth-rules access/space-admin
                   :summary "Initialize member admin's page"
                   :current-user current-user
                   :body-params [status :- s/Bool]
                   :path-params [id :- s/Int]
                   (ok (space/update-link-status ctx id status)))

            (POST "/invitelink/refresh_token/:id" []
                   :return {:status (s/enum "success" "error")}
                   :auth-rules access/space-admin
                   :summary "refresh invite-link token"
                   :current-user current-user
                   :path-params [id :- s/Int]
                   (ok (space/refresh-token ctx id)))

            (POST "/check_membership/:id" []
                   :auth-rules access/authenticated
                   :summary "Check if user is a member of the organization"
                   :path-params [id :- s/Int]
                   :current-user current-user
                   (ok (space/is-member? ctx id (:id current-user))))

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

            (POST "/reset_theme/:id" []
                    :return {:status (s/enum "success" "error")}
                    :auth-rules access/space-admin
                    :summary "Reset theme"
                    :path-params [id :- s/Int]
                    :current-user current-user
                    (ok (space/reset-theme! ctx id (:id current-user))))

            (POST "/accept/:id/:user_id" []
                    :return {:status (s/enum "success" "error")}
                    :auth-rules access/space-admin
                    :summary "accept user into space"
                    :path-params [id :- s/Int
                                  user_id :- s/Int]
                    :current-user current-user
                    (ok (space/accept! ctx id user_id)))

            (POST "/user" []
                   :auth-rules access/signed
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
                   :body-params [current-space :- s/Bool]
                   :current-user current-user
                   (if current-space
                     (space/leave! ctx id current-user (ok {:status "success"}))
                     (ok (space/leave! ctx id (:id current-user)))))

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
                  :auth-rules access/signed
                  :summary "Switch space"
                  :path-params [id :- s/Int]
                  :current-user current-user
                  (space/switch! ctx (ok {:status "success"}) current-user id))

            (POST "/reset_switch" req
                  ;:return {:status (s/enum "success" "error")}
                  :auth-rules access/signed
                  :summary "exit space"
                  :current-user current-user
                  (space/reset-switch! ctx (ok {:status "success"}) current-user))

            (POST "/extend/:id" []
                  :return {:status (s/enum "success" "error")}
                  :path-params [id :- s/Int]
                  :body-params [valid_until :- s/Int]
                  :summary "extend Subscription by one year"
                  :auth-rules access/space-admin
                  :current-user current-user
                  (ok (space/extend-space-validity! ctx id valid_until (:id current-user))))

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
                  :auth-rules access/space-admin
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
