(ns salava.badge.schemas
  (:require [schema.core :as s
             :include-macros true])) ;; cljs only


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
                    :criteria_content (s/maybe s/Str)
                    :badge_id (s/maybe s/Str)
                    :image_file (s/maybe s/Str)
                    :issuer_content_id (s/maybe s/Str)
                    :issuer_email (s/maybe s/Str)
                    :issuer_content_name (s/maybe s/Str)
                    :issuer_content_url (s/maybe s/Str)
                    :issuer_image (s/maybe s/Str)
                    :issuer_url (s/maybe s/Str)
                    :creator_email (s/maybe s/Str)
                    :creator_name (s/maybe s/Str)
                    :creator_url (s/maybe s/Str)
                    :creator_image (s/maybe s/Str)
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
                    :tags (s/maybe [s/Str])})

(s/defschema UserBadgeContent
  {:id                                   s/Int
   :name                                 (s/maybe s/Str)
   :description                          (s/maybe s/Str)
   :image_file                           (s/maybe s/Str)
   :issued_on                            (s/maybe s/Int)
   :expires_on                           (s/maybe s/Int)
   :revoked                              (s/maybe s/Bool)
   :visibility                           (s/maybe (s/enum "private" "internal" "public"))
   :status                               (s/maybe (s/enum "pending" "accepted" "declined"))
   :mtime                                s/Int
   :badge_id                     (s/maybe s/Str)
   ;  :issuer_url                           (s/maybe s/Str)
   ;  :badge_url                            (s/maybe s/Str)
   :obf_url                              (s/maybe s/Str)
   :issued_by_obf                        s/Bool
   :verified_by_obf                      s/Bool
   :issuer_verified                      (s/maybe s/Int)
   (s/optional-key :issuer_content_name) (s/maybe s/Str)
   (s/optional-key :issuer_content_url)  (s/maybe s/Str)
   (s/optional-key :email)               (s/maybe s/Str)
   (s/optional-key :assertion_url)       (s/maybe s/Str)
   (s/optional-key :meta_badge)          (s/maybe s/Str)
   (s/optional-key :meta_badge_req)      (s/maybe s/Str)
   (s/optional-key :message_count)       {:new-messages (s/maybe s/Int)
                                          :all-messages (s/maybe s/Int)}
   (s/optional-key :tags)                (s/maybe [s/Str])
   (s/optional-key :user_endorsements_count) (s/maybe s/Int)
   (s/optional-key :endorsement_count) (s/maybe s/Int)
   (s/optional-key :pending_endorsements_count) (s/maybe s/Int)})

(s/defschema BadgesToExport (select-keys Badge [:id :name :description :image_file
                                                :issued_on :expires_on :visibility
                                                :mtime :status :badge_content_id
                                                :email :assertion_url :tags
                                                :issuer_content_name ;:issuer_url
                                                :issuer_content_url]))

(s/defschema BadgeToImport {:status  (s/enum "ok" "invalid")
                            :message (s/maybe s/Str)
                            :error (s/maybe s/Str)
                            :import-key     s/Str
                            :name        s/Str
                            :description (s/maybe s/Str)
                            :image_file  (s/maybe s/Str)
                            :issuer_content_name (s/maybe s/Str)
                            :issuer_content_url (s/maybe s/Str)
                            :previous-id (s/maybe s/Int)})

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


(s/defschema Endorsement {:id s/Str
                          :content s/Str
                          :issued_on s/Int
                          :issuer {:id   s/Str
                                   :language_code s/Str
                                   :name s/Str
                                   :url  s/Str
                                   :description (s/maybe s/Str)
                                   :image_file (s/maybe s/Str)
                                   :email (s/maybe s/Str)
                                   :revocation_list_url (s/maybe s/Str)
                                   :endorsement [(s/maybe (s/recursive #'Endorsement))]}})

(s/defschema BadgeContent {:id    s/Str
                           :language_code s/Str
                           :name  s/Str
                           :image_file  s/Str
                           :description s/Str
                           (s/optional-key :obf_url)    (s/maybe s/Str)
                           :alignment [(s/maybe {:name s/Str
                                                 :url  s/Str
                                                 :description (s/maybe s/Str)})]
                           :tags      [(s/maybe s/Str)]})

(s/defschema IssuerContent {:id   s/Str
                            :language_code s/Str
                            :name s/Str
                            :url  s/Str
                            :description (s/maybe s/Str)
                            :image_file (s/maybe s/Str)
                            :email (s/maybe s/Str)
                            :revocation_list_url (s/maybe s/Str)
                            :endorsement [(s/maybe Endorsement)]})


(s/defschema CreatorContent (-> IssuerContent
                                (dissoc :revocation_list_url)
                                (assoc  :json_url s/Str)))

(s/defschema CriteriaContent {:id s/Str
                              :language_code s/Str
                              :url s/Str
                              :markdown_text (s/maybe s/Str)})

(s/defschema UserBackpackEmail {:email s/Str
                                :backpack_id (s/maybe s/Int)})

(s/defschema Evidence {:id (s/maybe s/Int)
                       :name (s/maybe s/Str)
                       :narrative (s/maybe s/Str)
                       :url s/Str
                       (s/optional-key  :resource_id) (s/maybe s/Int)
                       (s/optional-key  :resource_type) s/Str
                       (s/optional-key  :mime_type) (s/maybe s/Str)})

(s/defschema UserEndorsement {:id s/Int
                               :user_badge_id s/Int
                               :content s/Str
                               :status (s/enum "pending" "accepted" "declined")
                               :mtime s/Int
                               (s/optional-key :issuer_id) (s/maybe s/Int)
                               (s/optional-key :endorsee_id) (s/maybe s/Int)
                               (s/optional-key :issuer_name) (s/maybe s/Str)
                               (s/optional-key :issuer_url) (s/maybe s/Str)
                               (s/optional-key :first_name) s/Str
                               (s/optional-key :last_name) s/Str
                               (s/optional-key :profile_picture) (s/maybe s/Str)
                               (s/optional-key :profile_visibility) (s/enum "internal" "public")
                               (s/optional-key :name) (s/maybe s/Str)
                               (s/optional-key :image_file) (s/maybe s/Str)
                               (s/optional-key :description) (s/maybe s/Str)})

(s/defschema EndorsementRequest {:id s/Int
                                  :user_badge_id s/Int
                                  :content s/Str
                                  :status (s/enum "pending" "endorsed" "declined")
                                  :mtime s/Int
                                  (s/optional-key :requester_id) (s/maybe s/Int)
                                  (s/optional-key :user_id) (s/maybe s/Int)
                                  (s/optional-key :first_name) s/Str
                                  (s/optional-key :last_name) s/Str
                                  (s/optional-key :profile_picture) (s/maybe s/Str)
                                  (s/optional-key :profile_visibility) (s/enum "internal" "public")
                                  (s/optional-key :name) (s/maybe s/Str)
                                  (s/optional-key :image_file) (s/maybe s/Str)
                                  (s/optional-key :description) (s/maybe s/Str)
                                  (s/optional-key :type) (s/maybe s/Str)})

(s/defschema AllEndorsements {:given [(s/maybe UserEndorsement)]
                              :received [(s/maybe UserEndorsement)]
                              :requests [(s/maybe EndorsementRequest)]
                              :all-endorsements [(s/maybe (merge EndorsementRequest UserEndorsement))]})
