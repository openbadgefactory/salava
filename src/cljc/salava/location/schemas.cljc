(ns salava.location.schemas
  #?(:clj (:require [schema.core :as s]
                    [schema.coerce :as c]
                    [compojure.api.sweet :refer [describe]])
     :cljs (:require [schema.core :as s :include-macros true]
                     [schema.coerce :as c])))

#?(:cljs (defn describe [v _] v))

(defn between [min max]
  (fn [n]
    #?(:clj
       (try
         (<= min (Float. n) max)
         (catch Exception _ false))
       :cljs
       (<= min (js/parseFloat n) max))))

(def Lat (describe
           (s/constrained #?(:clj  (s/either s/Num s/Str)
                             :cljs (s/cond-pre s/Num s/Str)) (between -90 90) (list 'between -90 90))
           "Latitude, number between -90 and 90."))

(def Lng (describe
           (s/constrained #?(:clj  (s/either s/Num s/Str)
                             :cljs (s/cond-pre s/Num s/Str)) (between -180 180) (list 'between -180 180))
           "Longitude, number between -180 and 180."))

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


(s/defschema user-location {:id         (describe s/Int "internal user id")
                            :user_url   (describe s/Str "public user profile url")
                            :first_name (describe s/Str "user's first name")
                            :last_name  (describe s/Str "user's last name")
                            :lat        (describe Lat   "Latitude coordinate")
                            :lng        (describe Lng   "Longitude coordinate")})

(s/defschema badge-location {:id s/Int
                             :user_id s/Int
                             :badge_id s/Str
                             :lat Lat
                             :lng Lng})

(s/defschema badge-location-ex {:id          (describe s/Int "internal badge id")
                                :user_id     (describe s/Int "internal badge earner id")
                                :badge_id    (describe s/Str "internal badge content id")
                                :badge_url   (describe s/Str "public badge url")
                                :badge_name  (describe s/Str "badge name")
                                :badge_image (describe s/Str "badge image url")
                                :issuer_name (describe s/Str "badge issuer name")
                                :lat         (describe Lat   "Latitude coordinate")
                                :lng         (describe Lng   "Longitude coordinate")})

(s/defschema explore-badges {:badges [badge-location]})

(s/defschema explore-badges-ex {:badges [badge-location-ex]})

(s/defschema explore-users  {:users  [user-location]})

(s/defschema explore-filters {:tag_name    (describe [s/Str] "List of tags available in public badges")
                              :badge_name  (describe [s/Str] "List of public badge names")
                              :issuer_name (describe [s/Str] "List of public badge issuer names")})
