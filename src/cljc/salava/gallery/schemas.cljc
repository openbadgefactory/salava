(ns salava.gallery.schemas
  (:require [schema.core :as s
             :include-macros true ;; cljs only
             ]
            [salava.core.countries :refer [all-countries]]
            [salava.user.schemas :as u]))

(s/defschema UserSearch {:name          (s/constrained s/Str #(and (>= (count %) 0)
                                                                   (<= (count %) 255)))
                         :country       (apply s/enum (conj (keys all-countries) "all"))
                         :common_badges s/Bool
                         :order_by      (s/enum "name" "ctime" "common_badge_count")})

(s/defschema UserProfiles (-> u/User
                              (select-keys [:first_name :last_name :country :profile_picture])
                              (merge {:id s/Int
                                      :ctime s/Int
                                      :common_badge_count s/Int})))

(s/defschema Countries (s/constrained [s/Str] (fn [c]
                                                (and
                                                  (some #(= (first c) %) (keys all-countries))
                                                  (some #(= (second c) %) (vals all-countries))))))