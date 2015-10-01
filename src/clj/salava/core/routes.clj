(ns salava.core.routes
  (:require [salava.core.layout :as layout]
            [ring.util.response :as r]))

(def not-found (constantly (-> (r/response "404 Not Found")
                               (r/status 404)
                               (r/header "Content-type" "text/plain; charset=\"UTF-8\""))))

(defn routes [ctx]
  (let [get-main {:get (partial layout/main (assoc-in ctx [:active-plugin] "core"))}]
    {"/" [[""   get-main]
          [true not-found]]}))
