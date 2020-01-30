(ns salava.badgeIssuer.main
  (:require
   [buddy.core.bytes :as b]
   [buddy.core.codecs :as codecs]
   [buddy.core.hash :as hash]
   [buddy.core.nonce :as nonce]
   [clj-time.local :as l]
   [clojure.data.json :as json]
   [clojure.tools.logging :as log]
   [salava.badge.db :refer [save-user-badge!]]
   [salava.badgeIssuer.creator :refer [generate-image]]
   [salava.badgeIssuer.db :as db]
   [salava.core.util :refer [get-site-url bytes->base64 hex-digest now get-full-path]]
   [salava.profile.db :refer [user-information]]
   [salava.user.db :refer [primary-email]]
   [slingshot.slingshot :refer :all]))

(defn recipient [ctx email]
  (let [salt (bytes->base64 (nonce/random-bytes 8))
        identity  (hex-digest "sha256" (str email salt))]

    {:identity (str "sha256$" identity)
     :salt salt
     :hashed true
     :type "email"}))

(defn initialize
  ([ctx user]
   {:image (:url (generate-image ctx user))
    :name nil
    :criteria ""
    :description nil
    :tags nil
    :issuable_from_gallery 0
    :id nil})
  ([ctx user id]
   (let [selfie-badge (first (db/user-selfie-badge ctx (:id user) id))]
     (-> selfie-badge
         (assoc :issuable_from_gallery (if (:issuable_from_gallery selfie-badge) 1 0))
         (dissoc :deleted :ctime :mtime :creator_id)))))

(defn parse-badge [ctx user-id id badge initial-assertion]
  (let [{:keys [name image criteria description]} badge
        issuer (user-information ctx user-id)
        creator (user-information ctx (:creator_id badge))
        parser {:content [{:id ""
                           :name name
                           :description description
                           :image_file image
                           :tags nil
                           :language_code ""
                           :alignment []}]

                :criteria [{:id ""
                            :language_code ""
                            :url ""
                            :markdown_text criteria}]
                :issuer   [{:id ""
                            :name (str (:first_name issuer) " " (:last_name issuer))
                            :description (:about issuer)
                            :url (str (get-full-path ctx) "/profile/" user-id)
                            :image_file (str (get-site-url ctx)"/"(:profile_picture issuer))
                            :email nil
                            :language_code ""
                            :revocation_list_url nil}]
                :creator  [{:id ""
                            :name (str (:first_name creator) " " (:last_name creator))
                            :description (:about creator)
                            :url (str (get-full-path ctx) "/profile/" (:id creator))
                            :image_file (str (get-site-url ctx) "/"(:profile_picture creator))
                            :email nil
                            :language_code ""
                            :json_url (str (get-full-path ctx) "/profile/" (:creator_id creator))}]}]
    (assoc initial-assertion
      :badge (merge {:id ""
                     :remote_url nil
                     :remote_id nil
                     :remote_issuer_id nil
                     :issuer_verified 0
                     :default_language_code ""
                     :published 0
                     :last_received 0
                     :recipient_count 0}
               parser))))

(defn create-assertion [ctx id user-id recipient]
  (let [base-url (get-site-url ctx)
        badge (first (db/user-selfie-badge ctx user-id id))
        {:keys [name image criteria description]} badge
        issuedOn (now)
        initial {:user_id (:id recipient)
                 :email (:email recipient)
                 :status "pending"
                 :visibility "private"
                 :show_recipient_name 0
                 :rating nil
                 :ctime (now)
                 :mtime (now)
                 :deleted 0
                 :revoked 0
                 :assertion_url ""
                 :assertion_json ""
                 :issued_on issuedOn
                 :expires_on nil
                 :assertion_jws nil}
        user-badge-id (save-user-badge! ctx (parse-badge ctx user-id id badge initial))
        assertion_url (str (get-full-path ctx) "/obpv1/selfie/assertion/" user-badge-id)
        assertion_json (json/write-str
                        {:id assertion_url
                         :recipient (recipient ctx (:email recipient))
                         :expires nil
                         :issuedOn (str (l/to-local-date-time (long (* (now) 1000))))
                         :verification {:type "HostedBadge"}
                         :type  "Assertion"
                         :badge {:id id
                                 :type "BadgeClass"
                                 :name name
                                 :image image
                                 :description description
                                 :criteria {:id ""
                                            :narrative criteria}
                                 :issuer (str (get-full-path ctx) "/profile/" user-id)}
                         (keyword "@context") "https://w3id.org/openbadges/v2"})]
    (db/update-assertions-info! ctx {:id user-badge-id
                                     :assertion_url assertion_url
                                     :assertion_json assertion_json})))


(defn issue-selfie-badge [ctx selfie-id user-id recipients]
  (log/info "Got badge issue request for id" recipients)
  (doseq [r recipients
          :let [email (primary-email ctx r)
                recipient {:id r :email email}]]
    (log/info "Creating assertion for " recipient)
    (create-assertion ctx selfie-id user-id recipient))
  (log/info "Finished issuing badges"))
  ;(create-assertion ctx "5ea11b20-c470-4f43-937b-7b2371a85cb9" 12 {:id 12 :email "isaac.ogunlolu@discendum.com"}))
