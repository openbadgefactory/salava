(ns user
  (:require [reloaded.repl :refer [system init start stop go clear reset]]
            [salava.core.migrator :refer [run-test-reset]]
            [midje.repl :refer [load-facts]]
            [clojure.tools.namespace.repl :refer [set-refresh-dirs]]))

(set-refresh-dirs "./src")

(defonce test-mode? (atom false))

(def dev-config  "config")
(def test-config "test_config")

(defn set-opts []
  (let [system 'salava.core.system/base-system
        config-path (if @test-mode? test-config dev-config)]
  (reloaded.repl/set-init!
    (fn []
      (require (symbol (namespace system)))
      ((resolve system) config-path)))))


(defn toggle-test-mode []
  (reset! test-mode? (not @test-mode?))
  (println (str "IN TEST MODE: " @test-mode?))
  (stop)
  (set-opts)
  (go))


(defn ensure-test-mode []
  (if-not @test-mode? (toggle-test-mode)))


(defn run-tests []
  (ensure-test-mode)
  (run-test-reset)
  (load-facts))

(defn do-test []
  (run-tests)
  (stop)
  (System/exit 0))

(set-opts)
