(ns salava.extra.factory.routes
  (:require [compojure.api.sweet :refer :all]
            [ring.util.http-response :refer :all]
            [schema.core :as s]
            [salava.core.layout :as layout]
            [clojure.string :refer [split]]
            [salava.core.util :refer [base64->str]]
            [salava.extra.factory.db :as f]))

(defn wrap-basic-auth [handler ctx]
  (fn [request]
    (let [auth-header (get-in request [:headers "authorization"] "")
          api-key (get-in ctx [:config :core :obf :key])
          api-secret (get-in ctx [:config :core :obf :secret])
          credentials (if auth-header (base64->str (str (last (re-find #"^Basic (.*)$" auth-header)))))
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
            :no-doc true

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
