(ns salava.extra.application.schemas
  (:require [schema.core :as s
             :include-macros true ;; cljs only
             ]
            [salava.core.countries :refer [all-countries]]
            [salava.user.schemas :as u]))

(s/defschema Applications  {:iframe (s/maybe s/Str)
                            :language (s/maybe s/Str)})
