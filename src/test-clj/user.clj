(ns user
  (:require [reloaded.repl :refer [system init start stop go reset]]
            [clojure.tools.namespace.repl :refer [set-refresh-dirs]]
            [salava.core.migrator :refer [run-reset]]
            [salava.registry]
            [midje.repl :refer [load-facts]]))

(set-refresh-dirs "./src")

(defn set-opts [& {:keys [system opts]
                   :or {system 'salava.core.system/test-system
                        opts {}}}]
  (reloaded.repl/set-init!
    (fn []
      (require (symbol (namespace system)))
      ((resolve system)))))

(set-opts)

(defn migration-reset []
  (let [plugins (cons :core (:plugins salava.registry/enabled))]
    (doseq [plugin plugins]
      (run-reset true plugin))))

(defn run-tests []
  (migration-reset)
  (load-facts))