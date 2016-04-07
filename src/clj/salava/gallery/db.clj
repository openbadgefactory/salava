(ns salava.gallery.db
  (:require [yesql.core :refer [defqueries]]
            [clojure.set :refer [rename-keys]]
            [clojure.java.jdbc :as jdbc]
            [salava.core.helper :refer [dump]]
            [salava.core.util :refer [get-db]]
            [salava.core.countries :refer [all-countries all-countries-sorted]]
            [salava.page.main :as p]
            [salava.badge.main :as b]))

(defqueries "sql/gallery/queries.sql")

(defn public-badges-by-user
  "Return user's public badges"
  ([ctx user-id] (public-badges-by-user ctx user-id "public"))
  ([ctx user-id visibility]
   (select-users-public-badges {:user_id user-id :visibility visibility} (get-db ctx))))

(defn public-badges
  "Return badges visible in gallery. Badges can be searched by country ID, badge name, issuer name or recipient's first or last name"
  [ctx country badge-name issuer-name recipient-name]
  (let [where ""
        params []
        default-visibility " AND (b.visibility = 'public' OR b.visibility = 'internal')"
        [where params] (if-not (or (empty? country) (= country "all"))
                         [(str where " AND u.country = ?") (conj params country)]
                         [where params])
        [where params] (if-not (empty? badge-name)
                         [(str where " AND bc.name LIKE ?") (conj params (str "%" badge-name "%"))]
                         [where params])
        [where params] (if-not (empty? issuer-name)
                         [(str where " AND ic.name LIKE ?") (conj params (str "%" issuer-name "%"))]
                         [where params])
        [where params] (if-not (empty? recipient-name)
                         [(str where " AND CONCAT(u.first_name,' ',u.last_name) LIKE ?") (conj params (str "%" recipient-name "%"))]
                         [where params])
        [where params] (if (and (not (empty? issuer-name)) (empty? recipient-name)) ;if issuer name is present but recipient name is not, search also private badges
                         [where params]
                         [(str where default-visibility) params])
        query (str "SELECT bc.id, bc.name, bc.image_file, bc.description, b.mtime, ic.name AS issuer_content_name, ic.url AS issuer_content_url, MAX(b.ctime) AS ctime, COUNT(b.user_id) AS recipients FROM badge AS b
                    JOIN badge_content AS bc ON b.badge_content_id = bc.id
                    JOIN issuer_content AS ic ON b.issuer_content_id = ic.id
                    LEFT JOIN user AS u ON b.user_id = u.id
                    WHERE b.status = 'accepted' AND b.deleted = 0 AND b.revoked = 0 AND (b.expires_on IS NULL OR b.expires_on > UNIX_TIMESTAMP())"
                   where
                   " GROUP BY bc.id
                    ORDER BY b.ctime DESC
                    LIMIT 100")]
    (jdbc/with-db-connection
      [conn (:connection (get-db ctx))]
      (jdbc/query conn (into [query] params)))))

(defn user-country
  "Return user's country id"
  [ctx user-id]
  (select-user-country {:id user-id} (into {:row-fn :country :result-set-fn first} (get-db ctx))))

(defn all-badge-countries [ctx]
  "Return all countries which users have public badges"
  (let [country-keys (select-badge-countries {} (into {:row-fn :country} (get-db ctx)))]
    (select-keys all-countries country-keys)))

(defn all-page-countries [ctx]
  "Return all countries which users have public pages"
  (let [country-keys (select-page-countries {} (into {:row-fn :country} (get-db ctx)))]
    (select-keys all-countries country-keys)))

(defn badge-countries
  "Return user's country id and list of all countries which users have public badges"
  [ctx user-id]
  (let [current-country (user-country ctx user-id)
        countries (merge (all-badge-countries ctx) (select-keys all-countries [current-country]))]
    (hash-map :countries (into (sorted-map-by
                                 (fn [a b] (compare (countries a) (countries b)))) countries)
              :user-country current-country)))

(defn page-countries
  "Return user's country id and list of all countries which users have public pages"
  [ctx user-id]
  (let [current-country (user-country ctx user-id)
        countries (merge (all-page-countries ctx) (select-keys all-countries [current-country]))]
    (hash-map :countries (into (sorted-map-by
                                 (fn [a b] (compare (countries a) (countries b)))) countries)
              :user-country current-country)))

(defn public-badge-content
  "Return data of the public badge by badge-content-id. Fetch badge criteria and issuer data. If user has not received the badge use most recent criteria and issuer. Fetch also average rating of the badge, rating count and recipient count"
  [ctx badge-content-id user-id]
  (let [badge-content (select-common-badge-content {:id badge-content-id} (into {:result-set-fn first} (get-db ctx)))
        recipient-badge-data (select-badge-criteria-issuer-by-recipient {:badge_content_id badge-content-id :user_id user-id} (into {:result-set-fn first} (get-db ctx)))
        badge-data (or recipient-badge-data (select-badge-criteria-issuer-by-date {:badge_content_id badge-content-id} (into {:result-set-fn first} (get-db ctx))))
        rating-and-recipient (select-common-badge-rating-and-recipient {:badge_content_id badge-content-id} (into {:result-set-fn first} (get-db ctx)))
        recipients (if user-id (select-badge-recipients {:badge_content_id badge-content-id} (get-db ctx)))
        badge (merge badge-content badge-data rating-and-recipient)]
    (hash-map :badge (b/badge-issued-and-verified-by-obf ctx badge)
              :public_users (->> recipients
                                 (filter #(not= (:visibility %) "private"))
                                 (map #(dissoc % :visibility))
                                 distinct)
              :private_user_count (->> recipients
                                       (filter #(= (:visibility %) "private"))
                                       count))))

(defn public-pages-by-user
  "Return all public pages owned by user"
  ([ctx user-id] (public-badges-by-user ctx user-id "public"))
  ([ctx user-id visibility]
   (let [pages (select-users-public-pages {:user_id user-id :visibility visibility} (get-db ctx))]
     (p/page-badges ctx pages))))

(defn public-pages
  "Return public pages visible in gallery. Pages can be searched with page owner's name and/or country"
  [ctx country owner]
  (let [where ""
        params []
        [where params] (if-not (or (empty? country) (= country "all"))
                         [(str where " AND u.country = ?") (conj params country)]
                         [where params])
        [where params] (if-not (empty? owner)
                         [(str where " AND CONCAT(u.first_name,' ',u.last_name) LIKE ?") (conj params (str "%" owner "%"))]
                         [where params])
        query (str "SELECT p.id, p.ctime, p.mtime, user_id, name, description, u.first_name, u.last_name, u.profile_picture, GROUP_CONCAT(pb.badge_id) AS badges FROM page AS p
                    JOIN user AS u ON p.user_id = u.id
                    LEFT JOIN page_block_badge AS pb ON pb.page_id = p.id
                    WHERE (visibility = 'public' OR visibility = 'internal')"
                   where
                   " GROUP BY p.id
                    ORDER BY p.mtime DESC
                    LIMIT 100")
        pages (jdbc/with-db-connection
                [conn (:connection (get-db ctx))]
                (jdbc/query conn (into [query] params)))]
    (p/page-badges ctx pages)))

(defn profile-countries [ctx user-id]
  (let [current-country (user-country ctx user-id)
        countries (select-profile-countries {} (into {:row-fn :country} (get-db ctx)))]
    (select-keys all-countries-sorted (conj countries current-country))))

(defn public-profiles
  "Searcn public user profiles by user's name and country"
  [ctx search-params user-id]
  (let [{:keys [name country common_badges]} search-params
        where ""
        params [user-id]
        [where params] (if-not (or (empty? country) (= country "all"))
                         [(str where " AND country = ?") (conj params country)]
                         [where params])
        [where params] (if-not (empty? name)
                         [(str where " AND CONCAT(first_name,' ',last_name) LIKE ?") (conj params (str "%" name "%"))]
                         [where params])
        having (if common_badges " HAVING common_badge_count > 0")
        query (str "SELECT id AS uid, first_name, last_name, country, profile_picture, ctime, (SELECT COUNT(DISTINCT badge_content_id) FROM badge WHERE user_id = uid AND status = 'accepted' AND deleted = 0 AND (expires_on IS NULL OR expires_on > UNIX_TIMESTAMP()) AND badge_content_id IN (SELECT DISTINCT badge_content_id FROM badge WHERE user_id = ? AND status = 'accepted' AND deleted = 0 AND (expires_on IS NULL OR expires_on > UNIX_TIMESTAMP()))) AS common_badge_count
                    FROM user
                    WHERE profile_visibility = 'public'"
                   where
                   having
                   " LIMIT 100")
        profiles (jdbc/with-db-connection
                   [conn (:connection (get-db ctx))]
                   (jdbc/query conn (into [query] params)))]
    (map #(rename-keys % {:uid :id}) profiles)))



