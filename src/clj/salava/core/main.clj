(ns salava.core.main
  (:gen-class)
  (:require [salava.core.system]))

(defn -main [& args]
  (salava.core.system/start-base-system))
