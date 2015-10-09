(ns salava.core.components.db
  (:require [com.stuartsierra.component :as component]
            [hikari-cp.core :refer :all]))

(defrecord Db [config datasource]
  component/Lifecycle

  (start [component]
    (let [datasource (make-datasource (get-in config [:config :core :datasource]))]
      (assoc component :datasource datasource)))

  (stop [component]
    (if datasource
      (close-datasource datasource))
    (assoc component :datasource nil)))

(defn create []
  (map->Db {}))
