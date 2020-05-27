(ns salava.extra.spaces.schemas
  (:require [schema.core :as s :include-macros true]))

(s/defschema space-properties {:css {:p-color (s/maybe s/Str)
                                     :s-color (s/maybe s/Str)
                                     :t-color (s/maybe s/Str)}})



(s/defschema space {:uuid s/Str
                    :name s/Str
                    :alias s/Str
                    :description (s/maybe s/Str)
                    (s/optional-key :logo )(s/maybe s/Str)
                    (s/optional-key :banner) (s/maybe s/Str)
                    :visibility (s/enum "public" "private")
                    :status (s/enum "active" "suspended")
                    (s/optional-key :properties) space-properties
                    (s/optional-key :admins) [s/Int]
                    :ctime s/Int
                    :mtime s/Int})

(s/defschema create-space (-> space (dissoc :uuid :ctime :mtime :status :visibility)))
