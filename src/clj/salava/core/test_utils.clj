(ns salava.core.test-utils
  (:require [clojure.test]
            [clj-http.client :as client]
            [clojure.tools.logging :refer  [*logger-factory*]]
            [clojure.tools.logging.impl :refer  [disabled-logger-factory]]
            [clj-http.cookies :as cookies]
            [ring.mock.request :as mock]
            [com.stuartsierra.component :as component]
            [salava.core.system]
            [clojure.string :refer [split]]
            [salava.core.migrator :as migrator]
            [cheshire.core :as cheshire]
            [slingshot.slingshot :refer :all]))


(defn get-system []
  (binding [*logger-factory* disabled-logger-factory]
    (-> (salava.core.system/base-system "test_config")
        (dissoc :http-server :cron)
        (component/start))))

(defn stop-system [system]
   (binding [*logger-factory* disabled-logger-factory]
      (component/stop system)))


(defn parse-body [body]
  (if body
    (try
      (cheshire/parse-string (slurp body) true)
      (catch Exception _
        body))))

(defn test-api-request
  ([ctx method url] (test-api-request ctx method url {}))
  ([ctx method url opt]
   (let [handler (get-in (meta ctx) [:system :handler :handler])
         base (get-in ctx [:config :core :base-path])]
     (-> (mock/request method (str base url))
         (mock/content-type "application/json")
         (mock/body (cheshire/generate-string (:params opt)))
         (assoc :identity (:user opt))
         handler
         (update :body parse-body)))))


(defmacro deftest-ctx
  [name bind & body]
  `(clojure.test/deftest ~name
     (let [system# (get-system)
           ~(first bind) (with-meta {:config     (get-in system# [:config :config])
                                     :db         (get-in system# [:db :datasource])
                                     :input-chan (get-in system# [:pubsub :channel :input-chan])}
                                    {:system system#})]
       (try
         ~@body
         (finally
           (stop-system system#))))))

#_(migrator/run-test-reset)
