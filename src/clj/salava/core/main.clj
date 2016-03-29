(ns salava.core.main
  (:gen-class)
  (:require [clojure.tools.cli :refer  [parse-opts]]
            [clojure.java.io :as io]
            [salava.core.system]
            [salava.core.migrator :refer [migrate-all]]))

(def cli-options
  [["-c" "--config PATH" "Absolute path to config files"
    :validate  [#(.isDirectory  (io/file %)) "Must be a readable path"]]])

(defn run-action  [action opts]
  (case action
    "migrate" (migrate-all  (:config opts))
    "start"   (salava.core.system/start-base-system (:config opts))
    (println "no action")))

(defn -main  [& args]
  (let  [{:keys  [options arguments errors]}  (parse-opts args cli-options)
         errors  (if  (nil?  (:config options))  (cons "--config must be specified" errors) errors)]
    (if  (nil? errors)
      (run-action  (first arguments) options)
      (println "fatal:"  (last errors)))))
