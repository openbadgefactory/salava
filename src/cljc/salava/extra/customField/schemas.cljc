(ns salava.extra.customField.schemas
  (:require
   [schema.core :as s :include-macros true]))

(s/defschema gender* (s/enum "male" "female" "other"))
(s/defschema gender (s/maybe gender*))
