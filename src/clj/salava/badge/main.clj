(ns salava.badge.main
  (:require [yesql.core :refer [defqueries]]
            [clojure.string :refer [blank?]]
            [slingshot.slingshot :refer :all]
            [salava.core.time :refer [unix-time]]
            [salava.core.i18n :refer [t]]
            [salava.core.helper :refer [dump]]
            [salava.core.util :refer [get-db map-sha256 file-from-url]]))

(defqueries "sql/badge/main.sql")

(defn assoc-badge-tags [badge tags]
  (assoc badge :tags (map :tag (filter #(= (:badge_id %) (:id badge))
                                       tags))))
(defn map-badges-tags [badges tags]
  (map (fn [b] (assoc-badge-tags b tags))
       badges))

(defn user-badges-all
  "Returns all the badges of a given user"
  [ctx userid]
  (let [badges (select-user-badges-all {:user_id userid} (get-db ctx))
        tags (if-not (empty? badges) (select-taglist {:badge_id (map :id badges)} (get-db ctx)))]
    (map-badges-tags badges tags)))

(defn user-badges-to-export
  "Returns valid badges of a given user"
  [ctx userid]
  (let [badges (select-user-badges-to-export {:user_id userid} (get-db ctx))
        tags (if-not (empty? badges) (select-taglist {:badge_id (map :id badges)} (get-db ctx)))]
    (map-badges-tags badges tags)))

(defn user-badges-pending
  "Returns pending badges of a given user"
  [ctx userid]
  (let [badges (select-user-badges-pending {:user_id userid} (get-db ctx))
        tags (if-not (empty? badges) (select-taglist {:id (map :badge_content_id badges)} (get-db ctx)))]
    (map-badges-tags badges tags)))

(defn gallery-badges
  "Returns badges visible in gallery"
  [ctx]
  (select-public-badges {} (get-db ctx)))

(defn user-owns-badge?
  "Check if user owns badge"
  [ctx assertion user-id]
  (pos? (:count (if (= (get-in assertion [:verify :type]) "hosted")
                  (select-user-owns-hosted-badge
                    {:assertion_url (get-in assertion [:verify :url])
                     :user_id user-id}
                    (into {:result-set-fn first}
                          (get-db ctx)))
                  (select-user-owns-signed-badge
                    {:assertion_json (get-in assertion [:assertion_json])
                     :user_id user-id}
                    (into {:result-set-fn first}
                          (get-db ctx)))))))

(defn get-badge
  "Get badge by id"
  [ctx badge-id]
  (select-badge {:id badge-id}
             (into {:result-set-fn first}
                   (get-db ctx))))

(defn save-badge-content!
  "Save badge content"
  [ctx assertion image-file]
  (let [badge-data {:name (get-in assertion [:badge :name])
                    :description (get-in assertion [:badge :description])
                    :image_file image-file
                    :criteria_html (get-in assertion [:badge :criteria_html])
                    :criteria_markdown (get-in assertion [:badge :criteria_markdown])}
        badge-content-sha256 (map-sha256 badge-data)
        data (assoc badge-data :id badge-content-sha256)]
    (replace-badge-content! data (get-db ctx))
    badge-content-sha256))


(defn save-badge!
  "Save user's badge"
  [ctx user-id badge badge-content-id issuer-content-id]
  (let [data {:user_id             user-id
              :email               (get-in badge [:_email])
              :assertion_url       (get-in badge [:assertion :verify :url])
              :assertion_jws       (get-in badge [:assertion :assertion_jws])
              :assertion_json      (get-in badge [:assertion :assertion_json])
              :badge_url           (get-in badge [:assertion :badge :badge_url])
              :issuer_url          (get-in badge [:assertion :badge :issuer_url])
              :criteria_url        (get-in badge [:assertion :badge :criteria_url])
              :badge_content_id    badge-content-id
              :issuer_content_id   issuer-content-id
              :issued_on           (get-in badge [:assertion :issuedOn])
              :expires_on          (get-in badge [:assertion :expires])
              :evidence_url        (get-in badge [:assertion :evidence])
              :status              (get-in badge [:_status] "pending")
              :visibility          "private"
              :show_recipient_name 0
              :rating              0
              :ctime               (unix-time)
              :mtime               (unix-time)
              :deleted             0
              :revoked             0}]
    (insert-badge<! data (get-db ctx))))

