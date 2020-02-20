(ns salava.core.schemas
  #? (:clj (:require
            [schema.core :as s]
            [schema.coerce :as c]
            [clojure.string :refer [blank?]]
            [compojure.api.sweet :refer [describe]])
      :cljs (:require
             [schema.core :as s :include-macros true]
             [clojure.string :refer [blank?]])))

(defn either [s1 s2]
  #? (:clj (s/either s1 s2)
      :cljs (s/cond-pre s1 s2)))

(s/defschema PublishEvent
  {:subject s/Int
   :object (either s/Int s/Str)
   :verb (s/enum "message"
                 "follow"
                 "publish"
                 "delete_message"
                 "ticket"
                 "congratulate"
                 "modify"
                 "unpublish"
                 "advertise"
                 "request_endorsement"
                 "endorse_badge"
                 "issue"
                 "create"
                 "delete")

   :type (s/enum "badge"
                 "user"
                 "page"
                 "admin"
                 "advert"
                 "selfie")})
