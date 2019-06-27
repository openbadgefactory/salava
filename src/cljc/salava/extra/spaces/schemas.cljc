(ns salava.extra.spaces.schemas
  (:require [schema.core :as s :include-macros true]))

(s/defschema CreateSpace {:uuid s/Str
                          :name s/Str
                          :description (s/maybe s/Str)
                          :logo (s/maybe s/Str)
                          :banner (s/maybe s/Str)
                          :visibility s/Str
                          :status s/Str})
