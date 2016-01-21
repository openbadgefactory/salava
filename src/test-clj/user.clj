(ns user
  (:require [reloaded.repl :refer [system init start stop go reset]]
            [clojure.tools.namespace.repl :refer [set-refresh-dirs]]
            [salava.core.migrator :refer [run-reset]]
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
  (let [plugins (cons :core [:badge :page :gallery :file :user])] ;TODO load from config
    (doseq [plugin plugins]
      (run-reset true plugin))))

(defn run-tests []
  (migration-reset)
  (load-facts))
