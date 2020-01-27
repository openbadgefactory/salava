(ns salava.badgeIssuer.schemas
  #? (:clj (:require [schema.core :as s]
                     [schema.coerce :as c]
                     [compojure.api.sweet :refer [describe]])
           :cljs (:require [schema.core :as s :include-macros true])))

#? (:cljs (defn describe [v _] v))

(s/defschema selfie_badge {:id (s/maybe s/Str)
                           :name s/Str
                           :description s/Str
                           :criteria s/Str
                           :image s/Str
                           :deleted (s/enum 0 1)
                           :issuable_from_gallery (s/enum 0 1)
                           (s/optional-key :tags) (s/maybe s/Str)
                           :ctime s/Int
                           :mtime s/Int
                           :creator_id (s/maybe s/Int)})

(s/defschema save-selfie-badge  (-> selfie_badge
                                    (assoc (s/optional-key :tags) (s/maybe s/Str))
                                    (dissoc :ctime :mtime :deleted :creator_id)))
