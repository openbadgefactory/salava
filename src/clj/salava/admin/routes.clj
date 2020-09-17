(ns salava.admin.routes
  (:require [compojure.api.sweet :refer :all]
            [ring.util.http-response :refer :all]
            [ring.util.response :refer [redirect]]
            [ring.util.io :as io]
            [salava.core.layout :as layout]
            [schema.core :as s]
            [salava.core.util :refer [get-base-path plugin-fun get-plugins]]
            [salava.admin.db :as a]
            [salava.admin.report :as report]
            [salava.core.helper :refer [dump]]
            [salava.core.access :as access]
            [salava.admin.schemas :as schemas]
            salava.core.restructure))


(defn route-def [ctx]
  (routes
    (context "/admin" []
             (layout/main ctx "/")
             (layout/main ctx "/tickets")
             (layout/main ctx "/statistics")
             (layout/main ctx "/userlist")
             (layout/main ctx "/report"))

    (context "/obpv1/admin" []
             :tags ["admin"]
             :no-doc true

             (GET "/stats" []
                  :return schemas/Stats
                  :summary "Get statistics"
                  :auth-rules access/admin
                  :current-user current-user
                  (ok (a/get-stats ctx (:last-visited current-user))))

             (POST "/report" []
              :return schemas/report
              :auth-rules access/admin
              :summary "Generate report based on filters"
              :body [filters {:users [(s/maybe s/Int)]
                              :badges [(s/maybe s/Int)]
                              :to (s/maybe s/Int)
                              :from (s/maybe s/Int)
                              :page_count s/Int}]
              :current-user current-user
              (ok (report/report! ctx filters (:id current-user))))

             (POST "/report/badges" []
              :return schemas/badges-for-report
              :auth-rules access/admin
              :summary "get user badges for report"
              :body [filters {:users [(s/maybe s/Int)]
                              :badges [(s/maybe s/Int)]
                              :to (s/maybe s/Int)
                              :from (s/maybe s/Int)}]
                              ;:page_count s/Int}])
              :current-user current-user
              (ok (report/badges-for-report ctx filters)))

             (GET "/report/export/:id" [users badges to from]
              :summary "Export report to csv format"
              :auth-rules access/admin
              :current-user current-user
              :path-params [id :- s/Int]
              (-> (io/piped-input-stream (report/export-report ctx users badges to from id current-user))
                  ok
                  (header "Content-Disposition" (str "attachment; filename=\"""report.csv\""))
                  (header "Content-Type" "text/csv")))

             (POST "/private_badge/:user_badge_id" []
                   :return (s/enum "success" "error")
                   :path-params [user_badge_id :- s/Int]
                   :summary "Set badge to private"
                   :auth-rules access/admin
                   :current-user current-user
                   (ok (a/private-badge! ctx user_badge_id)))

             (POST "/private_badges/:badge_id" []
                   :return (s/enum "success" "error")
                   :path-params [badge_id :- s/Str]
                   :summary "Set badges from gallery to private"
                   :auth-rules access/admin
                   :current-user current-user
                   (ok (a/private-badges! ctx badge_id)))

             (POST "/private_page/:page_id" []
                   :return (s/enum "success" "error")
                   :path-params [page_id :- s/Int]
                   :summary "Set page to private"
                   :auth-rules access/admin
                   :current-user current-user
                   (ok (a/private-page! ctx page_id)))

             (POST "/private_user/:user_id" []
                   :return (s/enum "success" "error")
                   :path-params [user_id :- s/Int]
                   :summary "Set user to private"
                   :auth-rules access/admin
                   :current-user current-user
                   (ok (a/private-user! ctx user_id)))

             (POST "/send_message/:user_id" []
                   :return (s/enum "success" "error")
                   :summary "send message to user"
                   :body-params [subject :- s/Str
                                 message :- s/Str
                                 email   :- s/Str]
                   :path-params [user_id :- s/Int]
                   :auth-rules access/admin
                   :current-user current-user
                   (ok (a/send-message ctx user_id subject message email)))

             (GET "/user_name_and_email/:user_id" []
                  :return schemas/User-name-and-email
                  :path-params [user_id :- s/Int]
                  :summary "Get user name and email"
                  :auth-rules access/admin
                  :current-user current-user
                  (ok (a/get-user-name-and-primary-email ctx user_id)))

             (GET "/user/:user_id" []
                  :return schemas/User
                  :path-params [user_id :- s/Int]
                  :summary "Get user name, profile image and emails"
                  :auth-rules access/admin
                  :current-user current-user
                  (ok (a/get-user ctx user_id)))

             (GET "/badge/:user_badge_id" []
                  :return schemas/Badge
                  :path-params [user_badge_id :- s/Int]
                  :summary "Get badge name, image and info"
                  :auth-rules access/admin
                  :current-user current-user
                  (ok (a/get-badge-modal ctx user_badge_id)))

             (GET "/badges/:badge_id" []
                  :return schemas/Badges
                  :path-params [badge_id :- s/Str]
                  :summary "Get badges name, image and info"
                  :auth-rules access/admin
                  :current-user current-user
                  (ok (a/get-public-badge-content-modal ctx badge_id (:id current-user))))

             (GET "/page/:page_id" []
                  :return schemas/Page
                  :path-params [page_id :- s/Int]
                  :summary "Get page name, image and info"
                  :auth-rules access/admin
                  :current-user current-user
                  (ok (a/get-page-modal ctx page_id)))

             (POST "/delete_badge/:user_badge_id" []
                   :return (s/enum "success" "error")
                   :summary "Delete badge"
                   :body-params [subject :- s/Str
                                 message :- s/Str
                                 user-id :- s/Int]
                   :path-params [user_badge_id :- s/Int]
                   :auth-rules access/admin
                   :current-user current-user
                   (ok (a/delete-badge! ctx user_badge_id user-id subject message)))

             (POST "/delete_badges/:badge_id" []
                   :return (s/enum "success" "error")
                   :summary "Delete badge"
                   :body-params [subject :- s/Str
                                 message :- s/Str
                                 user-id :- [(s/maybe s/Int)]]
                   :path-params [badge_id :- s/Str]
                   :auth-rules access/admin
                   :current-user current-user
                   (ok (a/delete-badges! ctx badge_id subject message)))

             (POST "/delete_page/:page_id" []
                   :return (s/enum "success" "error")
                   :summary "Delete page"
                   :body-params [subject :- s/Str
                                 message :- s/Str
                                 user-id :- s/Int]
                   :path-params [page_id :- s/Int]
                   :auth-rules access/admin
                   :current-user current-user
                   (ok (a/delete-page! ctx page_id user-id subject message)))

             (POST "/delete_user/:user_id" []
                   :return (s/enum "success" "error")
                   :summary "Delete user"
                   :body-params [subject :- s/Str
                                 message :- s/Str
                                 email   :- s/Str]
                   :path-params [user_id :- s/Int]
                   :auth-rules access/admin
                   :current-user current-user
                   (ok (a/delete-user! ctx user_id subject message email)))

             (POST "/full_delete_user/:id" []
                   :return (s/enum "success" "error")
                   :summary "Delete user"
                   :body-params [subject :- s/Str
                                 message :- s/Str
                                 email   :- s/Str]
                   :path-params [id :- s/Int]
                   :auth-rules access/admin
                   :current-user current-user
                   (ok (a/delete-user-full! ctx id subject message email)))

             (POST "/delete_no_activated_user/:user_id" []
                   :return (s/enum "success" "error")
                   :summary "Delete no activated user "
                   :path-params [user_id :- s/Int]
                   :auth-rules access/admin
                   :current-user current-user
                   (ok (a/delete-no-activated-user ctx user_id)))

             (POST "/delete_no_verified_address/:user_id" []
                   :return (s/enum "success" "error")
                   :summary "Delete users no verified address"
                   :body-params [email   :- s/Str]
                   :path-params [user_id :- s/Int]
                   :auth-rules access/admin
                   :current-user current-user
                   (ok (a/delete-no-verified-adress ctx user_id email)))

             (POST "/undelete_user/:id" []
                   :return (s/enum "success" "error")
                   :summary "Undelete user"
                   :path-params [id :- s/Int]
                   :auth-rules access/admin
                   :current-user current-user
                   (ok (a/undelete-user! ctx id)))

             (POST "/ticket" []
                   :return (s/enum "success" "error")
                   :summary "Create reporting ticket"
                   :body [content schemas/Report]
                   :auth-rules access/authenticated
                   :current-user current-user
                   (let [{:keys [description report_type item_id item_url item_name item_type reporter_id item_content_id]} content]
                     (ok (a/ticket ctx description report_type item_id item_url item_name item_type reporter_id item_content_id))))

             (GET "/tickets" []
                  :return [schemas/Ticket]
                  :summary "Get all tickets with open status"
                  :auth-rules access/admin
                  :current-user current-user
                  (do
                    (ok (a/get-tickets ctx))))

             (GET "/closed_tickets" []
                  :return [schemas/Closed_ticket]
                  :summary "Get all tickets with closed status"
                  :auth-rules access/admin
                  :current-user current-user
                  (do
                    (ok (a/get-closed-tickets ctx))))

             (GET  "/export_statistics" []
                   :summary "Export admin stats to csv format"
                   :auth-rules access/admin
                   :current-user current-user
                   (-> (io/piped-input-stream (a/export-admin-statistics ctx current-user))
                       ok
                       (header "Content-Disposition" (str "attachment; filename=\"statistics.csv\""))
                       (header "Content-Type" "text/csv")))

             (POST "/close_ticket/:id" []
                   :return (s/enum "success" "error")
                   :path-params [id :- s/Int]
                   :body-params [new-status :- (:status schemas/Ticket)]
                   :summary "Set ticket status to closed or open"
                   :auth-rules access/admin
                   :current-user current-user
                   (ok (a/close-ticket! ctx id new-status)))

             (POST "/send_activation_message/:id" []
                   :return (s/enum "success" "error")
                   :summary "Send activation message to user"
                   :path-params [id :- s/Int]
                   :auth-rules access/admin
                   :current-user current-user
                   (ok (a/send-user-activation-message ctx id)))


             (POST "/fake_session/:user_id" []
                   ;:return (s/enum "success" "error")
                   :summary "Login as other user"
                   :path-params [user_id :- s/Int]
                   :auth-rules access/admin
                   :current-user current-user
                   (a/set-fake-session ctx (ok {:status "success" :real-id (:id current-user)}) user_id (:id current-user)))


             (POST "/return_to_admin" []
                   ;:return (s/enum "success" "error")
                   :summary "Login back as admin"
                   :auth-rules access/signed
                   :current-user current-user
                   (if (:real-id current-user)
                     (a/set-session ctx (ok) (:real-id current-user))))


             (POST "/upgrade_user_to_admin/:id" []
                   ;:return (s/enum "success" "error")
                   :summary "Update user to admin"
                   :path-params [id :- s/Int]
                   :auth-rules access/admin
                   :current-user current-user
                   (ok (a/update-user-to-admin ctx id)))

             (POST "/downgrade_admin_to_user/:id" []
                   ;:return (s/enum "success" "error")
                   :summary "Update admin to user"
                   :path-params [id :- s/Int]
                   :auth-rules access/admin
                   :current-user current-user
                   (ok (a/update-admin-to-user ctx id)))


             (POST "/profiles" []
                   ;:return
                   #_{:users     [schemas/UserProfiles]
                      :countries [schemas/Countries]}

                   :body [search-params {:name     (s/maybe s/Str)
                                         :country  (s/maybe s/Str)
                                         :order_by (s/enum "name" "ctime" "common_badge_count")
                                         :email    (s/maybe s/Str)
                                         :filter   (s/enum 1 0)
                                         (s/optional-key :custom-field-filters) (s/maybe {(s/optional-key :gender) (s/maybe (s/enum "Male" "Female" "Notspecified" "notset"))
                                                                                          (s/optional-key :organization) (s/maybe s/Str)})}]
                   :summary "Get public user profiles"
                   :auth-rules access/admin
                   :current-user current-user

                   (let [users     (a/all-profiles ctx search-params (:id current-user))
                         countries (a/profile-countries ctx (:id current-user))
                         accepted-terms-fn (first (plugin-fun (get-plugins ctx) "db" "get-accepted-terms-by-id"))
                         users-with-terms (map #(-> %
                                                    (assoc :terms (:status (accepted-terms-fn ctx (:id %))))) users)
                         users-with-custom-field-filters (if (empty? (:custom-field-filters search-params))
                                                             (vec users-with-terms)
                                                             (a/apply-custom-filters ctx (:custom-field-filters search-params) (vec users-with-terms)))]
                     (ok {:users     (vec users-with-custom-field-filters)
                          :countries countries}))))))
