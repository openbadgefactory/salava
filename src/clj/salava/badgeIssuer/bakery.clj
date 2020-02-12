(ns salava.badgeIssuer.bakery
  (:require
   [buddy.core.nonce :as nonce]
   [clj-time.coerce :as c]
   [clojure.data.json :as json]
   [clojure.string :refer [blank?]]
   [clojure.tools.logging :as log]
   [salava.badge.db :refer [save-user-badge!]]
   [salava.badgeIssuer.db :as db]
   [salava.core.util :refer [bytes->base64 hex-digest get-site-url get-full-path now]]
   [salava.profile.db :refer [user-information]]
   [slingshot.slingshot :refer :all]))

(defn badge-recipient [ctx email]
  (let [salt (bytes->base64 (nonce/random-bytes 8))
        identity  (hex-digest "sha256" (str email salt))]

    {:identity (str "sha256$" identity)
     :salt salt
     :hashed true
     :type "email"}))

(defn parse-badge [ctx user-id id badge initial-assertion]
  (let [{:keys [name image criteria description tags]} badge
        issuer (user-information ctx user-id)
        creator (user-information ctx (:creator_id badge))
        parser {:content [{:id ""
                           :name name
                           :description description
                           :image_file (str (get-site-url ctx) "/" image)
                           :tags tags
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
                            :image_file (str (get-site-url ctx) "/" (:profile_picture issuer))
                            :email "no-reply@openbadgepassport.com"
                            :language_code ""
                            :revocation_list_url nil}]
                :creator  [{:id ""
                            :name (str (:first_name creator) " " (:last_name creator))
                            :description (:about creator)
                            :url (str (get-full-path ctx) "/profile/" (:id creator))
                            :image_file (str (get-site-url ctx) "/" (:profile_picture creator))
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

(defn bake-assertion [ctx data]
  (try+
   (let [{:keys [id user-id recipient expires_on]} data
         base-url (get-site-url ctx)
         badge (first (db/user-selfie-badge ctx user-id id))
         badge (assoc badge :tags (if-not (blank? (:tags badge)) (json/read-str (:tags badge)) []))
         issuedOn (now)
         r (badge-recipient ctx (:email recipient))
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
                  :expires_on expires_on
                  :assertion_jws nil}
         user-badge-id (save-user-badge! ctx (parse-badge ctx user-id id badge initial))
         assertion_url (str (get-full-path ctx) "/obpv1/selfie/_/assertion/" user-badge-id)
         assertion_json (-> {:id assertion_url
                             :recipient r
                             :expires (if expires_on
                                        (str (c/from-long (long (* expires_on 1000))))
                                        nil)
                             :issuedOn (str (c/from-long (long (* (now) 1000))))
                             :verification {:type "HostedBadge"}
                             :type  "Assertion"
                             :badge (str (get-full-path ctx) "/obpv1/selfie/_/badge/" user-badge-id"?i="user-id)
                             (keyword "@context") "https://w3id.org/openbadges/v2"}
                            (json/write-str))]

     (log/info "Updating criteria url!")
     (db/update-criteria-url! ctx user-badge-id)

     (log/info "Finalising user badge!")
     (db/finalise-user-badge! ctx {:id user-badge-id
                                   :selfie_id id
                                   :assertion_url assertion_url
                                   :assertion_json assertion_json
                                   :issuer_id user-id})
     (log/info "Finished saving user badge!"))

   (catch Object _
     (log/error "error: " _))))
