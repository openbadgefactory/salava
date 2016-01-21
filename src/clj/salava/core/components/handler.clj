(ns salava.core.components.handler
  (:require [com.stuartsierra.component :as component]
            [salava.core.helper :refer [dump]]
            [salava.core.handler]))

(defrecord Handler [config db handler]
  component/Lifecycle

  (start [this]
    (let [context {:config (:config config)
                   :db     (:datasource db)}]
      (assoc this :handler (salava.core.handler/handler context))))

  (stop [this]
    (assoc this :handler nil)))


(defn create []
  (map->Handler {}))
