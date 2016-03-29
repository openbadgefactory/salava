(ns salava.core.migrator
  (:require [migratus.core :as migratus]
            [migratus.database :refer [find-migrations parse-migration-id]]
            [clojure.tools.logging :as log]
            [clojure.java.io :as io]
            [clojure.java.jdbc :as jdbc]
            [salava.core.util :refer [public-path]]))

(defn read-config [path]
  (-> (clojure.java.io/file (str path "/core.edn")) slurp read-string))

(defn dev-config  [] (read-config "resources/config"))
(defn test-config [] (read-config "resources/test_config"))


(defn plugins [] (cons :core (:plugins (dev-config))))


(defn jdbc-uri [conf]
  (let [source (:datasource conf)]
    (str "jdbc:" (:adapter source "mysql") "://"
         (:server-name source "localhost")  "/" (:database-name source)
         "?user=" (:username source)
         (if (not-empty (:password source)) (str "&password=" (:password source))))))


(def schema-table "schema_migrations")

;;;

(defn migration-dir [plugin]
  (str "migrations/" (name plugin) "/sql"))

(defn seed-dir [plugin]
  (str "migrations/" (name plugin) "/seed"))


(defn seed-insert [conf data-file]
  (doseq [seed (-> data-file slurp read-string)]
    (jdbc/insert! (jdbc-uri conf) (:table seed) (:data seed))))


(defn copy-file [source-path dest-path]
  (do
    (io/make-parents dest-path)
    (io/copy (io/file source-path) (io/file dest-path))))


(defn seed-copy [conf plugin]
  (when-let [data-dir (io/resource (str (seed-dir plugin) "/public"))]
    (doseq [source-file (-> data-dir io/as-file file-seq rest)]
      (copy-file source-file (str (:data-dir conf "resources/public") "/" (public-path source-file))))))


(defn migratus-config [conf plugin]
  {:store                :database
   :db                   (jdbc-uri conf)
   :migration-dir        (migration-dir plugin)
   :migration-table-name schema-table})


(defn applied-migrations
  ([conf]
   (try
     (map :id (jdbc/query (jdbc-uri conf) [(str "SELECT id from " schema-table " ORDER BY id ASC")]))
     (catch Exception e)))

  ([conf plugin]
   (let [applied (applied-migrations conf)
        available (->> (migration-dir plugin) find-migrations keys (map str) set)]
    (filter available (map str applied)))))


(defn run-down [conf plugin applied]
  (doseq [id applied]
    (migratus/down (migratus-config conf plugin) (parse-migration-id id))))


(defn run-seed [conf plugin]
  (when-let [data-file (io/resource (str (seed-dir plugin) "/data.edn"))]
    (log/info "running seed functions for plugin" (name plugin))
    (seed-insert conf data-file)
    (seed-copy conf plugin)))


(defn run-reset [conf plugin]
  (let [applied (reverse (applied-migrations conf plugin))]
    (do
      (log/info "running reset functions for plugin" (name plugin))
      (run-down conf plugin applied)
      (migratus/migrate (migratus-config conf plugin))
      (run-seed conf plugin))))


(defn run-test-reset []
  (doseq [plugin (plugins)]
    (run-reset (test-config) plugin)))


(defn migrate-all [config-path]
  (let [config (read-config config-path)]
    (doseq [plugin (:plugins config)]
      (migratus/migrate (migratus-config config plugin)))))

;;;


(defn migrate [& args]
  (doseq [plugin (or args (plugins))]
    (log/info "running migrations for plugin" (name plugin))
    (migratus/migrate (migratus-config (dev-config) plugin)))
  (System/exit 0))

(defn rollback [plugin]
  (when-let [id (last (applied-migrations (dev-config) plugin))]
    (log/info "rolling back latest migration for plugin" (name plugin))
    (run-down (dev-config) plugin [id]))
  (System/exit 0))

(defn remove-plugin [plugin]
  (log/info "rolling back all migrations for plugin" (name plugin))
  (let [applied (reverse (applied-migrations (dev-config) plugin))]
    (run-down (dev-config) plugin applied))
  (System/exit 0))

(defn seed [& args]
  (doseq [plugin (or args (plugins))]
    (run-seed (dev-config) plugin))
  (System/exit 0))

(defn reset [& args]
  (doseq [plugin (or args (plugins))]
    (run-reset (dev-config) plugin))
  (System/exit 0))
