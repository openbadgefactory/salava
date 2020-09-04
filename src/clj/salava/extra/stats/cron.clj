(ns salava.extra.stats.cron
  (:require [clojure.tools.logging :as log]
            [clojure.java.io :as io]
            [clojure.java.jdbc :as jdbc]
            [clojure.string :as string]
            [salava.core.util :as u]
            [salava.core.http :as http]
            [clojure.data.json :as json]
            [salava.extra.stats.db :as db]))

(defn- push-stats [ctx badge-hits]
  (when (get-in ctx [:config :factory])
    (let [payload (json/write-str {:social_hits badge-hits
                                   :source (get-in ctx [:config :core :site-url])
                                   :ts (System/currentTimeMillis)})
          checksum (u/hmac-sha256-hex payload (get-in ctx [:config :factory :secret]))
          url (str (get-in ctx [:config :factory :url]) "/c/badge/passport_stats?c=" checksum)]
      (http/http-post url {:body payload :throw-exceptions false})
      payload)))

(defn- id->assertion-url [ctx badge-hits]
  (let [db-conn (:connection (u/get-db ctx))
        sql "SELECT assertion_url AS url FROM user_badge WHERE id = ?"]
    (reduce (fn [coll id]
              (let [a-url (some->> (jdbc/query db-conn [sql id]) first :url)]
                (if (and a-url (re-find #"^https?://[^\?&;#]+/v[0-9]/assertion/\w+\.json" a-url))
                  (assoc coll a-url (get badge-hits id))
                  coll)))
            {} (keys badge-hits))))

(defn- hit-count [filename services]
  (with-open [rdr (io/reader filename)]
    (reduce (fn [out line]
              (reduce (fn [coll [service regex]]
                        (if-let [[_ id] (re-find regex line)]
                          (update-in coll [id service] #(if (nil? %) 1 (inc %)))
                          coll))
                      out services))
            {} (line-seq rdr))))

(defn every-hour [ctx]
  (let [conf (get-in ctx [:config :extra/stats])]
    (when (= (get-in ctx [:-cron :hour]) (get conf :run-at 1))
     (log/info "stats/hit-count: started working")
     (->> (hit-count (:source-file conf) (:services conf))
          (id->assertion-url ctx)
          (push-stats ctx)
          (db/log-to-db ctx))
     (log/info "stats/hit-count: done"))))
