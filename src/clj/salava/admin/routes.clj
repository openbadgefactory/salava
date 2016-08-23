(ns salava.admin.routes
  (:require [compojure.api.sweet :refer :all]
            [ring.util.http-response :refer :all]
            [ring.util.response :refer [redirect]]
            [salava.core.layout :as layout]
            [schema.core :as s]
            [salava.core.util :refer [get-base-path]]
            [salava.admin.db :as a]
            [salava.core.access :as access]
            [salava.admin.schemas :as schemas]
            salava.core.restructure))


(defn route-def [ctx]
  (routes
    (context "/admin" []
             (layout/main ctx "/")
             (layout/main ctx "/tickets")
             (layout/main ctx "/statistics"))

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
                                 message :- s/Str]
                   :path-params [user_id :- s/Int]
                   :auth-rules access/admin
                   :current-user current-user
                   (ok (a/send-message ctx user_id subject message)))

             (GET "/user_name_and_email/:user_id" []
                  :return schemas/User-name-and-email
                  :path-params [user_id :- s/Int]
                  :summary "Get user name and email"
                  :auth-rules access/admin
                  :current-user current-user
                  (ok (a/get-user-name-and-primary-email ctx user_id)))

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
                                 user-id :- (s/maybe s/Int)]
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
                                 message :- s/Str]
                   :path-params [id :- s/Int]
                   :auth-rules access/admin
                   :current-user current-user
                   (ok (a/delete-user! ctx id subject message)))

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

             (POST "/close_ticket/:id" []
                   :return (s/enum "success" "error")
                   :path-params [id :- s/Int]
                   :summary "Set ticket status to closed"
                   :auth-rules access/admin
                   :current-user current-user
                   (ok (a/close-ticket! ctx id)))

             )))
