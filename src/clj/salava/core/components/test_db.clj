(ns salava.core.components.test-db
  (:require [com.stuartsierra.component :as component]
            [hikari-cp.core :refer :all]))

(defrecord TestDb [config datasource]
  component/Lifecycle

  (start [component]
    (let [datasource (make-datasource (get-in config [:config :core :test-datasource]))]
      (assoc component :datasource datasource)))

  (stop [component]
    (if datasource
      (close-datasource datasource))
    (assoc component :datasource nil)))

(defn create []
  (map->TestDb {}))

