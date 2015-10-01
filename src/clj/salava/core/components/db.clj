(ns salava.core.components.db
  (:require [com.stuartsierra.component :as component]
            [hikari-cp.core :refer :all]))

(defrecord Db [config dbconn]
  component/Lifecycle

  (start [component]
    (let [datasource (make-datasource (get-in config [:config :core :datasource]))]
      (assoc component :dbconn datasource)))

  (stop [component]
    (if dbconn
      (close-datasource dbconn))
    (assoc component :dbconn nil)))

(defn create []
  (map->Db {}))
