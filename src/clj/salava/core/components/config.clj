(ns salava.core.components.config
  (:require [clojure.java.io :as io]
            [com.stuartsierra.component :as component]))


(defn load-config [plugin]
  (let [config-file (io/resource (str "config/" (name plugin) ".edn"))]
    (if-not (nil? config-file)
      (-> config-file slurp read-string))))


(defrecord Config [config]
  component/Lifecycle

  (start [this]
    (let [core-conf (load-config :core)
          enabled (:plugins core-conf)
          config (reduce #(assoc %1 %2 (load-config %2)) {} enabled)]

    (assoc this :config (assoc config :core core-conf))))

  (stop [this]
    (assoc this :config nil)))

(defn create []
  (map->Config {}))
