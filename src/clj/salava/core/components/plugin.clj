(ns salava.core.components.plugin
  (:require [com.stuartsierra.component :as component]
            [salava.core.common :as common]))


(defn plugin-routes [plugin context]
  (let [router (symbol (str "salava." (name plugin) ".routes/routes"))]
    (require (symbol (namespace router)) :reload)
    ((resolve router) context)))


(defn collect-routes [plugins context]
  (let [coll (apply common/deep-merge (map #(plugin-routes % context) plugins))]
    (common/deep-merge coll (plugin-routes :core context))))


(defrecord Plugin [config db routes]
  component/Lifecycle

  (start [this]
    (let [context         {:config (:config config) :dbconn (:dbconn db)}
          enabled-plugins (get-in config [:config :core :plugins])
          route-map       (collect-routes enabled-plugins context)]
      (assoc this :routes ["" route-map])))

  (stop [this]
    (assoc this :routes nil)))


(defn create []
  (map->Plugin {}))
