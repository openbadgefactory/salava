(ns salava.badge.schemas
  #? (:clj (:require [schema.core :as s]
                     [schema.coerce :as c]
                     [compojure.api.sweet :refer [describe]])
           :cljs (:require [schema.core :as s :include-macros true])))

#? (:cljs (defn describe [v _] v))

(defn either [s1 s2]
  #? (:clj (s/either s1 s2)
      :cljs (s/cond-pre s1 s2)))

(s/defschema user-badge-p {:id                                   (describe s/Int "internal user-badge id")
                           :name                                 s/Str
                           :description                          (s/maybe s/Str)
                           :image_file                           (s/maybe s/Str)
                           (s/optional-key :assertion_url)       (s/maybe s/Str)
                           (s/optional-key :assertion-jws)       (s/maybe s/Str)
                           :revoked                              (either  s/Bool s/Int)
                           :issued_on                            (s/maybe s/Int)
                           :expires_on                           (s/maybe s/Int)
                           :mtime                                s/Int
                           :visibility                           (describe (s/maybe (s/enum "private" "internal" "public")) "internal user-badge visibility")
                           (s/optional-key :issuer_content_name) (s/maybe s/Str)
                           (s/optional-key :issuer_content_url)  (s/maybe s/Str)
                           (s/optional-key :status)              (describe (s/maybe (s/enum "pending" "accepted" "declined")) "internal user-badge acceptance status")
                           (s/optional-key :tags)                (describe (s/maybe [s/Str]) "internal tags added by current user")
                           (s/optional-key :png_image_file)      (describe (s/maybe s/Str) "png version of svg badge image")})

(s/defschema user-badge (-> user-badge-p
                            (assoc (s/optional-key :user_endorsement_count)    (s/maybe s/Int)
                                   (s/optional-key :meta_badge)                 (describe (s/maybe s/Str) "badge is a metabadge")
                                   (s/optional-key :meta_badge_req)             (describe (s/maybe s/Str) "badge is a required part of a metabadge")
                                   (s/optional-key :pending_endorsements_count) (s/maybe s/Int)
                                   (s/optional-key :pending_ext_endorsements_count) (s/maybe s/Int)
                                   (s/optional-key :endorsement_count)          (s/maybe s/Int)
                                   (s/optional-key :assertion-jws)              (s/maybe s/Str)
                                   (s/optional-key :new_message_count)          (s/maybe s/Int)
                                   (s/optional-key :gallery_id)                 (s/maybe s/Int)
                                   (s/optional-key :badge_id)                   (s/maybe s/Str)
                                   (s/optional-key :status)                     (describe (s/maybe (s/enum "pending" "accepted" "declined")) "internal user-badge acceptance status"))))

(s/defschema user-badges {:badges [user-badge]})

(s/defschema user-badges-p {:badges [user-badge-p]})

(s/defschema congratulation {:id s/Int
                             :first_name (s/maybe s/Str)
                             :last_name (s/maybe s/Str)
                             :profile_picture (s/maybe s/Str)
                             (s/optional-key :ctime) s/Int})

(s/defschema evidence-properties {(s/optional-key :hidden)         (describe (s/maybe s/Bool) "evidence visibility flag")
                                  (s/optional-key  :resource_id)   (describe (s/maybe s/Int) "used internally, attached evidence resource id")
                                  (s/optional-key  :mime_type)     (describe (s/maybe s/Str) "used internally, mime type of attached evidence resource")
                                  (s/optional-key  :resource_type) (s/maybe s/Str)})

(s/defschema evidence {:id                              (either s/Int s/Str)
                       :name                            (s/maybe s/Str)
                       :narrative                       (s/maybe s/Str)
                       :url                             (s/maybe s/Str)
                       (s/optional-key  :description)   (s/maybe s/Str)
                       (s/optional-key  :ctime)         (s/maybe s/Int)
                       (s/optional-key  :mtime)         (s/maybe s/Int)
                       (s/optional-key  :properties)    (s/maybe evidence-properties)})

(s/defschema badge-evidence {:evidence [evidence]})

(s/defschema alignment {:name s/Str
                        :description (s/maybe s/Str)
                        :url (s/maybe s/Str)})

