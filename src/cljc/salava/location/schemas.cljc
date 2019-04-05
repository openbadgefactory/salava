(ns salava.location.schemas
  (:require [schema.core :as s
             :include-macros true ;; cljs only
             ]
             [schema.coerce :as c]
            ))

(defn between [min max]
  (fn [n]
    #?(:clj
       (try
         (<= min (Float. n) max)
         (catch Exception _ false))
       :cljs
       (<= min (js/parseFloat n) max))))

(def Lat (s/constrained #?(:clj  (s/either s/Num s/Str)
                           :cljs (s/cond-pre s/Num s/Str)) (between -90 90) (list 'between -90 90)))

(def Lng (s/constrained #?(:clj  (s/either s/Num s/Str)
                           :cljs (s/cond-pre s/Num s/Str)) (between -180 180) (list 'between -180 180)))

(s/defschema explore-user-query {:max_lat Lat
                                 :max_lng Lng
                                 :min_lat Lat
                                 :min_lng Lng
                                 (s/optional-key :user_name) s/Str
                                 })

(s/defschema explore-badge-query {:max_lat Lat
                                  :max_lng Lng
                                  :min_lat Lat
                                  :min_lng Lng
                                  (s/optional-key :tag_name) s/Str
                                  (s/optional-key :badge_name) s/Str
                                  (s/optional-key :issuer_name) s/Str
                                  })

(s/defschema lat-lng {:lat (s/maybe Lat) :lng (s/maybe Lng)})

(s/defschema success {:success s/Bool})

(s/defschema self-location {:enabled (s/maybe lat-lng)
                            :country lat-lng
                            :public s/Bool})


(s/defschema user-location {:id s/Int
                            :user_url s/Str
                            :lat Lat
                            :lng Lng})

(s/defschema badge-location {:id s/Int
                             :user_id s/Int
                             :badge_id s/Str
                             :badge_url s/Str
                             :lat Lat
                             :lng Lng})

(s/defschema explore-badges {:badges [badge-location]})

(s/defschema explore-users  {:users  [user-location]})

(s/defschema explore-filters {:tag_name    [s/Str]
                              :badge_name  [s/Str]
                              :issuer_name [s/Str]
                              })
