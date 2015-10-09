(ns salava.core.system
  (:require [com.stuartsierra.component :as component]
            [salava.core.components.config  :as config]
            [salava.core.components.db      :as db]
            [salava.core.components.handler :as handler]
            [salava.core.components.server  :as server]))


(def base {:db          (component/using (db/create)      [:config])
           :handler     (component/using (handler/create) [:config :db])
           :http-server (component/using (server/create)  [:config :handler])})


(defn base-system []
  (component/map->SystemMap (assoc base :config (config/create))))

(defn start-base-system []
  (component/start (base-system)))
