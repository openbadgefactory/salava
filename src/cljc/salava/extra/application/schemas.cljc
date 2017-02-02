(ns salava.extra.application.schemas
  (:require [schema.core :as s
             :include-macros true]  ;; cljs only
            
            ))

(s/defschema Applications  {:iframe (s/maybe s/Str)
                            :language (s/maybe s/Str)})
