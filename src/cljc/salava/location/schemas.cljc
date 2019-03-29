(ns salava.location.schemas
  (:require [schema.core :as s
             :include-macros true ;; cljs only
             ]
            ))


(def Lat (s/constrained s/Num (fn [n] (and (>= n -90)  (<= n 90)))))
(def Lng (s/constrained s/Num (fn [n] (and (>= n -180)  (<= n 180)))))

(s/defschema explore-input {:_       s/Any
                            :max_lat Lat
                            :max_lng Lng
                            :min_lat Lat
                            :min_lng Lng
                            })


