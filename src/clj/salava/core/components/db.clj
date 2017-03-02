(ns salava.core.components.db
  (:require [clojure.tools.logging :as log]
            [com.stuartsierra.component :as component]
            [hikari-cp.core :refer :all]))

(defrecord Db [config datasource]
  component/Lifecycle

  (start [component]
    (let [datasource (make-datasource (get-in config [:config :core :datasource]))]
      (log/info "hikari-cp started")
      (assoc component :datasource datasource)))

  (stop [component]
    (when datasource
      (log/info "hikari-cp close initiated...")
      (close-datasource datasource)
      (log/info "hikari-cp closed"))
    (assoc component :datasource nil)))

(defn create []
  (map->Db {}))
