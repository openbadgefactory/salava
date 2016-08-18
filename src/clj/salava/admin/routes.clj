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
