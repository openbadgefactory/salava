(ns salava.file.routes
  (:require [salava.core.layout :as layout]))


(defn routes [ctx]
  (let [get-main {:get (partial layout/main (assoc-in ctx [:active-plugin] "file"))}]
    {"/pages" [["/files/" get-main]]}))

