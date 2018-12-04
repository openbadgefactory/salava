(ns salava.metabadge.cron
  (:require [salava.core.util :as u]
            [salava.core.http :as http]
            [clojure.java.jdbc :as jdbc]
            [clojure.tools.logging :as log]
            [salava.metabadge.metabadge :as mb]
            [clojure.string :as string]))

(defn- fetch-json-data [url]
  (try
    (http/http-get url {:as :json :accept :json})
    (catch Exception e
      (log/error (str "Could not fetch data: " url))
      {})))

(defn- metabadge?!
  "function checks if badge is a metabadge, db is updated with information"
  [db-conn factory-url user_badge]
  (let [url (str factory-url "/v1/assertion/is_metabadge/?url=" (u/url-encode (:assertion_url user_badge)))
        data (fetch-json-data url)
        ;metabadge? (if (:metabadge data) 1 0)
        ;required? (if (:required_badge data) 1 0)
        ]
    (if-not (empty? data)
      (jdbc/execute! db-conn
                     ["INSERT IGNORE INTO user_badge_metabadge (user_badge_id, meta_badge, meta_badge_req) VALUES (?,?,?)"
                      (:id user_badge) (:metabadge data) (:required_badge data)]))))

(defn- check-metabadges [db-conn factory-url]
  (log/info "check-metabadges: started working")
  (let [time-limit (+ (System/currentTimeMillis) (* 15 60 1000))
        sql "SELECT id, assertion_url FROM user_badge
        WHERE assertion_url IS NOT NULL
        AND deleted = 0 AND revoked = 0 AND status != 'declined'
        AND (expires_on IS NULL OR expires_on > UNIX_TIMESTAMP())
        ORDER BY last_checked LIMIT 1000"
        all-badges (jdbc/query db-conn [sql])
        badges (filter #(and (not (nil? (:assertion_url %))) (string/starts-with? (:assertion_url %) factory-url)) all-badges)
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
    )
  (log/info "check-badges: done")
  ;;call other function
  )

(defn every-hour [ctx]
  (check-metabadges (:connection (u/get-db ctx)) (get-in ctx [:config :factory :url])))
