(ns salava.core.system
  (:require [com.stuartsierra.component :as component]
            [salava.core.components.config  :as config]
            [salava.core.components.db      :as db]
            [salava.core.components.test-db :as test-db]
            [salava.core.components.handler :as handler]
            [salava.core.components.server  :as server]))


(def base {:config      (config/create)
           :db          (component/using (db/create)      [:config])
           :handler     (component/using (handler/create) [:config :db])
           :http-server (component/using (server/create)  [:config :handler])})

(defn base-system
  ([] (base-system {}))
  ([opts]
   (component/map->SystemMap (merge base opts))))

(defn test-system []
  (base-system {:db (component/using (test-db/create) [:config])}))

(defn start-base-system []
  (component/start (base-system)))
