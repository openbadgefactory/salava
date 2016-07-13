(ns salava.admin.db
 (:require [yesql.core :refer [defqueries]]
            [clojure.set :refer [rename-keys]]
            [clojure.java.jdbc :as jdbc]
            [salava.core.helper :refer [dump]]
            [salava.core.util :refer [get-db]]
            [salava.core.time :refer [unix-time get-date-from-today]]))

(defqueries "sql/admin/queries.sql")

(defn register-users-count
  "Get count from all active users"
  [ctx]
  (total-user-count {} (into {:result-set-fn first :row-fn :count} (get-db ctx))))

(defn last-month-users-login-count
  "Check if provided email address exsits"
  [ctx]
  (let [date (get-date-from-today -1 0 0)]
    (count-logged-users-after-date {:time date} (into {:result-set-fn first :row-fn :count} (get-db ctx)))))

(defn last-month-users-registered-count
  "Check if provided email address exsits"
  [ctx]
  (let [date (get-date-from-today -1 0 0)]
    (count-registered-users-after-date {:time date} (into {:result-set-fn first :row-fn :count} (get-db ctx)))))


(defn badges-count
  "Check if provided email address exsits"
  [ctx]
  (count-all-badges {} (into {:result-set-fn first :row-fn :count} (get-db ctx))))

(defn last-month-added-badges-count
  "Check if provided email address exsits"
  [ctx]
  (let [date (get-date-from-today -1 0 0)]
    (count-all-badges-after-date {:time date} (into {:result-set-fn first :row-fn :count} (get-db ctx)))))

(defn pages-count
  "Check if provided email address exsits"
  [ctx]
  (count-all-pages {} (into {:result-set-fn first :row-fn :count} (get-db ctx))))


(defn get-stats [ctx]
  {:register-users (register-users-count ctx)
   :last-month-active-users (last-month-users-login-count ctx)
   :last-month-registered-users (last-month-users-registered-count ctx)
   :all-badges (badges-count ctx)
   :last-month-added-badges (last-month-added-badges-count ctx)
   :pages (pages-count ctx)})

