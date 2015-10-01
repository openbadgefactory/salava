(ns salava.page.routes
  (:require [salava.core.layout :as layout]))


(defn routes [ctx]
  (let [get-main {:get (partial layout/main (assoc-in ctx [:active-plugin] "page"))}]
    {"/pages" [["/"         get-main]
               ["/mypages/" get-main]]}))

