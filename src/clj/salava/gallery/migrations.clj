(ns salava.gallery.migrations
  (:require [clojure.pprint :refer [pprint]]
            [clojure.string :as string]
            [clojure.data.json :as json]
            [clojure.java.jdbc :as jdbc]))

#_(def debug-conf {:conn {:dbtype "mysql"
                        :dbname "salava"
                        :user "salava"
                        :password "salava"}})


(defn- gallery-insert! [config row]
  (jdbc/execute! (:conn config) ["INSERT IGNORE INTO gallery (badge_name, badge_image, issuer_name) VALUES (?,?,?)"
                                 (:badge_name row) (:badge_image row) (:issuer_name row)])
  (when-let [gallery-id (some->
                          (jdbc/query (:conn config) ["SELECT id FROM gallery
                                                      WHERE badge_name = ? AND badge_image = ? AND issuer_name = ?"
                                                      (:badge_name row) (:badge_image row) (:issuer_name row)])
                          first :id)]
    (jdbc/execute! (:conn config) ["UPDATE user_badge         SET gallery_id = ? WHERE id = ?"       gallery-id (:user_badge_id row)])
    (jdbc/execute! (:conn config) ["UPDATE badge_message      SET gallery_id = ? WHERE badge_id = ?" gallery-id (:badge_id row)])
    (jdbc/execute! (:conn config) ["UPDATE badge_message_view SET gallery_id = ? WHERE badge_id = ?" gallery-id (:badge_id row)])

    (jdbc/execute! (:conn config) ["UPDATE gallery SET badge_id = ? WHERE id = ?" (:badge_id row) gallery-id])))

(defn gallery-table-insert-up [config]
  (let [total (-> (jdbc/query (:conn config) ["SELECT COUNT(*) AS count FROM user_badge"]) first :count)
        limit 1000]
    (doseq [offset (range 0 total limit)]
      (doseq [row (jdbc/query (:conn config) ["SELECT
                                                ub.id AS user_badge_id,
                                                ub.badge_id,
                                                b.name AS badge_name,
                                                b.image_file AS badge_image,
                                                i.name AS issuer_name
                                              FROM user_badge ub
                                              INNER JOIN badge ON ub.badge_id = badge.id
                                              INNER JOIN badge_badge_content bc ON badge.id = bc.badge_id
                                              INNER JOIN badge_content b ON bc.badge_content_id = b.id
                                              INNER JOIN badge_issuer_content ic ON badge.id = ic.badge_id
                                              INNER JOIN issuer_content i ON ic.issuer_content_id = i.id
                                              WHERE b.language_code = badge.default_language_code AND i.language_code = badge.default_language_code
                                              ORDER BY ub.ctime
                                              LIMIT ? OFFSET ?" limit offset])]
        (gallery-insert! config row)))))

(defn gallery-table-insert-down [config]
  (jdbc/execute! (:conn config) ["UPDATE user_badge         SET gallery_id = NULL"])
  (jdbc/execute! (:conn config) ["UPDATE badge_message      SET gallery_id = NULL"])
  (jdbc/execute! (:conn config) ["UPDATE badge_message_view SET gallery_id = NULL"])

  (jdbc/execute! (:conn config) ["DELETE FROM gallery"])
  (jdbc/execute! (:conn config) ["ALTER TABLE gallery AUTO_INCREMENT = 1"]))

;;;

#_(gallery-table-insert-up debug-conf)

#_(gallery-table-insert-down debug-conf)
