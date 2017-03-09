(ns salava.core.components.server
  (:require [clojure.tools.logging :as log]
            [org.httpkit.server :as http-kit]
            [com.stuartsierra.component :as component]))

(defrecord Server [config handler http-kit]
  component/Lifecycle

  (start [this]
    (let [http-config   (get-in config [:config :core :http])
          port          (or (System/getenv "SALAVA_PORT") (:port http-config))
          ring-handler  (get-in handler [:handler])
          server        (http-kit/run-server ring-handler (assoc http-config :port (Integer. port)))]
      (log/info (str "http-kit application server started at " (:host http-config) ":" port))
      (assoc this :http-kit server)))

  (stop [this]
    (when http-kit
      (log/info "Stopping http-kit...")
      (http-kit :timeout 10000)
      (log/info "Stopped"))
    (assoc this :http-kit nil)))

(defn create []
  (map->Server {}))
