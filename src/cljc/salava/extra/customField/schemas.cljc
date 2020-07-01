(ns salava.extra.customField.schemas
  (:require
   [schema.core :as s :include-macros true]))

(s/defschema gender* (s/enum "male" "female" "other"))
(s/defschema gender (s/maybe gender*))
(s/defschema organization* (s/constrained s/Str #(and (>= (count %) 1) (<= (count %) 255))))
(s/defschema organization (s/maybe organization*))