(s/defschema badge-content {(s/optional-key :creator_content_id)     (s/maybe s/Str)
                            (s/optional-key :creator_description)    (s/maybe s/Str)
                            (s/optional-key :creator_email)          (s/maybe s/Str)
                            (s/optional-key :creator_name)           (s/maybe s/Str)
                            (s/optional-key :creator_image)          (s/maybe s/Str)
                            (s/optional-key :creator_url)            (s/maybe s/Str)
                            (s/optional-key :criteria_content)       (s/maybe s/Str)
                            (s/optional-key :criteria_url)           (s/maybe s/Str)
                            (s/optional-key :issuer_content_id)      (s/maybe s/Str)
                            (s/optional-key :issuer_description)     (s/maybe s/Str)
                            (s/optional-key :issuer_contact)         (s/maybe s/Str)
                            (s/optional-key :issuer_image)           (s/maybe s/Str)
                            (s/optional-key :issuer_content_url)     (s/maybe s/Str)
                            (s/optional-key :issuer_content_name)    (s/maybe s/Str)
                            (s/optional-key :language_code)          (s/maybe s/Str)
                            (s/optional-key :default_language_code)  (s/maybe s/Str)
                            (s/optional-key :endorsement_count)      (s/maybe s/Int)
                            (s/optional-key :alignment)              (s/maybe [alignment])
                            (s/optional-key :png_image_file)         (describe (s/maybe s/Str) "png version of svg badge image")
                            :badge_id                                (describe (s/maybe s/Str) "used internally to group user badges with same content")
                            :name                                    s/Str
                            :image_file                              (s/maybe s/Str)
                            :description                             (s/maybe s/Str)})

(s/defschema user-badge-content (-> user-badge-p
                                    (select-keys [:id :mtime :issued_on :expires_on :visibility :revoked])
                                    (assoc :ctime                                   s/Int
                                           :owner?                                  s/Bool
                                           :issued_by_obf                           (describe s/Bool "badge issued by OBF?")
                                           :verified_by_obf                         (describe s/Bool "badge verified by OBF?")
                                           :issuer_verified                         (describe (s/maybe s/Int) "issuer verified by OBF?")
                                           :first_name                              (describe (s/maybe s/Str) "badge earner's first name")
                                           :last_name                               (describe (s/maybe s/Str) "badge earner's last name")
                                           :owner                                   (describe s/Int "internal id of badge owner")
                                           :content                                 [badge-content]
                                           (s/optional-key :assertion_url)          (s/maybe s/Str)
                                           (s/optional-key :assertion-jws)          (s/maybe s/Str)
                                           (s/optional-key :assertion_json)         (s/maybe s/Str)
                                           (s/optional-key :view_count)             (describe (s/maybe s/Int) "no of times user badge has been viewed")
                                           (s/optional-key :gallery_id)             (describe s/Int "internal gallery badge id")
                                           ;(s/optional-key :user_endorsement_count) (s/maybe s/Int)
                                           (s/optional-key :congratulations)        (describe (s/maybe [congratulation]) "internal user badge congratulations by other users")
                                           (s/optional-key :congratulated?)         (s/maybe s/Bool)
                                           (s/optional-key :badge_id)               (describe (s/maybe s/Str) "used internally to group user badges with same content")
                                           (s/optional-key :qr_code)                (s/maybe s/Str)
                                           (s/optional-key :obf_url)                (describe (s/maybe s/Str) "OBF factory url badge was issued from")
                                           (s/optional-key :user-logged-in?)        (s/maybe s/Bool)
                                           (s/optional-key :show_recipient_name)    (describe (s/maybe s/Int) "used internally; when set, earner's name is shown in badge ")
                                           (s/optional-key :remote_url)             (describe (s/maybe s/Str) "domain where badge assertion is hosted")
                                           (s/optional-key :recipient_count)        (s/maybe s/Int)
                                           (s/optional-key :email)                  (describe (s/maybe s/Str) "email badge was issued to")
                                           (s/optional-key :user_id)                (s/maybe s/Int))))

