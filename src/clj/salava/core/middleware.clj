(ns salava.core.middleware
  (:require [clojure.tools.logging :as log]
            [salava.core.util :as u]
            [clojure.string :refer [split]]
            [ring.util.http-response :refer :all]
            )) 

(defn wrap-factory-auth [handler ctx]
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
