(ns salava.factory.routes
  (:require [clojure.tools.logging :as log]
            [compojure.api.sweet :refer :all]
            [ring.util.http-response :refer :all]
            [ring.util.response :refer [redirect]]
            [schema.core :as s]
            [salava.core.layout :as layout]
            [clojure.string :refer [split]]
            [salava.core.util :as u]
            [salava.factory.db :as f]))

(defn wrap-basic-auth [handler ctx]
  (fn [request]
    (let [auth-header (get-in request [:headers "authorization"] "")
          api-key (get-in ctx [:config :factory :key])
          api-secret (get-in ctx [:config :factory :secret])
          credentials (if auth-header (u/base64->str (str (last (re-find #"^Basic (.*)$" auth-header)))))
          [key pass] (if credentials (split (str credentials) #":" 2))]
      (if (or (empty? key) (empty? pass))
        (unauthorized)
        (if-not (and (= key api-key) (= pass api-secret))
          (forbidden)
          (handler request))))))

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
                  (if-let [user-badge-id (f/receive-badge ctx e k t)]
                    (-> (str (u/get-base-path ctx) (str "/badge/receive/" user-badge-id))
                        redirect
                        (assoc-in [:session :pending] {:user-badge-id user-badge-id}))
                    (not-found "404 Not Found")))

             (POST "/backpack_email_list" []
                   :header-params [authorization :- s/Str]
                   :body-params [emails :- [s/Str]]
                   :middleware [#(wrap-basic-auth % ctx)]
                   (ok (f/get-user-emails ctx emails)))

             (POST "/users_badges" []
                   :header-params [authorization :- s/Str]
                   :body-params [assertions :- s/Any]
                   :middleware [#(wrap-basic-auth % ctx)]
                   (let [result (f/save-assertions-for-emails ctx assertions)]
                     (if result
                       (ok {:success true})
                       (internal-server-error {:error "transaction failed"}))))

             (GET "/get_updates" []
                  :query-params [user :- s/Int
                                 badge :- s/Int]
                  (ok (f/get-badge-updates ctx user badge))))))
