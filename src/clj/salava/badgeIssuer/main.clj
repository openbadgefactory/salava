(ns salava.badgeIssuer.main
  (:require
   [buddy.core.bytes :as b]
   [buddy.core.codecs :as codecs]
   [buddy.core.hash :as hash]
   [buddy.core.nonce :as nonce]
   [clj-time.coerce :as c]
   [clojure.data.json :as json]
   [clojure.string :refer [blank?]]
   [clojure.tools.logging :as log]
   [salava.badge.db :refer [save-user-badge!]]
   [salava.badge.main :refer [fetch-badge-p]]
   [salava.badgeIssuer.creator :refer [generate-image]]
   [salava.badgeIssuer.db :as db]
   [salava.badgeIssuer.util :refer [selfie-id]]
   [salava.core.util :refer [get-site-url bytes->base64 hex-digest now get-full-path get-db get-db-1 file-from-url-fix md->html]]
   [salava.profile.db :refer [user-information]]
   [salava.user.db :refer [primary-email]]
   [slingshot.slingshot :refer :all]))

(defn badge-assertion [ctx user-badge-id]
  (some-> (db/get-assertion-json {:id user-badge-id} (into {:result-set-fn first :row-fn :assertion_json} (get-db ctx)))
          (json/read-str :key-fn keyword)))

(defn badge-recipient [ctx email]
  (let [salt (bytes->base64 (nonce/random-bytes 8))
        identity  (hex-digest "sha256" (str email salt))]

    {:identity (str "sha256$" identity)
     :salt salt
     :hashed true
     :type "email"}))

(defn badge-issuer [ctx cid uid]
  (let [{:keys [name description image_file]} (db/get-issuer-information {:id cid} (into {:result-set-fn first} (get-db ctx)))]
    {:id (str (get-full-path ctx) "/obpv1/selfie/_/issuer?cid="cid "&uid="uid)
     :name name
     :description description
     :type "Profile"
     :url (str (get-full-path ctx) "/profile/" uid)
     :email "no-reply@openbadgepassport.com"
     :image (str (get-site-url ctx) "/" image_file)
     (keyword "@context") "https://w3id.org/openbadges/v2"}))

(defn badge-criteria [ctx id]
  (let [badge_id (db/select-badge-id-by-criteria-content-id {:id id} (into {:result-set-fn first :row-fn :badge_id} (get-db ctx)))
        criteria-content (db/get-criteria-page-information {:badge_id badge_id} (into  {:result-set-fn first} (get-db ctx)))]
   (-> criteria-content (update :criteria_content md->html))))

(defn get-badge [ctx user-badge-id uid]
  (let [badge_id (db/select-badge-id-by-user-badge-id {:user_badge_id user-badge-id} (into {:result-set-fn first :row-fn :badge_id} (get-db ctx)))
        badge (db/select-multi-language-badge-content {:id badge_id} (into {:result-set-fn first} (get-db ctx)))
        {:keys [id badge_id name badge_content_id description image criteria_url criteria_content_id criteria_content issuer_content_id issuer_url]} badge
        tags (vec (db/select-badge-tags {:id badge_content_id} (into {:row-fn :tag} (get-db ctx))))]
    {:id (str (get-full-path ctx) "/obpv1/selfie/_/badge/" user-badge-id"?i="uid)
     :type "BadgeClass"
     :name name
     :image (str (get-site-url ctx) "/" image)
     :description description
     :criteria {:id (str (get-full-path ctx) "/selfie/criteria/" criteria_content_id)
                :narrative criteria_content}
     :issuer (str (get-full-path ctx) "/obpv1/selfie/_/issuer?cid="issuer_content_id "&uid="uid)
     :tags (if (seq tags) tags [])
     (keyword "@context") "https://w3id.org/openbadges/v2"}))

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

(defn create-assertion [ctx data]
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
                             :recipient r ;(recipient ctx (:email (badge-criteria ctx user-badge-id)recipient))
                             :expires (if expires_on
                                        (str (c/from-long (long (* expires_on 1000))))
                                        nil)
                             :issuedOn (str (c/from-long (long (* (now) 1000))))
                             :verification {:type "HostedBadge"}
                             :type  "Assertion"
                             :badge (str (get-full-path ctx) "/obpv1/selfie/_/badge/" user-badge-id"?i="user-id)
                             (keyword "@context") "https://w3id.org/openbadges/v2"}
                            (json/write-str))]

     ;(log/info "Updating assetion url and assertion json!")
     #_(db/update-assertions-info! ctx {:id user-badge-id
                                        :assertion_url assertion_url
                                        :assertion_json assertion_json})

     (log/info "Updating criteria url!")
     (db/update-criteria-url! ctx user-badge-id)

     (log/info "Finalising user badge!")
     (db/finalise-user-badge! ctx {:id user-badge-id
                                   :selfie_id id
                                   :assertion_url assertion_url
                                   :assertion_json assertion_json
                                   :issuer_id user-id})
     ;(db/update-user-badge-issuer-id! {:issuer_id user-id :id user-badge-id} (get-db ctx))
     (log/info "Finished saving user badge!"))

   (catch Object _
     (log/error "error: " _))))

(defn issue-selfie-badge [ctx data user-id]
  (let [{:keys [selfie_id recipients issuer_to_self expires_on]} data]
    (log/info "Got badge issue request for id" recipients)
    (try+
     (doseq [r recipients
             :let [email (primary-email ctx r)
                   recipient {:id r :email email}
                   data {:selfie_id selfie_id}]]
       (log/info "Creating assertion for " recipient)
       (create-assertion ctx {:id selfie_id :user-id user-id :recipient recipient :expires_on expires_on}))
     (log/info "Finished issuing badges")
     {:status "success"}
     (catch Object _
       (log/error _)
       {:status "error"}))))

(defn save-selfie-badge [ctx data user-id]
  (try+
   (let [id (if-not (blank? (:id data))
              (:id data)
              (selfie-id))
         image (if (re-find #"^data:image" (:image data))
                 (file-from-url-fix ctx (:image data))
                 (:image data))
         tags (if (seq (:tags data)) (json/write-str (:tags data)) nil)]
     (db/insert-selfie-badge<! (-> data
                                   (assoc
                                    :id id
                                    :creator_id user-id
                                    :image image
                                    :tags tags)
                                   (dissoc :issue_to_self))
                               (get-db ctx))
    (when (pos? (:issue_to_self data))
      (issue-selfie-badge ctx {:selfie_id id :recipients [user-id]} user-id))
    {:status "success" :id id})
   (catch Object _
     (log/error (.getMessage _))
     {:status "error" :id "-1"})))


(defn issuing-history [ctx selfie-id user-id]
  (db/select-selfie-badge-issuing-history {:selfie_id selfie-id :issuer_id user-id} (get-db ctx)))

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
         (assoc :issuable_from_gallery (if (:issuable_from_gallery selfie-badge) 1 0)
                :tags (if (blank? (:tags selfie-badge)) nil (json/read-str (:tags selfie-badge))))
         (dissoc :deleted :ctime :mtime :creator_id)))))
