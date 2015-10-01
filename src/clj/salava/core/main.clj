(ns salava.core.main
  (:gen-class)
  (:require [salava.core.system]
            [clojure.string :as str]
            ))

(defn -main [& args]
  (salava.core.system/start-base-system))
