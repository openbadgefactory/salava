(ns salava.gallery.routes
  (:require [salava.core.layout :as layout]))


(defn routes [ctx]
  (let [get-main {:get (partial layout/main (assoc-in ctx [:active-plugin] "gallery"))}]
    {"/gallery" [["/"          get-main]
                 ["/badges/"   get-main]
                 ["/pages/"    get-main]
                 ["/profiles/" get-main]
                 ["/getbadge/" get-main]]}))

