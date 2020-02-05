(ns salava.badgeIssuer.schemas
  #? (:clj (:require [schema.core :as s]
                     [schema.coerce :as c]
                     [clojure.string :refer [blank?]]
                     [compojure.api.sweet :refer [describe]])
           :cljs (:require [schema.core :as s :include-macros true]
                           [clojure.string :refer [blank?]])))

#? (:cljs (defn describe [v _] v))

(s/defschema selfie_badge {:id (s/maybe s/Str)
                           :name (s/conditional #(not (blank? %)) s/Str)
                           :description (s/conditional #(not (blank? %)) s/Str)
                           :criteria (s/conditional #(not (blank? %)) s/Str)
                           :image (s/conditional #(not (blank? %)) s/Str)
                           :deleted (s/enum 0 1)
                           :issuable_from_gallery (s/enum 0 1)
                           (s/optional-key :tags) (s/maybe s/Str)
                           :ctime s/Int
                           :mtime s/Int
                           :creator_id (s/maybe s/Int)})

(s/defschema save-selfie-badge  (-> selfie_badge
                                    (assoc (s/optional-key :tags) [(s/maybe s/Str)])
                                    (dissoc :ctime :mtime :deleted :creator_id)))

(s/defschema issue-selfie-badge {:selfie_id s/Str
                                 :recipients [s/Int]
                                 :expires_on (s/maybe s/Int)
                                 :issue_to_self s/Int})
