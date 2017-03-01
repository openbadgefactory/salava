(ns salava.core.components.handler
  (:require [com.stuartsierra.component :as component]
            [salava.core.helper :refer [dump]]
            [salava.core.handler]))

(defrecord Handler [config db pubsub handler]
  component/Lifecycle

  (start [this]
    (let [context {:config (:config config)
                   :db     (:datasource db)
                   :input-chan (get-in pubsub [:channel :input-chan])}]
      (assoc this :handler (salava.core.handler/handler context)
                  :ctx context)))

  (stop [this]
    (assoc this :handler nil)))


(defn create []
  (map->Handler {}))
