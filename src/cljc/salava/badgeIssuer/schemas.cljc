(ns salava.badgeIssuer.schemas
  #? (:clj (:require
            [schema.core :as s]
            [schema.coerce :as c]
            [clojure.string :refer [blank?]]
            [compojure.api.sweet :refer [describe]])
      :cljs (:require
             [schema.core :as s :include-macros true]
             [clojure.string :refer [blank?]])))

#? (:cljs (defn describe [v _] v))

(defn either [s1 s2 s3]
  #? (:clj (s/either s1 s2 s3)
      :cljs (s/cond-pre s1 s2 s3)))


(s/defschema selfie_badge
  {:id                    (s/maybe s/Str)
   :name                  (s/conditional #(not (blank? %)) s/Str)
   :description           (s/conditional #(not (blank? %)) s/Str)
   :criteria              (s/conditional #(not (blank? %)) s/Str)
   :image                 (s/conditional #(not (blank? %)) s/Str)
   :deleted               (s/enum 0 1)
   :issuable_from_gallery (s/enum 0 1)
   (s/optional-key :tags) (s/maybe s/Str)
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
   :issue_to_self s/Int})

(s/defschema recipient
  {:type    (s/eq "email")
   :identity s/Str
   :hashed   s/Bool
   :salt     s/Str})

(s/defschema revoked-assertion
  {:id s/Str
   :revoked (s/eq true)})

(s/defschema valid-assertion
  {:id s/Str
   :issuedOn s/Str
   :recipient recipient
   :badge s/Str
   :expires (s/maybe s/Str)
   :verification  {:type (s/eq "HostedBadge")}
   :type (s/eq "Assertion")
   (keyword "@context") (s/eq "https://w3id.org/openbadges/v2")})

(s/defschema not-found
  {:status (s/eq 404)
   :headers (s/eq {})
   :body (s/eq "Badge assertion not found")})

(s/defschema assertion
  (either valid-assertion revoked-assertion not-found))

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
   :email (s/eq "no-reply@openbadgepassport.com")
   :image (s/maybe s/Str)
   (keyword "@context") (s/eq "https://w3id.org/openbadges/v2")})
