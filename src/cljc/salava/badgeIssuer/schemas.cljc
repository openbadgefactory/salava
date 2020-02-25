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
   (s/optional-key :request_endorsement) {:comment s/Str
                                          :selected_users [s/Int]}
   (s/optional-key :issue_to_self) (s/enum 0 1)
   (s/optional-key :issued_from_gallery) s/Bool})

#_(s/defschema recipient
    {:type    (s/eq "email")
     :identity s/Str
     :hashed   s/Bool
     :salt     s/Str})

#_(s/defschema revoked-assertion
    {:status (s/eq 410)
     :body {:id s/Str
            :revoked (s/eq true)}})

#_(s/defschema valid-assertion
    {:status (s/eq 200)
     :headers (s/eq {"Content-Type" "application/json"})
     :body {:id s/Str
            :issuedOn s/Str
            :recipient recipient
            :badge s/Str
            :expires (s/maybe s/Str)
            :verification  {:type (s/eq "HostedBadge")}
            :type (s/eq "Assertion")
            (keyword "@context") (s/eq (str "https://w3id.org/openbadges/v2"))}})

#_(def valid-assertion
    {:status (s/eq 200)
     :headers s/Any #_(s/eq {"Content-Type" "application/json"})
     :body {:id s/Str
            :issuedOn s/Str
            :recipient recipient
            :badge s/Str
            :expires (s/maybe s/Str)
            :verification  {:type (s/eq "HostedBadge")}
            :type (s/eq "Assertion")
            (keyword "@context") (s/eq (str "https://w3id.org/openbadges/v2"))}})

#_(s/defschema not-found
    {:status (s/eq 404)
     :body (s/eq "Badge assertion not found")})

#_(s/defschema server-error
    {:status (s/eq 500)
     :body (s/eq nil)})

#_(s/defschema assertion-response
    (s/conditional #(= (:status %) 500) server-error
                  #(= (:status %) 404) not-found
                  #(= (:status %) 410) revoked-assertion
                  #(= (:status %) 200) valid-assertion))

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
