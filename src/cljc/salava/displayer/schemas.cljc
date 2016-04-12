(ns salava.displayer.schemas
  (:require [schema.core :as s
             :include-macros true ;; cljs only
             ]))

(s/defschema DisplayerEmail {:status (s/enum "okay")
                             :email s/Str
                             :userId s/Int})

(s/defschema DisplayerGroups {:userId s/Int
                              :groups [{:groupId s/Int
                                        :name s/Str
                                        :badges (s/maybe s/Int)}]})

(s/defschema DisplayerBadges {:userId  s/Int
                              :groupId s/Int
                              :badges  s/Any})