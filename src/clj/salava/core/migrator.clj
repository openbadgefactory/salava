(ns salava.core.migrator
  (:require [joplin.repl :as joplin]
            [joplin.jdbc.database]
            [clojure.java.io :as io]
            [clojure.java.jdbc :as jdbc]
            [salava.core.util :as util]))


(def config (-> (io/resource "config/core.edn") slurp read-string))

(def plugins (conj (:plugins config) :core))

(defn to-jdbc-uri [ds]
  (str "jdbc:" (:adapter ds "mysql") "://"
       (:server-name ds "localhost")  "/" (:database-name ds "salava")
       "?user=" (:username ds "salava") "&password=" (:password ds "salava")))


(defn seed-data [plugin]
  (-> (io/resource (str "migrators/" (name plugin) "/seed/data.edn")) slurp read-string))


(defn seed-insert [plugin db-uri]
  (doseq [seed (seed-data plugin)]
    (jdbc/insert! {:connection-uri db-uri} (:table seed) (:data seed))))


(defn copy-file [source-path dest-path]
  (do
    (io/make-parents dest-path)
    (io/copy (io/file source-path) (io/file dest-path))))

(defn seed-copy [plugin]
  (doseq [source-file (-> (io/resource (str "migrators/" plugin "/seed/public")) io/as-file file-seq rest)]
     (copy-file source-file (str "resources/public/" (util/public-path source-file)))))


(defn seed-run [plugin target & args]
  (do
    (seed-insert plugin (get-in target [:db :url]))
    (seed-copy plugin)))


;;;

(defn joplin-config [plugin]
  {:migrators    {:sql-mig
                  (str "resources/migrators/" (name plugin) "/sql")}
   :seeds        {:sql-seed
                  (partial seed-run plugin)}
   :databases    {:sql
                  {:type :jdbc, :url (to-jdbc-uri (:datasource config))}}
   :environments {:default
                  [{:db :sql, :migrator :sql-mig, :seed :sql-seed}]}})



(defn migrate [& args]
  (doseq [plugin (or args plugins)]
    (joplin/migrate (joplin-config plugin) :default :sql))
  (System/exit 0))

(defn rollback [& args]
  (doseq [plugin (or args plugins)]
    (joplin/rollback (joplin-config plugin) :default :sql))
  (System/exit 0))

(defn seed [& args]
  (doseq [plugin (or args plugins)]
    (joplin/seed (joplin-config plugin) :default :sql))
  (System/exit 0))

(defn reset [& args]
  (doseq [plugin (or args plugins)]
    (joplin/reset (joplin-config plugin) :default :sql))
  (System/exit 0))
