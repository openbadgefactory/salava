(ns salava.metabadge.cron
  (:require [salava.core.util :as u]
            [salava.core.http :as http]
            [clojure.java.jdbc :as jdbc]
            [clojure.tools.logging :as log]
            [salava.metabadge.metabadge :as mb]
            [salava.metabadge.db :as db]
            [clojure.string :as string]))

(defn- fetch-json-data [url]
  (try
    (http/http-get url {:as :json :accept :json})
    (catch Exception e
      (log/error (str "Could not fetch data: " url))
      {})))

(defn- metabadge?!
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
                          (:id user_badge) m (:required_badge data) (:last_modified data)]))))))

;;TODO why does it skip some badges
(defn- get-metabadge! [ctx db-conn factory-url user_badge]
  (let [url (str factory-url "/v1/assertion/metabadge/?url=" (u/url-encode (:assertion_url user_badge)))
        metabadges (:metabadge (fetch-json-data url))]
    (when-not (empty? metabadges)
      (doseq [metabadge metabadges
              :let [metabadge-badge-content (when-not (-> metabadge :badge :received) (fetch-json-data (-> metabadge :badge :url)))
                    user_metabadge (when (-> metabadge :badge :received) (db/user-badge-by-assertion ctx (-> metabadge :badge :url)))
                    {:keys [description criteria image]} metabadge-badge-content]]

        (if-not (-> metabadge :badge :received)
          (jdbc/execute! db-conn
                         ["INSERT IGNORE INTO factory_metabadge (id, name, description, criteria, image_file, min_required, ctime, mtime ) VALUES (?,?,?,?,?,?,?,?)"
                          (:id metabadge) (:name metabadge) description (u/md->html criteria) (u/file-from-url ctx image) (:min_required metabadge) (u/now) (u/now)])

          (doseq [um user_metabadge]
            (jdbc/execute! db-conn
                           ["INSERT IGNORE INTO user_metabadge_received (metabadge_id, user_badge_id, user_id, name, min_required, last_modified) VALUES (?,?,?,?,?,?)"
                            (:id metabadge) (:id um) (:user_id user_badge) (:name metabadge) (:min_required metabadge) (:last_modified user_badge) ])
            ))
        (doseq [badge (:required_badges metabadge)
                :let [required-badge-content (when-not (:received badge) (fetch-json-data (:url badge)))
                      user-badge (when (:received badge) (db/user-badge-by-assertion ctx (:url badge)))
                      {:keys [name description image criteria]} required-badge-content]]
          (if-not (:received badge)
            (jdbc/execute! db-conn
                           ["INSERT IGNORE INTO factory_metabadge_required (metabadge_id, required_badge_id, name, description, criteria, image_file, ctime, mtime) VALUES (?,?,?,?,?,?,?,?)"
                            (:id metabadge) (:id badge) name description (u/md->html criteria) (u/file-from-url ctx image) (u/now) (u/now)])
            (doseq [um user_metabadge]
              (doseq [ub user-badge]
                (jdbc/execute! db-conn
                               ["INSERT IGNORE INTO user_metabadge_required_received (metabadge_id, user_metabadge_id, user_required_badge_id, user_id, last_modified) VALUES (?,?,?,?,?)"
                                (:id metabadge) (:id um) (:id ub) (:user_id user_badge) (:last_modified user_badge)])))
            ))))))

(defn- badge->metabadge! [ctx factory-url]
  (log/info "get metabadges: started working")
  (jdbc/with-db-transaction  [db-conn (:connection (u/get-db ctx))]
    (let [time-limit (+ (System/currentTimeMillis) (* 15 60 1000))
          sql "SELECT DISTINCT ub.id, ub.user_id, ub.assertion_url, ubm.last_modified FROM user_badge_metabadge AS ubm
          JOIN user_badge AS ub ON ubm.user_badge_id = ub.id
          WHERE ubm.meta_badge IS NOT NULL OR ubm.meta_badge_req IS NOT NULL"
          badges (jdbc/query db-conn [sql])]

      (doseq [chunk (partition-all 10 badges)]
        (when (> time-limit (System/currentTimeMillis))
          (doseq [user-badge chunk]
            (try
              (get-metabadge! ctx db-conn factory-url user-badge)
              (catch Throwable ex
                (log/error "get-metabadges failed")
                (log/error (.toString ex))
                )
              )
            )
          (try (Thread/sleep 1000) (catch InterruptedException _))
          ))
      (log/info "get metabadges: done")
      )))

(defn- check-metabadges [ctx factory-url]
  (log/info "check for metabadges: started working")
  (jdbc/with-db-transaction  [db-conn (:connection (u/get-db ctx))]
    (let [time-limit (+ (System/currentTimeMillis) (* 15 60 1000))
          sql "SELECT id, assertion_url FROM user_badge
          WHERE assertion_url IS NOT NULL
          AND deleted = 0 AND revoked = 0 AND status != 'declined'
          AND (expires_on IS NULL OR expires_on > UNIX_TIMESTAMP())
          ORDER BY last_checked LIMIT 1000"
          ; all-badges (jdbc/query db-conn [sql])
          badges (filter #(and (not (nil? (:assertion_url %))) (string/starts-with? (:assertion_url %) factory-url)) (jdbc/query db-conn [sql]))
          ]
      (doseq [chunk (partition-all 10 badges)]
        (when (> time-limit (System/currentTimeMillis))
          (doseq [user-badge chunk]
            (try
              (metabadge?! db-conn factory-url user-badge)
              (catch Throwable ex
                (log/error "check-metabadges failed")
                (log/error (.toString ex))
                )
              )
            )
          (try (Thread/sleep 1000) (catch InterruptedException _))
          )
        )
      ;;call other function
      )
    (log/info "check for metabadges: done")
    (badge->metabadge! ctx factory-url)
    ))

(defn every-hour [ctx]
  (check-metabadges ctx (get-in ctx [:config :factory :url])))
