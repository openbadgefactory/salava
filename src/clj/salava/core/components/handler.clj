(ns salava.core.components.handler
  (:require [com.stuartsierra.component :as component]
            [salava.core.handler]))


(defrecord Handler [config plugin handler]
  component/Lifecycle

  (start [this]
    (assoc this :handler (salava.core.handler/handler (:core config) (:routes plugin))))

  (stop [this]
    this))


(defn create []
  (map->Handler {}))
