(ns salava.gallery.db
  (:require [yesql.core :refer [defqueries]]
            [clojure.java.jdbc :as jdbc]
            [salava.core.helper :refer [dump]]
            [salava.core.util :refer [get-db]]
            [salava.core.countries :refer [all-countries]]))

(defqueries "sql/gallery/queries.sql")

(defn public-badges-by-user [ctx user-id]
  (select-users-public-badges {:user_id user-id} (get-db ctx)))

(defn public-badges
  "Returns badges visible in gallery"
  [ctx country badge-name issuer-name recipient-name]
  (let [where ""
        params []
        [where params] (if-not (empty? country)
                         [(str where " AND u.country = ?") (conj params country)]
                         [where params])
        [where params] (if-not (empty? badge-name)
                         [(str where " AND bc.name LIKE ?") (conj params (str "%" badge-name "%"))]
                         [where params])
        [where params] (if-not (empty? issuer-name)
                         [(str where " AND ic.name LIKE ?") (conj params (str "%" issuer-name "%"))]
                         [where params])
        [where params] (if-not (empty? recipient-name)
                         [(str where " AND (u.first_name LIKE ? OR u.last_name LIKE ?)") (conj params (str recipient-name "%") (str recipient-name "%"))]
                         [where params])
        query (str "SELECT bc.id, bc.name, bc.image_file, bc.description, b.mtime, ic.name AS issuer_name, ic.url AS issuer_url, MAX(b.ctime) AS ctime, COUNT(DISTINCT b.user_id) AS recipients FROM badge AS b
                    JOIN badge_content AS bc ON b.badge_content_id = bc.id
                    JOIN issuer_content AS ic ON b.issuer_content_id = ic.id
                    LEFT JOIN user AS u ON b.user_id = u.id
                    WHERE b.status = 'accepted' AND b.deleted = 0 AND b.revoked = 0 AND (b.expires_on IS NULL OR b.expires_on > UNIX_TIMESTAMP()) AND b.visibility = 'public'GROUP BY bc.id
                    ORDER BY b.ctime DESC
                    LIMIT 100")]
    (jdbc/with-db-connection
      [conn (:connection (get-db ctx))]
      (jdbc/query conn (into [query] params)))))

(defn user-country
  "Get user's country"
  [ctx user-id]
  (select-user-country {:id user-id} (into {:row-fn :country :result-set-fn first} (get-db ctx))))

(defn all-badge-countries [ctx]
  (let [country-keys (select-badge-countries {} (into {:row-fn :country} (get-db ctx)))]
    (select-keys all-countries country-keys)))

(defn badge-countries
  ""
  [ctx user-id]
  (let [current-country (user-country ctx user-id)
        countries (merge (all-badge-countries ctx) (select-keys all-countries [current-country]))]
    (hash-map :countries (into (sorted-map-by
                                 (fn [a b] (compare (countries a) (countries b)))) countries)
              :country current-country)))

