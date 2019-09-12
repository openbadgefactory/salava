(ns salava.factory.routes
  (:require [clojure.tools.logging :as log]
            [compojure.api.sweet :refer :all]
            [ring.util.http-response :refer :all]
            [ring.util.response :refer [redirect]]
            [schema.core :as s]
            [salava.core.layout :as layout]
            [salava.core.middleware :as mw]
            [salava.core.util :as u]
            [salava.factory.db :as f]))


(defn route-def [ctx]
  (routes
    (context "/obpv1/factory" []
             :tags ["factory"]

             (HEAD "/receive" []
                  :no-doc true
                  :summary "Capability check for GET /receive"
                  :query-params [e :- String
                                 k :- String
                                 t :- String]
                  (ok ""))

             (GET "/receive" []
                  :no-doc true
                  :summary "Receive new badges from OBF"
                  :query-params [e :- String
                                 k :- String
                                 t :- String]
                  (let [badge-info (f/receive-badge-json ctx e k t)]
                    (if-let [user-badge-id (f/receive-badge ctx badge-info)]
                      (-> (str (u/get-base-path ctx) (str "/badge/receive/" user-badge-id "?banner=" (f/receive-banner (:banner badge-info))))
                          redirect
                          (assoc-in [:session :pending] {:user-badge-id user-badge-id :email e}))
                      (not-found "404 Not Found"))))

             (DELETE "/receive/:id" req
                     :no-doc true
                     :summary "Receive new badges from OBF"
                     :path-params [id :- s/Int]
                     (when (= id (get-in req [:session :pending :user-badge-id]))
                       (ok (f/reject-badge! ctx id))))

             (POST "/backpack_email_list" []
                   :header-params [authorization :- s/Str]
                   :body-params [emails :- [s/Str]]
                   :middleware [#(mw/wrap-factory-auth % ctx)]
                   (ok (f/get-user-emails ctx emails)))

             (POST "/users_badges" []
                   :header-params [authorization :- s/Str]
                   :body-params [assertions :- s/Any]
                   :middleware [#(mw/wrap-factory-auth % ctx)]
                   (let [result (f/save-assertions-for-emails ctx assertions)]
                     (if result
                       (ok {:success true})
                       (internal-server-error {:error "transaction failed"}))))

             (GET "/get_updates" []
                  :query-params [user :- s/Int
                                 badge :- s/Int]
                  (ok (f/get-badge-updates ctx user badge))))))
