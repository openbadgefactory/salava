(ns salava.core.system
  (:require [clojure.tools.logging :as log]
            [com.stuartsierra.component :as component]
            [salava.core.components.config  :as config]
            [salava.core.components.db      :as db]
            [salava.core.components.cron    :as cron]
            [salava.core.components.pubsub  :as pubsub]
            [salava.core.components.handler :as handler]
            [salava.core.components.server  :as server]))


(def base {:db          (component/using (db/create)      [:config])
           :cron        (component/using (cron/create)    [:config :db])
           :pubsub      (component/using (pubsub/create)  [:config :db])
           :handler     (component/using (handler/create) [:config :db :pubsub])
           :http-server (component/using (server/create)  [:config :handler])})

(defn base-system [config-path]
  (component/map->SystemMap (assoc base :config (config/create config-path))))

(defn start-base-system [config-path]
  (let [system (component/start (base-system config-path))
        stop-system (fn []
                      (log/info "Shutting down...")
                      (component/stop system)
                      (log/info "Done"))]
    (.addShutdownHook (Runtime/getRuntime) (Thread. stop-system))
    system))
