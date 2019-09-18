(ns salava.metabadge.cron
  (:require [salava.core.util :as u]
            [salava.core.http :as http]
            [clojure.java.jdbc :as jdbc]
            [clojure.tools.logging :as log]
            [salava.metabadge.metabadge :as mb]
            [salava.metabadge.db :as db]
            [clojure.string :as string]))

(defn- badges->metabadges! [ctx factory-url]
  (log/info "badges->metabadges: started working")
  (let [time-limit (+ (System/currentTimeMillis) (* 15 60 1000))
        badges (db/all-metabadges ctx)]
    (doseq [chunk (partition-all 10 badges)]
      (when (> time-limit (System/currentTimeMillis))
        (doseq [user-badge chunk]
          (try
            (db/get-metabadge! ctx factory-url user-badge)
            (catch Throwable ex
              (log/error "badges->metabadges: failed")
              (log/error (.toString ex)))))
        (try (Thread/sleep 1000) (catch InterruptedException _))))
    (log/info "badges->metabadges: done")))

(defn- check-metabadges [ctx factory-url]
  (log/info "check for metabadges: started working")
  (let [time-limit (+ (System/currentTimeMillis) (* 15 60 1000))
        badges (db/all-badges ctx factory-url)]
    (doseq [chunk (partition-all 10 badges)]
      (when (> time-limit (System/currentTimeMillis))
        (doseq [user-badge chunk]
          (try
            (db/metabadge?! ctx factory-url user-badge)
            (catch Throwable ex
              (log/error "check-metabadges failed")
              (log/error (.toString ex)))))
        (try (Thread/sleep 1000) (catch InterruptedException _))))
    (log/info "check for metabadges: done")
    ;(badges->metabadges! ctx factory-url) ; Disabled, metabadge content updates are sent from factory instead.
    ))

(defn every-hour [ctx]
  (check-metabadges ctx (get-in ctx [:config :factory :url])))
