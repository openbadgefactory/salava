(ns salava.core.components.plugin
  (:require [com.stuartsierra.component :as component]))


(defn plugin-routes [plugin]
  (let [router (symbol (str "salava." (name plugin) ".routes/route-def"))]
    (require (symbol (namespace router)) :reload)
    (resolve router)))


(defn collect-routes [plugins]
  (map #(plugin-routes %) (conj plugins :core)))

(defrecord Plugin [config db context routes]
  component/Lifecycle

  (start [this]
    (let [context {:config (:config config) :db (:dbconn db)}
          enabled-plugins (get-in config [:config :core :plugins])]
      (assoc this :routes (collect-routes enabled-plugins) :context context)))

  (stop [this]
    (assoc this :routes nil :context nil)))

(defn create []
  (map->Plugin {}))
