(ns salava.core.main
  (:gen-class)
  (:require [salava.core.system]
            [salava.core.migrator :refer [migrate]]))

(defn -main [& args]
  (if (= "migrate" (first args))
    (migrate false)
    (salava.core.system/start-base-system)))