(defn save-issuer-content!
  "Save issuer-data"
  [ctx assertion image-file]
  (let [issuer-data {:name (get-in assertion [:badge :issuer :name])
                     :description (get-in assertion [:badge :issuer :description])
                     :url (get-in assertion [:badge :issuer :url])
                     :image_file image-file
                     :email (get-in assertion [:badge :issuer :email])
                     :revocation_list_url (get-in assertion [:badge :issuer :revocationList])}
        issuer-content-sha256 (map-sha256 issuer-data)
        data (assoc issuer-data :id issuer-content-sha256)]
    (replace-issuer-content! data (get-db ctx))
    issuer-content-sha256))

(defn save-badge-from-assertion!
  [ctx badge user-id]
  (try+
    (let [assertion (:assertion badge)
          badge-image-path (file-from-url (get-in assertion [:badge :image]))
          badge-content-id (save-badge-content! ctx assertion badge-image-path)
          issuer-image (get-in assertion [:badge :issuer :image])
          issuer-image-path (if issuer-image
                              (file-from-url issuer-image))
          issuer-content-id (save-issuer-content! ctx assertion issuer-image-path)]
      (if (user-owns-badge? ctx (:assertion badge) user-id)
        (throw+ (t :badge/Alreadyowned)))
      (:generated_key (save-badge! ctx user-id badge badge-content-id issuer-content-id)))))

(defn save-badge-tags!
  "Save tags associated to badge. Delete existing tags."
  [ctx tags badge-id]
  (let [valid-tags (filter #(not (blank? %)) (distinct tags))]
    (delete-badge-tags! {:badge_id badge-id} (get-db ctx))
    (doall (for [tag valid-tags]
             (replace-badge-tag! {:badge_id badge-id :tag tag}
                              (get-db ctx))))))
(defn set-visibility!
  "Set badge visibility"
  [ctx badgeid visibility]
  (update-visibility! {:id badgeid
                      :visibility visibility}
                     (get-db ctx)))

(defn set-status!
  "Set badge status"
  [ctx badgeid status]
  (update-status! {:id badgeid
                   :status status}
                  (get-db ctx)))

(defn toggle-show-recipient-name!
  "Toggle recipient name visibility"
  [ctx badgeid show-recipient-name]
  (update-show-recipient-name! {:id badgeid
                       :show_recipient_name show-recipient-name}
                      (get-db ctx)))

(defn badge-settings
  "Get badge settings"
  [ctx badge-id]
  (let [badge (select-badge-settings {:id badge-id}
                                     (into {:result-set-fn first}
                                           (get-db ctx)))
        tags (select-taglist {:badge_id [badge-id]} (get-db ctx))]
    (assoc-badge-tags badge tags)))

(defn save-badge-settings!
  "Update badge setings"
  [ctx badge-id visibility evidence-url rating tags]
  (let [data {:id badge-id
              :visibility visibility
              :evidence_url evidence-url
              :rating rating}]
    (update-badge-settings! data (get-db ctx))
    (save-badge-tags! ctx tags badge-id)))

(defn delete-badge!
  "Set badge deleted and delete tags"
  [ctx badge-id]
  (delete-badge-tags! {:badge_id badge-id} (get-db ctx))
  (update-badge-set-deleted! {:id badge-id} (get-db ctx)))

(defn search-gallery-badges
  "Search badges from gallery"
  [country user-name badge-name issuer-name])


