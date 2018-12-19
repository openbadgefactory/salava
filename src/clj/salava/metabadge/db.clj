(ns salava.metabadge.db
  (:require [yesql.core :refer [defqueries]]
            [salava.core.util :as u]
            [salava.core.http :as http]
            [clojure.tools.logging :as log]
            [clojure.string :refer [blank?]]
            [clojure.java.jdbc :as jdbc]))

(defqueries "sql/metabadge/queries.sql")

(defn- revoked? [asr]
  (or (= (:status asr) 410) (get-in asr [:body :revoked])))

(defn- assertion [assertion-url]
  (http/http-req {:socket-timeout 10000
                  :conn-timeout   10000
                  :method :get
                  :url assertion-url
                  :throw-exceptions false
                  :as :json
                  :accept :json}))

(defn- fetch-json-data [url]
  (try
    (http/http-get url {:as :json :accept :json})
    (catch Exception e
      (log/error (str "Could not fetch data: " url))
      {})))

(defn user-badge-by-assertion
  ;"in case of multiple hits return the most recent"
  [ctx assertion_url]
  (some-> (select-user-badge-by-assertion-url {:assertion_url assertion_url} (u/get-db ctx))))

(defn metabadge?!
  "checks if badge is a metabadge, db is updated with information"
  [db-conn factory-url user_badge]
  (let [url (str factory-url "/v1/assertion/is_metabadge/?url=" (u/url-encode (:assertion_url user_badge)))
        data (fetch-json-data url)]
    (when-not (every? nil? (-> data (dissoc :last_modified) vals))
      (if (nil? (:metabadge data))
        (jdbc/execute! db-conn
                       ["INSERT IGNORE INTO user_badge_metabadge (user_badge_id, meta_badge_req, last_modified) VALUES (?,?,?)"
                        (:id user_badge) (:required_badge data) (:last_modified data)])
        (doseq [m (:metabadge data)]
          (jdbc/execute! db-conn
                         ["INSERT IGNORE INTO user_badge_metabadge (user_badge_id, meta_badge, meta_badge_req, last_modified) VALUES (?,?,?,?)"
                          (:id user_badge) m (:required_badge data) (:last_modified data)]))))
    (if (empty? data) false true)))

#_(defn save-required-badges! [ctx db-conn metabadge user_badge user_metabadge_id]
    (log/info "saving required badges" (:id metabadge) "," user_metabadge_id)
    (doseq [badge (:required-badges metabadge)
            :let [required-badge-content (when-not (:received badge) (fetch-json-data (:url badge)))
                  user-badge (when (:received badge) (user-badge-by-assertion ctx (:url badge)))
                  {:keys [name description image criteria]} required-badge-content]]
      (if-not (:received badge)
        (jdbc/execute! db-conn
                       ["INSERT IGNORE INTO factory_metabadge_required (metabadge_id, required_badge_id, name, description, criteria, image_file, ctime, mtime) VALUES (?,?,?,?,?,?,?,?)"
                        (:id metabadge) (:id badge) name description (u/md->html criteria) (u/file-from-url ctx image) (u/now) (u/now)])
        (doseq [ub user-badge]
          (jdbc/execute! db-conn
                         ["INSERT IGNORE INTO user_metabadge_required_received (metabadge_id, user_metabadge_id, user_required_badge_id, user_id, last_modified) VALUES (?,?,?,?,?)"
                          (:id metabadge) user_metabadge_id (:id ub) (:user_id user_badge) (:last_modified user_badge)])))))

;;TODO Fix
(defn get-metabadge! [ctx db-conn factory-url user_badge]
  (let [url (str factory-url "/v1/assertion/metabadge/?url=" (u/url-encode (:assertion_url user_badge)))
        metabadges (:metabadge (fetch-json-data url))]
    (try
      (when-not (empty? metabadges)
        (doseq [metabadge metabadges
                :let [metabadge-badge-content (when-not (-> metabadge :badge :received) (fetch-json-data (-> metabadge :badge :url)))
                      user_metabadge (when (-> metabadge :badge :received) (user-badge-by-assertion ctx (-> metabadge :badge :url)))
                      {:keys [description criteria image]} metabadge-badge-content]]

          (if-not (-> metabadge :badge :received)
            (jdbc/execute! db-conn
                           ["INSERT IGNORE INTO factory_metabadge (id, name, description, criteria, image_file, min_required, ctime, mtime ) VALUES (?,?,?,?,?,?,?,?)"
                            (:id metabadge) (:name metabadge) description (u/md->html criteria) (u/file-from-url ctx image) (:min_required metabadge) (u/now) (u/now)])

            (doseq [um user_metabadge]
              (jdbc/execute! db-conn
                             ["INSERT IGNORE INTO user_metabadge_received (metabadge_id, user_badge_id, user_id, name, min_required, last_modified) VALUES (?,?,?,?,?,?)"
                              (:id metabadge) (:id um) (:user_id user_badge) (:name metabadge) (:min_required metabadge) (:last_modified user_badge) ])))


          (doseq [badge (:required_badges metabadge)
                  :let [required-badge-content (when-not (:received badge) (fetch-json-data (:url badge)))
                        user-badge (when (:received badge) (user-badge-by-assertion ctx (:url badge)))
                        {:keys [name description image criteria]} required-badge-content]]
            (if-not (:received badge)
              (jdbc/execute! db-conn
                             ["INSERT IGNORE INTO factory_metabadge_required (metabadge_id, required_badge_id, name, description, criteria, image_file, ctime, mtime) VALUES (?,?,?,?,?,?,?,?)"
                              (:id metabadge) (:id badge) name description (u/md->html criteria) (u/file-from-url ctx image) (u/now) (u/now)])
              (doseq [ub user-badge]
                (if (empty? user_metabadge)
                  (jdbc/execute! db-conn
                                 ["INSERT IGNORE INTO user_metabadge_required_received (metabadge_id, user_required_badge_id, user_id, last_modified) VALUES (?,?,?,?)"
                                  (:id metabadge) (:id ub) (:user_id user_badge) (:last_modified user_badge)])
                  (doseq [um user_metabadge]
                    (jdbc/execute! db-conn
                                   ["INSERT IGNORE INTO user_metabadge_required_received (metabadge_id, user_metabadge_id, user_required_badge_id, user_id, last_modified) VALUES (?,?,?,?,?)"
                                    (:id metabadge) (:id um) (:id ub) (:user_id user_badge) (:last_modified user_badge)]))
                  )

                )))))
      (catch Exception e
        (log/error (.getMessage e))))))

(defn clean-metabadge-tables
  "delete entries in user_badge_metabadge, user_metabadge_received and user_metabadge_required_received"
  [ctx db-conn user-badge-id]
  (jdbc/with-db-transaction [t-con db-conn]
    (jdbc/execute! db-conn
                   ["DELETE FROM user_badge_metabadge WHERE user_badge_id = ?"
                     user-badge-id])
    (jdbc/execute! db-conn
                   ["DELETE FROM user_metabadge_received WHERE user_badge_id = ?"
                     user-badge-id])
    (jdbc/execute! db-conn
                   ["DELETE FROM user_metabadge_required_received WHERE user_required_badge_id = ?"
                     user-badge-id])))
