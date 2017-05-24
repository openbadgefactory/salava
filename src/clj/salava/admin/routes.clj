(ns salava.admin.routes
  (:require [compojure.api.sweet :refer :all]
            [ring.util.http-response :refer :all]
            [ring.util.response :refer [redirect]]
            [salava.core.layout :as layout]
            [schema.core :as s]
            [salava.core.util :refer [get-base-path]]
            [salava.admin.db :as a]
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
             (layout/main ctx "/userlist"))

    (context "/obpv1/admin" []
             :tags ["admin"]

             (GET "/stats" []
                   :return schemas/Stats
                   :summary "Get statistics"
                   :auth-rules access/admin
                   :current-user current-user
                   (do
                     (ok (a/get-stats ctx))))
             
             (POST "/private_badge/:id" []
                   :return (s/enum "success" "error")
                   :path-params [id :- s/Int]
                   :summary "Set badge to private"
                   :auth-rules access/admin
                   :current-user current-user
                   (ok (a/private-badge! ctx id)))

             (POST "/private_badges/:id" []
                   :return (s/enum "success" "error")
                   :path-params [id :- s/Str]
                   :summary "Set badges from gallery to private"
                   :auth-rules access/admin
                   :current-user current-user
                   (ok (a/private-badges! ctx id)))
             
             (POST "/private_page/:id" []
                   :return (s/enum "success" "error")
                   :path-params [id :- s/Int]
                   :summary "Set page to private"
                   :auth-rules access/admin
                   :current-user current-user
                   (ok (a/private-page! ctx id)))
             
             (POST "/private_user/:id" []
                   :return (s/enum "success" "error")
                   :path-params [id :- s/Int]
                   :summary "Set user to private"
                   :auth-rules access/admin
                   :current-user current-user
                   (ok (a/private-user! ctx id)))

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

             (GET "/badge/:id" []
                  :return schemas/Badge
                  :path-params [id :- s/Int]
                  :summary "Get badge name, image and info"
                  :auth-rules access/admin
                  :current-user current-user
                  (ok (a/get-badge-modal ctx id)))

             (GET "/badges/:id" []
                  :return schemas/Badges
                  :path-params [id :- s/Str]
                  :summary "Get badges name, image and info"
                  :auth-rules access/admin
                  :current-user current-user
                  (ok (a/get-public-badge-content-modal ctx id)))
             
             (GET "/page/:id" []
                  :return schemas/Page
                  :path-params [id :- s/Int]
                  :summary "Get page name, image and info"
                  :auth-rules access/admin
                  :current-user current-user
                  (ok (a/get-page-modal ctx id)))

             (POST "/delete_badge/:id" []
                   :return (s/enum "success" "error")
                   :summary "Delete badge"
                   :body-params [subject :- s/Str
                                 message :- s/Str
                                 user-id :- s/Int]
                   :path-params [id :- s/Int]
                   :auth-rules access/admin
                   :current-user current-user
                   (ok (a/delete-badge! ctx id user-id subject message)))

             (POST "/delete_badges/:id" []
                   :return (s/enum "success" "error")
                   :summary "Delete badge"
                   :body-params [subject :- s/Str
                                 message :- s/Str
                                 user-id :- [(s/maybe s/Int)]]
                   :path-params [id :- s/Str]
                   :auth-rules access/admin
                   :current-user current-user
                   (ok (a/delete-badges! ctx id subject message)))

             (POST "/delete_page/:id" []
                   :return (s/enum "success" "error")
                   :summary "Delete page"
                   :body-params [subject :- s/Str
                                 message :- s/Str
                                 user-id :- s/Int]
                   :path-params [id :- s/Int]
                   :auth-rules access/admin
                   :current-user current-user
                   (ok (a/delete-page! ctx id  user-id subject message)))

             (POST "/delete_user/:id" []
                   :return (s/enum "success" "error")
                   :summary "Delete user"
                   :body-params [subject :- s/Str
                                 message :- s/Str
                                 email   :- s/Str]
                   :path-params [id :- s/Int]
                   :auth-rules access/admin
                   :current-user current-user
                   (ok (a/delete-user! ctx id subject message email)))

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
                     (ok (a/ticket ctx description report_type item_id item_url item_name item_type reporter_id item_content_id) )))

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

             (POST "/profiles" []
                   ;:return
                   #_{:users     [schemas/UserProfiles]
                    :countries [schemas/Countries]
                    }
                   :body [search-params {:name     (s/maybe s/Str)
                                         :country  (s/maybe s/Str)
                                         :order_by (s/enum "name" "ctime" "common_badge_count")
                                         :email    (s/maybe s/Str)
                                         :filter   (s/enum 1 0)}]
                   :summary "Get public user profiles"
                   :auth-rules access/admin
                   :current-user current-user
                   
                   (let [users     (a/all-profiles ctx search-params (:id current-user))
                         countries (a/profile-countries ctx (:id current-user))]
                     (ok {:users     (vec users)
                          :countries countries
                          }))))))
