(ns salava.metabadge.cron
  (:require [salava.core.util :as u]
            [salava.core.http :as http]
            [clojure.java.jdbc :as jdbc]
            [clojure.tools.logging :as log]
            [salava.metabadge.metabadge :as mb]
            [salava.metabadge.db :as db]
            [clojure.string :as string]))

(defn- badge->metabadge! [ctx db-conn factory-url]
  (log/info "get metabadges: started working")
    (let [time-limit (+ (System/currentTimeMillis) (* 15 60 1000))
          sql "SELECT DISTINCT ub.id, ub.user_id, ub.assertion_url, ubm.last_modified FROM user_badge_metabadge AS ubm
          JOIN user_badge AS ub ON ubm.user_badge_id = ub.id
          WHERE ubm.meta_badge IS NOT NULL OR ubm.meta_badge_req IS NOT NULL"
          badges (jdbc/query db-conn [sql])]

      (doseq [chunk (partition-all 10 badges)]
        (when (> time-limit (System/currentTimeMillis))
          (doseq [user-badge chunk]
            (try
              (db/get-metabadge! ctx db-conn factory-url user-badge)
              (catch Throwable ex
                (log/error "get-metabadges failed")
                (log/error (.toString ex))
                )
              )
            )
          (try (Thread/sleep 1000) (catch InterruptedException _))
          ))
      (log/info "get metabadges: done")
      ))

(defn- check-metabadges [ctx factory-url]
  (log/info "check for metabadges: started working")
  (jdbc/with-db-transaction  [db-conn (:connection (u/get-db ctx))]
    (let [time-limit (+ (System/currentTimeMillis) (* 15 60 1000))
          sql "SELECT id, assertion_url FROM user_badge
          WHERE assertion_url IS NOT NULL
          AND deleted = 0 AND revoked = 0 AND status != 'declined'
          AND (expires_on IS NULL OR expires_on > UNIX_TIMESTAMP())
          ORDER BY last_checked LIMIT 1000"
          badges (filter #(and (not (nil? (:assertion_url %))) (string/starts-with? (:assertion_url %) factory-url)) (jdbc/query db-conn [sql]))]
      (doseq [chunk (partition-all 10 badges)]
        (when (> time-limit (System/currentTimeMillis))
          (doseq [user-badge chunk]
            (try
              (db/metabadge?! db-conn factory-url user-badge)
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
    (badge->metabadge! ctx db-conn factory-url)
    ))

(defn every-hour [ctx]
  (check-metabadges ctx (get-in ctx [:config :factory :url])))
