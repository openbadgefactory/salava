(ns salava.admin.helper
 (:require [yesql.core :refer [defqueries]]
            [clojure.set :refer [rename-keys]]
            [clojure.java.jdbc :as jdbc]
            [salava.core.helper :refer [dump]]
            [slingshot.slingshot :refer :all]
            [salava.core.util :refer [get-db]]
            [clojure.data.csv :as csv]
            [clojure.java.io :as io]))

(defqueries "sql/admin/queries.sql")

(defn user-admin?
  "Check if user is admin"
  [ctx user-id]
  (let [admin (select-user-admin {:id user-id} (into {:result-set-fn first :row-fn :role} (get-db ctx)))]
    (= admin "admin")))

(defn make-csv
 ([ctx data]
  (fn [out]
   (with-open [writer (io/writer out)]
    (csv/write-csv writer data :separator \:))))
 ([ctx data seperator]
   (fn [out]
    (with-open [writer (io/writer out)]
     (csv/write-csv writer data :separator seperator)))))
