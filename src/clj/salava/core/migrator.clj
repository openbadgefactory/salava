(ns salava.core.migrator
  (:require [migratus.core :as migratus]
            [migratus.database :refer [find-migrations parse-migration-id]]
            [clojure.tools.logging :as log]
            [clojure.java.io :as io]
            [clojure.java.jdbc :as jdbc]
            [salava.core.util :refer [public-path]]))

(def dev-config (-> (clojure.java.io/resource "config/core.edn") slurp read-string))

(def test-config (-> (clojure.java.io/resource "test_config/core.edn") slurp read-string))

(def plugins (cons :core (:plugins dev-config)))

(def dev-ds (:datasource dev-config))

(def test-ds (:datasource test-config))

(defn jdbc-uri [source]
  (str "jdbc:" (:adapter source "mysql") "://"
       (:server-name source "localhost")  "/" (:database-name source)
       "?user=" (:username source)
       (if (not-empty (:password source)) (str "&password=" (:password source)))))


(def schema-table "schema_migrations")

;;;

(defn migration-dir [plugin]
  (str "migrations/" (name plugin) "/sql"))

(defn seed-dir [plugin]
  (str "migrations/" (name plugin) "/seed"))


(defn seed-insert [ds data-file]
  (doseq [seed (-> data-file slurp read-string)]
    (jdbc/insert! (jdbc-uri ds) (:table seed) (:data seed))))


(defn copy-file [source-path dest-path]
  (do
    (io/make-parents dest-path)
    (io/copy (io/file source-path) (io/file dest-path))))


(defn seed-copy [plugin]
  (when-let [data-dir (io/resource (str (seed-dir plugin) "/public"))]
    (doseq [source-file (-> data-dir io/as-file file-seq rest)]
      (copy-file source-file (str "resources/public/" (public-path source-file))))))


(defn migratus-config [ds plugin]
  {:store                :database
   :db                   (jdbc-uri ds)
   :migration-dir        (migration-dir plugin)
   :migration-table-name schema-table})


(defn applied-migrations
  ([ds]
   (try
     (map :id (jdbc/query (jdbc-uri ds) [(str "SELECT id from " schema-table " ORDER BY id ASC")]))
     (catch Exception e)))

  ([ds plugin]
   (let [applied (applied-migrations ds)
        available (->> (migration-dir plugin) find-migrations keys (map str) set)]
    (filter available (map str applied)))))


(defn run-down [ds plugin applied]
  (doseq [id applied]
    (migratus/down (migratus-config ds plugin) (parse-migration-id id))))


(defn run-seed [ds plugin]
  (when-let [data-file (io/resource (str (seed-dir plugin) "/data.edn"))]
    (log/info "running seed functions for plugin" (name plugin))
    (seed-insert ds data-file)
    (seed-copy plugin)))


(defn run-reset [ds plugin]
  (let [applied (reverse (applied-migrations ds plugin))]
    (do
      (log/info "running reset functions for plugin" (name plugin))
      (run-down ds plugin applied)
      (migratus/migrate (migratus-config ds plugin))
      (run-seed ds plugin))))


(defn run-test-reset []
  (doseq [plugin plugins]
    (run-reset test-ds plugin)))


(defn migrate-all [config]
  (doseq [plugin (:plugins config)]
    (migratus/migrate (migratus-config (:datasource config) plugin))))

;;;


(defn migrate [& args]
  (doseq [plugin (or args plugins)]
    (log/info "running migrations for plugin" (name plugin))
    (migratus/migrate (migratus-config dev-ds plugin)))
  (System/exit 0))

(defn rollback [plugin]
  (when-let [id (last (applied-migrations dev-ds plugin))]
    (log/info "rolling back latest migration for plugin" (name plugin))
    (run-down dev-ds plugin [id]))
  (System/exit 0))

(defn remove-plugin [plugin]
  (log/info "rolling back all migrations for plugin" (name plugin))
  (let [applied (reverse (applied-migrations dev-ds plugin))]
    (run-down dev-ds plugin applied))
  (System/exit 0))

(defn seed [& args]
  (doseq [plugin (or args plugins)]
    (run-seed dev-ds plugin))
  (System/exit 0))

(defn reset [& args]
  (doseq [plugin (or args plugins)]
    (run-reset dev-ds plugin))
  (System/exit 0))
