(ns salava.badgeIssuer.schemas
  #? (:clj (:require
            [schema.core :as s]
            [schema.coerce :as c]
            [clojure.string :refer [blank?]]
            [compojure.api.sweet :refer [describe]]
            [salava.badge.schemas :refer [evidence]])
      :cljs (:require
             [schema.core :as s :include-macros true]
             [clojure.string :refer [blank?]]
             [salava.badge.schemas :refer [evidence]])))

#? (:cljs (defn describe [v _] v))

(s/defschema selfie_badge
  {:id                    (s/maybe s/Str)
   :name                  (s/conditional #(not (blank? %)) s/Str)
   :description           (s/conditional #(not (blank? %)) s/Str)
   :criteria              (s/conditional #(not (blank? %)) s/Str)
   (s/optional-key :criteria_html) (s/maybe s/Str)
   :image                 (s/conditional #(not (blank? %)) s/Str)
   :deleted               (s/enum 0 1)
   :issuable_from_gallery (s/enum 0 1)
   (s/optional-key :tags) (s/maybe s/Str)
   (s/optional-key :issue_to_self) (s/enum 0 1)
   :ctime                 s/Int
   :mtime                 s/Int
   :creator_id            (s/maybe s/Int)})

(s/defschema initialize-badge
  (-> selfie_badge
      (assoc (s/optional-key :tags) [(s/maybe s/Str)])
      (dissoc :deleted :ctime :mtime :creator_id)))

(s/defschema save-selfie-badge
  (-> selfie_badge
      (assoc
       (s/optional-key :issue_to_self) (s/enum 0 1)
       (s/optional-key :tags) [(s/maybe s/Str)])
      (dissoc :ctime :mtime :deleted :creator_id)))

(s/defschema issue-selfie-badge
  {:selfie_id     s/Str
   :recipients    [s/Int]
   :expires_on    (s/maybe s/Int)
   (s/optional-key :request_endorsement) (s/maybe {:comment s/Str
                                                   :selected_users [s/Int]})
   (s/optional-key :evidence) (s/maybe [(assoc evidence (s/optional-key :resource_visibility) (s/maybe s/Str))])
   (s/optional-key :visibility) (s/enum "private" "public" "internal")
   (s/optional-key :issue_to_self) (s/enum 0 1)
   (s/optional-key :issued_from_gallery) s/Bool})

(s/defschema badge
  {:id s/Str
   :name s/Str
   :description s/Str
   :criteria {:id s/Str
              :narrative s/Str}
   :type (s/eq "BadgeClass")
   :issuer s/Str
   :image (s/maybe s/Str)
   :tags [(s/maybe s/Str)]
   (keyword "@context") (s/eq "https://w3id.org/openbadges/v2")})

(s/defschema issuer
  {:id s/Str
   :name s/Str
   :description (s/maybe s/Str)
   :type (s/eq "Profile")
   :url s/Str
   :email s/Str
   :image (s/maybe s/Str)
   (keyword "@context") (s/eq "https://w3id.org/openbadges/v2")})
