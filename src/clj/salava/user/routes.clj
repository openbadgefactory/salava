(ns salava.user.routes
  (:require [salava.core.layout :as layout]))


(defn routes [ctx]
  (let [get-main {:get (partial layout/main (assoc-in ctx [:active-plugin] "user"))}]
    {"/user" [["/login/"   get-main]
              ["/account/" get-main]]}))
