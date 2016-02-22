(ns salava.badge.schemas
  (:require [schema.core :as s
             :include-macros true ;; cljs only
             ]))

(s/defschema Badge {:id s/Int
                    :name s/Str
                    :description (s/maybe s/Str)
                    :user_id (s/maybe s/Int)
                    :email s/Str
                    :assertion_url (s/maybe s/Str)
                    :assertion_jws (s/maybe s/Str)
                    :assertion_json (s/maybe s/Str)
                    :badge_url (s/maybe s/Str)
                    :criteria_url (s/maybe s/Str)
                    :criteria_markdown (s/maybe s/Str)
                    :badge_content_id (s/maybe s/Str)
                    :image_file (s/maybe s/Str)
                    :issuer_content_id (s/maybe s/Str)
                    :issuer_email (s/maybe s/Str)
                    :issuer_content_name (s/maybe s/Str)
                    :issuer_content_url (s/maybe s/Str)
                    :issuer_url (s/maybe s/Str)
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
                    :revoked (s/maybe s/Bool)
                    :tag (s/maybe s/Str)
                    :tags (s/maybe [s/Str])})

(s/defschema BadgeContent {:id                             s/Int
                           :name                           (s/maybe s/Str)
                           :description                    (s/maybe s/Str)
                           :image_file                     (s/maybe s/Str)
                           :issued_on                      (s/maybe s/Int)
                           :expires_on                     (s/maybe s/Int)
                           :visibility                     (s/maybe (s/enum "private" "internal" "public"))
                           :status                         (s/maybe (s/enum "pending" "accepted" "declined"))
                           :mtime                          s/Int
                           :badge_content_id               (s/maybe s/Str)
                           :issuer_url                     (s/maybe s/Str)
                           :badge_url                      (s/maybe s/Str)
                           :obf_url                        (s/maybe s/Str)
                           :issued_by_obf                  s/Bool
                           :verified_by_obf                s/Bool
                           :issuer_verified                s/Bool
                           (s/optional-key :issuer_content_name) s/Str
                           (s/optional-key :issuer_content_url)  s/Str
                           (s/optional-key :email)         s/Str
                           (s/optional-key :assertion_url) (s/maybe s/Str)
                           (s/optional-key :tags)          (s/maybe [s/Str])})

(s/defschema BadgesToExport (select-keys Badge [:id :name :description :image_file :issued_on :expires_on :visibility :mtime :status :badge_content_id :email :assertion_url :tags]))

(s/defschema BadgeToImport {:status  (s/enum "ok" "invalid")
                            :message (s/maybe s/Str)
                            :key     s/Str
                            :name        s/Str
                            :description (s/maybe s/Str)
                            :image_file  (s/maybe s/Str)})

(s/defschema Import {:status (s/enum "success" "error")
                     :badges [BadgeToImport]
                     :error  (s/maybe s/Str)})

(s/defschema Upload {:status (s/enum "success" "error")
                     :message s/Str
                     :reason (s/maybe s/Str)})

(s/defschema BadgeStats {:badge_count           s/Int
                         :expired_badge_count   s/Int
                         :badge_views           [(merge
                                                   (select-keys Badge [:id :name :image_file])
                                                   {:reg_count   s/Int
                                                    :anon_count  s/Int
                                                    :latest_view (s/maybe s/Int)})]
                         :badge_congratulations [(merge
                                                   (select-keys Badge [:id :name :image_file])
                                                   {:congratulation_count  s/Int
                                                    :latest_congratulation (s/maybe s/Int)})]
                         :badge_issuers         [(-> Badge
                                                     (select-keys [:issuer_content_id :issuer_content_name :issuer_content_url])
                                                     (assoc :badges [(select-keys Badge [:id :name :image_file])]))]})
