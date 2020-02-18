(ns salava.badgeIssuer.main
  (:require
   [clojure.data.json :as json]
   [clojure.string :refer [blank?]]
   [clojure.tools.logging :as log]
   [ring.util.http-response :refer [not-found gone ok internal-server-error]]
   [salava.badgeIssuer.bakery :as bakery]
   [salava.badgeIssuer.creator :refer [generate-image]]
   [salava.badgeIssuer.db :as db]
   [salava.badgeIssuer.util :refer [selfie-id is-badge-issuer? badge-valid?]]
   [salava.core.util :refer [get-site-url bytes->base64 hex-digest now get-full-path get-db get-db-1 file-from-url-fix md->html get-db-col]]
   [salava.profile.db :refer [user-information]]
   [salava.user.db :refer [primary-email]]
   [slingshot.slingshot :refer :all]))

(defn badge-assertion
  "Return Assertion as response body with appropriate status codes
   https://www.imsglobal.org/sites/default/files/Badges/OBv2p0Final/index.html#Assertion
   200 -> valid badge
   404 -> deleted badge
   410 -> revoked badge
   500 -> server error"
  [ctx user-badge-id]
  (try+
   (let [{:keys [id deleted revoked assertion_url]} (badge-valid? ctx user-badge-id)]
     (cond
       (and id (zero? deleted) (zero? revoked)) (ok (some-> (db/get-assertion-json {:id user-badge-id} (into {:result-set-fn first
                                                                                                              :row-fn :assertion_json} (get-db ctx)))
                                                            (json/read-str :key-fn keyword)))
       (or (nil? id) (pos? deleted))            (not-found "Badge assertion not found")
       (pos? revoked)                           (gone {:revoked true :id assertion_url})))
   (catch Object _
     (log/error (.getMessage _))
     (internal-server-error))))

(defn badge-issuer
  "Return Profile -> https://www.imsglobal.org/sites/default/files/Badges/OBv2p0Final/index.html#Profile"
  [ctx cid uid]
  (let [{:keys [name description image_file]} (db/get-issuer-information {:id cid} (into {:result-set-fn first} (get-db ctx)))]
    {:id (str (get-full-path ctx) "/obpv1/selfie/_/issuer?cid=" cid "&uid=" uid)
     :name name
     :description description
     :type "Profile"
     :url (if (pos? uid) (str (get-full-path ctx) "/profile/" uid) (get-site-url ctx))
     :email "no-reply@openbadgepassport.com"
     :image (str (get-site-url ctx) "/" image_file)
     (keyword "@context") "https://w3id.org/openbadges/v2"}))

(defn badge-criteria
  [ctx id]
  (some-> (db/get-criteria-page-information {:url (str (get-full-path ctx) "/selfie/criteria/" id)} #_{:bid badge_id :url id} (into {:result-set-fn first} (get-db ctx)))
          (update :criteria_content md->html)))

(defn get-badge
  "Return BadgeClass -> https://www.imsglobal.org/sites/default/files/Badges/OBv2p0Final/index.html#BadgeClass"
  [ctx user-badge-id uid]
  (let [badge_id (db/select-badge-id-by-user-badge-id {:user_badge_id user-badge-id} (into {:result-set-fn first :row-fn :badge_id} (get-db ctx)))
        badge (db/select-multi-language-badge-content {:id badge_id} (into {:result-set-fn first} (get-db ctx)))
        {:keys [id badge_id name badge_content_id description image criteria_url criteria_content_id criteria_content issuer_content_id issuer_url]} badge
        tags (vec (db/select-badge-tags {:id badge_content_id} (into {:row-fn :tag} (get-db ctx))))]

    {:id (str (get-full-path ctx) "/obpv1/selfie/_/badge/" user-badge-id "?i=" uid)
     :type "BadgeClass"
     :name name
     :image (str (get-site-url ctx) "/" image)
     :description description
     :criteria {:id criteria_url ;(str (get-full-path ctx) "/selfie/criteria/" criteria_content_id)
                :narrative criteria_content}
     :issuer (str (get-full-path ctx) "/obpv1/selfie/_/issuer?cid=" issuer_content_id "&uid=" uid)
     :tags (if (seq tags) tags [])
     (keyword "@context") "https://w3id.org/openbadges/v2"}))

(defn issue-selfie-badge [ctx data user-id]
  (let [{:keys [selfie_id recipients expires_on issued_from_gallery]} data
        user-id (if issued_from_gallery 0 user-id)]
    (log/info "Got badge issue request for id" recipients)
    (try+
     (doseq [r recipients
             :let [email (primary-email ctx r)
                   recipient {:id r :email email}
                   data {:selfie_id selfie_id}]]
       (log/info "Creating assertion for " recipient)
       (bakery/bake-assertion ctx {:id selfie_id :user-id user-id :recipient recipient :expires_on expires_on}))
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
         tags (if (seq (:tags data)) (json/write-str (:tags data)) nil)
         selfie (-> data (assoc :id id :creator_id user-id :image image :tags tags)
                         (dissoc :issue_to_self))]

     (db/insert-selfie-badge<! selfie (get-db ctx))

     (when (pos? (:issue_to_self data))
       (issue-selfie-badge ctx {:selfie_id id :recipients [user-id]} user-id))

     {:status "success" :id id})
   (catch Object _
     (log/error (.getMessage _))
     {:status "error" :id "-1"})))

(defn issuing-history [ctx selfie-id user-id]
  (db/select-selfie-badge-issuing-history {:selfie_id selfie-id :issuer_id user-id} (get-db ctx)))

(defn revoke-selfie-badge! [ctx user-badge-id user-id]
  (log/error "Got revoke request for user-badge-id: " user-badge-id)
  (try+
   (if (is-badge-issuer? ctx user-badge-id user-id)
     (db/revoke-issued-selfie-badge! {:issuer_id user-id :id user-badge-id} (get-db ctx))
     (throw+ "Error! user trying to revoke badge they have not issued"))
   (log/info "Badge revoked!")
   {:status "success"}
   (catch Object _
     (log/error _)
     {:status "error" :message _})))

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
   (let [selfie-badge (first (db/user-selfie-badge ctx (:id user) id))
         ifg (if (:issuable_from_gallery selfie-badge) 1 0)
         tags (if (blank? (:tags selfie-badge)) nil (json/read-str (:tags selfie-badge)))]
     (-> selfie-badge
         (assoc :issuable_from_gallery ifg
                :tags tags
                :criteria_html (md->html (:criteria selfie-badge)))
         (dissoc :deleted :ctime :mtime :creator_id))))
  ([ctx user id md?]
   (if md?
     (-> (initialize ctx user id)
         (update :criteria md->html))
     (initialize ctx user id))))

(defn latest-selfie-badges
  "Get 5 most recently published selfie badges"
  [ctx user-id]
  (let [gallery_ids (db/select-user-gallery-ids {:user_id user-id} (get-db ctx))
        selfies (db/select-latest-selfie-badges {:user_id user-id} (get-db ctx))]
    (some->> selfies 
             (remove #(some (fn [b] (or (= (:gallery_id %) (:gallery_id b))
                                        (= (:selfie_id %) (:selfie_id b))))
                          gallery_ids)))))
