(ns salava.core.migrator
  (:require [migratus.core :as migratus]
            [migratus.database :refer [find-migrations parse-migration-id]]
            [clojure.tools.logging :as log]
            [clojure.java.io :as io]
            [clojure.java.jdbc :as jdbc]
            [salava.registry]
            [salava.core.util :as util]))


(def config (-> (io/resource "config/core.edn") slurp read-string))

(def plugins (cons :core (:plugins salava.registry/enabled)))

(def ds (:datasource config))

(def jdbc-uri (str "jdbc:" (:adapter ds "mysql") "://"
                   (:server-name ds "localhost")  "/" (:database-name ds "salava")
                   "?user=" (:username ds "salava") (if (not-empty (:password ds)) (str "&password=" (:password ds)))))


(def schema-table "schema_migrations")

;;;

(defn migration-dir [plugin]
  (str "migrations/" (name plugin) "/sql"))

(defn seed-dir [plugin]
  (str "migrations/" (name plugin) "/seed"))


(defn seed-insert [data-file]
  (doseq [seed (-> data-file slurp read-string)]
    (jdbc/insert! jdbc-uri (:table seed) (:data seed))))


(defn copy-file [source-path dest-path]
  (do
    (io/make-parents dest-path)
    (io/copy (io/file source-path) (io/file dest-path))))


(defn seed-copy [plugin]
  (when-let [data-dir (io/resource (str (seed-dir plugin) "/public"))]
    (doseq [source-file (-> data-dir io/as-file file-seq rest)]
      (copy-file source-file (str "resources/public/" (util/public-path source-file))))))



(defn migratus-config [plugin]
  {:store :database
   :db    jdbc-uri
   :migration-dir (migration-dir plugin)
   :migration-table-name schema-table})


(defn applied-migrations
  ([]
   (try
     (map :id (jdbc/query jdbc-uri [(str "SELECT id from " schema-table " ORDER BY id ASC")]))
     (catch Exception e)))

  ([plugin]
   (let [applied (applied-migrations)
        available (->> (migration-dir plugin) find-migrations keys (map str) set)]
    (filter available (map str applied)))))


(defn run-down [plugin applied]
  (doseq [id applied]
    (migratus/down (migratus-config plugin) (parse-migration-id id))))


(defn run-seed [plugin]
  (when-let [data-file (io/resource (str (seed-dir plugin) "/data.edn"))]
    (log/info "running seed functions for plugin" (name plugin))
    (seed-insert data-file)
    (seed-copy plugin)))


(defn run-reset [plugin]
  (let [applied (reverse (applied-migrations plugin))]
    (do
      (log/info "running reset functions for plugin" (name plugin))
      (run-down plugin applied)
      (migratus/migrate (migratus-config plugin))
      (run-seed plugin))))

;;;


(defn migrate [& args]
  (doseq [plugin (or args plugins)]
    (log/info "running migrations for plugin" (name plugin))
    (migratus/migrate (migratus-config plugin)))
  (System/exit 0))

(defn rollback [plugin]
  (when-let [id (last (applied-migrations plugin))]
    (log/info "rolling back latest migration for plugin" (name plugin))
    (run-down plugin [id]))
  (System/exit 0))

(defn remove-plugin [plugin]
  (log/info "rolling back all migrations for plugin" (name plugin))
  (let [applied (reverse (applied-migrations plugin))]
    (run-down plugin applied))
  (System/exit 0))

(defn seed [& args]
  (doseq [plugin (or args plugins)]
    (run-seed plugin))
  (System/exit 0))

(defn reset [& args]
  (doseq [plugin (or args plugins)]
    (run-reset plugin))
  (System/exit 0))
