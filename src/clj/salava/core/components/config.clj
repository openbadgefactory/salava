(ns salava.core.components.config
  (:require [com.stuartsierra.component :as component]
            [clojure.java.io :as io]
            [salava.registry]))


(defn load-config [plugin]
  (let [config-file (io/resource (str "config/" (name plugin) ".edn"))]
    (if-not (nil? config-file)
      (-> config-file slurp read-string))))


(defrecord Config [config]
  component/Lifecycle

  (start [this]
    (let [enabled (:plugins salava.registry/enabled) 
          core-conf (assoc (load-config :core) :plugins enabled)
          config (reduce #(assoc %1 %2 (load-config %2)) {} enabled)]

    (assoc this :config (assoc config :core core-conf))))

  (stop [this]
    (assoc this :config nil)))

(defn create []
  (map->Config {}))
