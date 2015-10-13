(ns salava.badge.main
  (:require [yesql.core :refer [defqueries]]
            [clojure.string :refer [blank?]]
            [salava.core.time :refer [unix-time]]
            [salava.core.helper :refer [dump]]
            [salava.core.util :refer [get-db map-sha256]]))

(defqueries "sql/badge/main.sql")

(defn map-badges-tags [badges tags]
  (map (fn [b]
         (assoc b :tags
                  (map :tag
                       (filter #(= (:badge_id %) (:id b))
                               tags))))
       badges))

(defn userbadges
  "Returns all the badges of a given user"
  [ctx userid]
  (let [badges (select-user-badges-all {:user_id userid} (get-db ctx))
        tags (if-not (empty? badges) (select-taglist {:badge_id (map :id badges)} (get-db ctx)))]
    (map-badges-tags badges tags)))

(defn userbadges-export
  "Returns valid badges of a given user"
  [ctx userid]
  (let [badges (select-user-badges-valid {:user_id userid} (get-db ctx))
        tags (if-not (empty? badges) (select-taglist {:badge_id (map :id badges)} (get-db ctx)))]
    (map-badges-tags badges tags)))

(defn userbadges-pending
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

(defn badge
  "Get badge by id"
  [ctx badge-id]
  (select-get-badge {:id badge-id}
             (into {:result-set-fn first}
                   (get-db ctx))))

(defn save-badge-content
  "Save badge content"
  [ctx assertion image-file]
  (let [badge-data {:name (get-in assertion [:badge :name])
                    :description (get-in assertion [:badge :description])
                    :image_file image-file
                    :criteria_html (get-in assertion [:badge :criteria_html])
                    :criteria_markdown (get-in assertion [:badge :criteria_markdown])}
        badge-content-sha256 (map-sha256 badge-data)
        data (assoc badge-data :id badge-content-sha256)]
    (replace-save-badge-content! data (get-db ctx))
    badge-content-sha256))


(defn save-badge
  "Save user's badge"
  [ctx user-id badge badge-content-id issuer-content-id]
  (let [data {:user_id user-id
              :email (get-in badge [:_email])
              :assertion_url (get-in badge [:assertion :verify :url])
              :assertion_jws (get-in badge [:assertion :assertion_jws])
              :assertion_json (get-in badge [:assertion :assertion_json])
              :badge_url (get-in badge [:assertion :badge :badge_url])
              :issuer_url (get-in badge [:assertion :badge :issuer_url])
              :criteria_url (get-in badge [:assertion :badge :criteria_url])
              :badge_content_id badge-content-id
              :issuer_content_id issuer-content-id
              :issued_on (get-in badge [:assertion :issuedOn])
              :expires_on (get-in badge [:assertion :expires])
              :evidence_url (get-in badge [:assertion :evidence])
              :status "pending"
              :visibility "private"
              :show_recipient_name 0
              :rating 0
              :ctime (unix-time)
              :mtime (unix-time)
              :deleted 0
              :revoked 0}]
    (insert-save-badge<! data (get-db ctx))))

(defn save-badge-tags
  "Save tags associated to badge"
  [ctx tags badge-id]
  (let [valid-tags (filter #(not (blank? %)) (distinct tags))]
    (doall (for [tag valid-tags]
             (replace-save-badge-tag! {:badge_id badge-id :tag tag}
                              (get-db ctx))))))
(defn set-visibility
  "Set badge visibility"
  [ctx badgeid visibility]
  (update-visibility! {:id badgeid
                      :visibility visibility}
                     (get-db ctx)))

(defn toggle-show-recipient-name
  "Toggle recipient name visibility"
  [ctx badgeid show-recipient-name]
  (update-show-recipient-name! {:id badgeid
                       :show_recipient_name show-recipient-name}
                      (get-db ctx)))

(defn search-gallery-badges
  "Search badges from gallery"
  [country user-name badge-name issuer-name])


