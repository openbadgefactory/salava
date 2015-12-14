(ns salava.core.plugins
  (:require [compojure.api.sweet :refer :all]))

(defmacro defplugins [def-name & body]
  (let [router (map #(symbol (str "salava." (name %) ".routes/route-def")) body)]
    `(def ~def-name {:plugins [~@body]
                     :routes (fn [~'ctx]
                               (api {:components {:context ~'ctx}}
                                    (swagger-ui "/swagger-ui")
                                    (swagger-docs {:info {:title "Salava public API" :description ""}})
                                    ~@router
                                    salava.core.routes/route-def))})))
