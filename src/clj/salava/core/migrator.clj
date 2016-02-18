(ns salava.core.migrator
  (:require [migratus.core :as migratus]
            [migratus.database :refer [find-migrations parse-migration-id]]
            [clojure.tools.logging :as log]
            [clojure.java.io :as io]
            [clojure.java.jdbc :as jdbc]
            [salava.core.util :refer [public-path config]]))

(def plugins (cons :core (:plugins config)))

(def datasource (:datasource config))

(def test-datasource (:test-datasource config))

(defn jdbc-uri [test-mode?]
  (let [source (if-not (or (= test-mode? "false") (= test-mode? false))
                 test-datasource
                 datasource)]
    (str "jdbc:" (:adapter source "mysql") "://"
         (:server-name source "localhost")  "/" (:database-name source)
         "?user=" (:username source) (if (not-empty (:password source)) (str "&password=" (:password source))))))


(def schema-table "schema_migrations")

;;;

(defn migration-dir [plugin]
  (str "migrations/" (name plugin) "/sql"))

(defn seed-dir [plugin]
  (str "migrations/" (name plugin) "/seed"))


(defn seed-insert [test-mode? data-file]
  (doseq [seed (-> data-file slurp read-string)]
    (jdbc/insert! (jdbc-uri test-mode?) (:table seed) (:data seed))))


(defn copy-file [source-path dest-path]
  (do
    (io/make-parents dest-path)
    (io/copy (io/file source-path) (io/file dest-path))))


(defn seed-copy [plugin]
  (when-let [data-dir (io/resource (str (seed-dir plugin) "/public"))]
    (doseq [source-file (-> data-dir io/as-file file-seq rest)]
      (copy-file source-file (str "resources/public/" (public-path source-file))))))


(defn migratus-config [test-mode? plugin]
  {:store                :database
   :db                   (jdbc-uri test-mode?)
   :migration-dir        (migration-dir plugin)
   :migration-table-name schema-table})


(defn applied-migrations
  ([test-mode?]
   (try
     (map :id (jdbc/query (jdbc-uri test-mode?) [(str "SELECT id from " schema-table " ORDER BY id ASC")]))
     (catch Exception e)))

  ([test-mode? plugin]
   (let [applied (applied-migrations test-mode?)
        available (->> (migration-dir plugin) find-migrations keys (map str) set)]
    (filter available (map str applied)))))


(defn run-down [test-mode? plugin applied]
  (doseq [id applied]
    (migratus/down (migratus-config test-mode? plugin) (parse-migration-id id))))


(defn run-seed [test-mode? plugin]
  (when-let [data-file (io/resource (str (seed-dir plugin) "/data.edn"))]
    (log/info "running seed functions for plugin" (name plugin))
    (seed-insert test-mode? data-file)
    (seed-copy plugin)))


(defn run-reset [test-mode? plugin]
  (let [applied (reverse (applied-migrations test-mode? plugin))]
    (do
      (log/info "running reset functions for plugin" (name plugin))
      (run-down test-mode? plugin applied)
      (migratus/migrate (migratus-config test-mode? plugin))
      (run-seed test-mode? plugin))))

;;;


(defn migrate [test-mode? & args]
  (doseq [plugin (or args plugins)]
    (log/info "running migrations for plugin" (name plugin))
    (migratus/migrate (migratus-config test-mode? plugin)))
  (System/exit 0))

(defn rollback [test-mode? plugin]
  (when-let [id (last (applied-migrations plugin))]
    (log/info "rolling back latest migration for plugin" (name plugin))
    (run-down test-mode? plugin [id]))
  (System/exit 0))

(defn remove-plugin [test-mode? plugin]
  (log/info "rolling back all migrations for plugin" (name plugin))
  (let [applied (reverse (applied-migrations plugin))]
    (run-down test-mode? plugin applied))
  (System/exit 0))

(defn seed [test-mode? & args]
  (doseq [plugin (or args plugins)]
    (run-seed test-mode? plugin))
  (System/exit 0))

(defn reset [test-mode? & args]
  (doseq [plugin (or args plugins)]
    (run-reset test-mode? plugin))
  (System/exit 0))
