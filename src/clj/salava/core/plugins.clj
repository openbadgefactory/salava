(ns salava.core.plugins
  (:require [compojure.api.sweet :refer :all]))

(defmacro defplugins [def-name & body]
  (let [router (map #(symbol (str "salava." (name %) ".routes/route-def")) body)]
    `(def ~def-name {:plugins [~@body]
                     :routes (fn [~'ctx]
                               (api {:components {:context ~'ctx}}
                                    (swagger-ui "/swagger-ui")
                                    (swagger-docs {:info  {:version "0.1.0"
                                                           :title "Salava REST API"
                                                           :description ""
                                                           :contact  {:name "Discendum Oy"
                                                                      :email "contact@openbadgepassport.com"
                                                                      :url "http://salava.org"}
                                                           :license  {:name "Apache 2.0"
                                                                      :url "http://www.apache.org/licenses/LICENSE-2.0"}}
                                                   :tags  [{:name "badge", :description "plugin"}
                                                           {:name "file", :description "plugin"}
                                                           {:name "gallery", :description "plugin"}
                                                           {:name "page", :description "plugin"}
                                                           {:name "translator", :description "plugin"}
                                                           {:name "user", :description "plugin"}]})
                                    ~@router
                                    salava.core.routes/route-def))})))
