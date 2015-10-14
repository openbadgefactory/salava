(ns salava.badge.schemas
  (:require [schema.core :as s
             :include-macros true ;; cljs only
             ]))

(s/defschema Badge {:id s/Int
                    :user_id (s/maybe s/Int)
                    :email s/Str
                    :assertion_url (s/maybe s/Str)
                    :assertion_jws (s/maybe s/Str)
                    :assertion_json (s/maybe s/Str)
                    :badge_url (s/maybe s/Str)
                    :issuer_url (s/maybe s/Str)
                    :criteria_url (s/maybe s/Str)
                    :badge_content_id (s/maybe s/Str)
                    :issuer_content_id (s/maybe s/Str)
                    :issued_on (s/maybe s/Int)
                    :expires_on (s/maybe s/Int)
                    :evidence_url (s/maybe s/Str)
                    :status (s/maybe (s/enum "pending" "accepted" "declined"))
                    :visibility (s/maybe (s/enum "private" "internal" "public"))
                    :show_recipient_name (s/maybe s/Bool)
                    :rating (s/maybe s/Int)
                    :ctime s/Int
                    :mtime s/Int
                    :deleted (s/maybe s/Bool)
                    :revoked (s/maybe s/Bool)})

(s/defschema BadgeContent {:id s/Int
                           :name (s/maybe s/Str)
                           :description (s/maybe s/Str)
                           :image_file (s/maybe s/Str)
                           :issued_on (s/maybe s/Int)
                           :expires_on (s/maybe s/Int)
                           :visibility (s/maybe (s/enum "private" "internal" "public"))
                           :mtime s/Int
                           :badge_content_id (s/maybe s/Str)
                           (s/optional-key :email) s/Str
                           (s/optional-key :assertion_url) (s/maybe s/Str)
                           (s/optional-key :tags) (s/maybe [s/Str])})

(s/defschema BadgeToImport {:status  (s/enum "ok" "invalid")
                            :message (s/maybe s/Str)
                            :key     s/Str
                            :name        s/Str
                            :description (s/maybe s/Str)
                            :image_file  (s/maybe s/Str)})

(s/defschema Import {:status (s/enum "success" "error")
                     :badges [BadgeToImport]
                     :error  (s/maybe s/Str)})