(s/defschema user-badge-content-p (-> user-badge-p
                                      (select-keys [:id :mtime :issued_on :expires_on :visibility :revoked])
                                      (assoc :ctime                                   s/Int
                                             :content                                 [badge-content]
                                             :first_name                              (describe (s/maybe s/Str) "badge earner's first name")
                                             :last_name                               (describe (s/maybe s/Str) "badge earner's last name")
                                             :owner                                   (describe s/Int "internal id of badge owner")
                                             :endorsement_count    s/Int
                                             :evidence_count       s/Int
                                             :congratulation_count s/Int
                                             ;:issuer_verified                         (describe (s/maybe s/Int) "issuer verified by OBF?")
                                             ;:issued_by_obf                           (describe s/Bool "badge issued by OBF?")
                                             ;:verified_by_obf                         (describe s/Bool "badge verified by OBF?")
                                             (s/optional-key :assertion_url)          (s/maybe s/Str)
                                             (s/optional-key :assertion_jws)          (s/maybe s/Str)
                                             (s/optional-key :gallery_id)             (describe s/Int "internal gallery badge id")
                                             (s/optional-key :show_recipient_name)    (describe (s/maybe s/Int) "used internally; when set, earner's name is shown in badge ")
                                             (s/optional-key :remote_url)             (describe (s/maybe s/Str) "domain where badge assertion is hosted")
                                             (s/optional-key :assertion_json)         (s/maybe s/Str))))
                                             ;(s/optional-key :qr_code)                (s/maybe s/Str)
                                             ;(s/optional-key :email)                  (describe (s/maybe s/Str) "email badge was issued to")
                                             ;(s/optional-key :user_id)                (s/maybe s/Int))))

(s/defschema congratulations-p {:congratulations [congratulation]})

(s/defschema verification {:type                                  (s/enum "HostedBadge" "SignedBadge" "hosted" "signed")
                           (s/optional-key :url)                  (s/maybe s/Str)
                           (s/optional-key :startsWith)           (s/maybe s/Str)
                           (s/optional-key :id)                   (s/maybe s/Str)
                           (s/optional-key :allowedOrigins)       (either s/Str [s/Str])})

(s/defschema recipient {:type                  (either s/Str [s/Str])
                        :identity              s/Str
                        :hashed                s/Bool
                        (s/optional-key :salt) (s/maybe s/Str)})

(s/defschema assertion {(s/optional-key :id)                 (s/maybe s/Str)
                        (s/optional-key :uid)                 (s/maybe s/Str)
                        :recipient                            recipient
                        :badge                                s/Str
                        (s/optional-key :verification)        verification
                        (s/optional-key :verify)              verification
                        :issuedOn                             s/Str
                        (s/optional-key :type)                (s/maybe (either s/Str [s/Str]))
                        (s/optional-key :image)               (s/maybe s/Str)
                        (s/optional-key :evidence)            s/Any #_(s/maybe (s/cond-pre s/Str [s/Str] [evidence]))
                        (s/optional-key :narrative)           (s/maybe s/Str)
                        (s/optional-key :expires)             (s/maybe s/Str)
                        (s/optional-key :revoked)             (s/maybe s/Bool)
                        (s/optional-key :revocationReason)    (s/maybe s/Str)
                        (s/optional-key (keyword "@context"))  s/Any})

(s/defschema verify-success {:assertion-status      (describe (s/eq 200) "Fetched assertion status response")
                             :assertion             s/Any #_assertion
                             :asr                   (describe s/Str "Assertion url")
                             :badge-image-status    (s/eq 200)
                             :badge-criteria-status (s/eq 200)
                             :badge-issuer-status   (s/eq 200)
                             :revoked?              (s/maybe (either s/Str s/Bool))
                             :expired?              (s/maybe s/Bool)
                             (s/optional-key :revocation_reason) (s/maybe s/Str)})

(s/defschema verify-failure {:assertion-status            (describe (s/enum 404 410 500) "Fetched assertion status response")
                             :asr                         (describe s/Str "Assertion url")
                             (s/optional-key :revoked?)   (s/maybe s/Bool)
                             (s/optional-key :message)    (s/maybe s/Str)})

(s/defschema verify-badge #? (:clj (s/either verify-success verify-failure)
                                   :cljs (s/cond-pre verify-success verify-failure)))

