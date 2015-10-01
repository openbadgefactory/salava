(ns salava.badge.routes
  (:require [salava.core.layout :as layout]))


(defn routes [ctx]
  (let [get-main {:get (partial layout/main (assoc-in ctx [:active-plugin] "badge"))}]
    {"/badge" [["/"            get-main]
               [["/show/" :id] get-main]
               ["/import/" get-main]
               ["/upload/" get-main]
               ["/export/" get-main]
               ["/stats/"  get-main]]}))