(s/defschema endorsement {:id s/Str
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
                                   :endorsement [(s/maybe (s/recursive #'endorsement))]}})

(s/defschema badge-endorsements {:endorsements (s/maybe [endorsement])})

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
                    :creator_content_id (s/maybe s/Str)
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
             {:id                                   (describe s/Int "internal user-badge id")
              :name                                 (s/maybe s/Str)
              :description                          (s/maybe s/Str)
              :image_file                           (s/maybe s/Str)
              :png_image_file                       (s/maybe s/Str)
              :issued_on                            (s/maybe s/Int)
              :expires_on                           (s/maybe s/Int)
              :revoked                              s/Bool
              :mtime                                s/Int
              (s/optional-key :visibility)          (describe (s/maybe (s/enum "private" "internal" "public")) "internal user-badge visibility")
              (s/optional-key :status)              (describe (s/maybe (s/enum "pending" "accepted" "declined")) "internal user-badge acceptance status")
              (s/optional-key :badge_id)            (describe (s/maybe s/Str) "used internally to group user badges with same content")
              (s/optional-key :obf_url)             (describe (s/maybe s/Str) "OBF factory url badge was issued from")
              :issued_by_obf                        (describe s/Bool "badge issued by OBF?")
              :verified_by_obf                      (describe s/Bool "badge verified by OBF?")
              :issuer_verified                      (describe (s/maybe s/Int) "issuer verified by OBF?")
              (s/optional-key :issuer_content_name) (s/maybe s/Str)
              (s/optional-key :issuer_content_url)  (s/maybe s/Str)
              (s/optional-key :email)               (s/maybe s/Str)
              (s/optional-key :assertion_url)       (s/maybe s/Str)
              (s/optional-key :meta_badge)          (describe (s/maybe s/Str) "badge is a metabadge")
              (s/optional-key :meta_badge_req)      (describe (s/maybe s/Str) "badge is a required part of a metabadge")
              (s/optional-key :message_count)       (describe {:new-messages (s/maybe s/Int)
                                                               :all-messages (s/maybe s/Int)} "internal badge comments")
              (s/optional-key :tags)                (describe (s/maybe [s/Str]) "internal tags added by user")
              (s/optional-key :user_endorsements_count) (s/maybe s/Int)
              (s/optional-key :endorsement_count) (s/maybe s/Int)
              (s/optional-key :pending_endorsements_count) (s/maybe s/Int)
              (s/optional-key :new_message_count) (s/maybe s/Int)
              (s/optional-key :gallery_id)        (s/maybe s/Int)})

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

(s/defschema user-badge-stats {:id           s/Int
                               :reg_count    (describe (s/maybe s/Int) "No of views by registered users")
                               :anon_count   (describe (s/maybe s/Int) "No of anonymous views")
                               :latest_view  (describe (s/maybe s/Int) "Latest view's timestamp")})

(s/defschema badge-congratulations (-> (select-keys Badge [:id :name :image_file])
                                       (merge {:congratulation_count s/Int :latest_congratulation (s/maybe s/Int)})))

(s/defschema badge-issuers (-> (select-keys Badge [:issuer_content_id :issuer_content_name :issuer_content_url])
                               (assoc :badges [(select-keys Badge [:id :name :image_file])])))

(s/defschema user-badges-statistics {:badge_count           (describe s/Int "user badges count")
                                     :expired_badge_count   (describe s/Int "user expired badges count")
                                     :badge_views           [(-> (select-keys Badge [:id :name :image_file])
                                                                 (merge user-badge-stats))]
                                     :badge_congratulations [badge-congratulations]
                                     :badge_issuers         [badge-issuers]})

(s/defschema user-badge-settings (-> (select-keys user-badge [:id :name :image_file :visibility])
                                     (assoc (s/optional-key :tags)                 (describe (s/maybe [s/Str]) "internal tags added by current user")
                                            (s/optional-key :show_recipient_name)  (describe (s/maybe s/Int) "used internally; when set, earner's name is shown in badge ")
                                            (s/optional-key :show_evidence) s/Int
                                            :rating (s/maybe s/Int))))

(s/defschema badge-settings {(s/optional-key :visibility)         (describe (s/maybe (s/enum "private" "internal" "public")) "internal user-badge visibility")
                             (s/optional-key :rating)              (s/maybe (s/enum 5 10 15 20 25 30 35 40 45 50))
                             (s/optional-key :show_recipient_name) (s/maybe (describe (s/enum 0 1 2) "used internally; when set, earner's name is shown in badge; 1-> show name as plain text to external users; 2-> show name to external users as link to badge owner's profile"))})

(s/defschema update-badge-settings {(s/optional-key :settings) badge-settings
                                    (s/optional-key :tags)     (describe (s/maybe [s/Str]) "Adding tags overrides previously added tags, To preserve previously added tags include them in the list of tags. Pass empty vector to delete all tags")})

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

(s/defschema save-badge-evidence {(s/optional-key :id)             (s/maybe s/Int)
                                  :name                            (s/maybe s/Str)
                                  :narrative                       (s/maybe s/Str)
                                  :url                             s/Str
                                  (s/optional-key  :resource_id)   (describe (s/maybe s/Int) "used internally, attached evidence resource id")
                                  (s/optional-key  :mime_type)     (describe (s/maybe s/Str) "used internally, mime type of attached evidence resource")
                                  (s/optional-key  :resource_type) (s/maybe s/Str)})
