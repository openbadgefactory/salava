(ns salava.admin.db
 (:require [yesql.core :refer [defqueries]]
            [clojure.set :refer [rename-keys]]
            [clojure.java.jdbc :as jdbc]
            [salava.core.helper :refer [dump]]
            [slingshot.slingshot :refer :all]
            [salava.core.util :refer [get-db]]
            [salava.core.time :refer [unix-time get-date-from-today]]))

(defqueries "sql/admin/queries.sql")

(defn user-admin?
  "Check if user is admin"
  [ctx user-id]
  (let [admin (select-user-admin {:id user-id} (into {:result-set-fn first :row-fn :role} (get-db ctx)))]
    (= admin "admin")))


(defn register-users-count
  "Get count from all active and registered users"
  [ctx]
  (total-user-count {} (into {:result-set-fn first :row-fn :count} (get-db ctx))))

(defn last-month-users-login-count
  "Get count from all last month logged in users"
  [ctx]
  (let [date (get-date-from-today -1 0 0)]
    (count-logged-users-after-date {:time date} (into {:result-set-fn first :row-fn :count} (get-db ctx)))))

(defn last-month-users-registered-count
  "Get count form all last month registered users"
  [ctx]
  (let [date (get-date-from-today -1 0 0)]
    (count-registered-users-after-date {:time date} (into {:result-set-fn first :row-fn :count} (get-db ctx)))))


(defn badges-count
  "Get count from all badges"
  [ctx]
  (count-all-badges {} (into {:result-set-fn first :row-fn :count} (get-db ctx))))

(defn last-month-added-badges-count
  "Get count from all last month added badges"
  [ctx]
  (let [date (get-date-from-today -1 0 0)]
    (count-all-badges-after-date {:time date} (into {:result-set-fn first :row-fn :count} (get-db ctx)))))

(defn pages-count
  "Get count from all pages have been created"
  [ctx]
  (count-all-pages {} (into {:result-set-fn first :row-fn :count} (get-db ctx))))


(defn get-stats [ctx]
  (try+
   {:register-users (register-users-count ctx)
   :last-month-active-users (last-month-users-login-count ctx)
   :last-month-registered-users (last-month-users-registered-count ctx)
   :all-badges (badges-count ctx)
   :last-month-added-badges (last-month-added-badges-count ctx)
   :pages (pages-count ctx)}
   (catch Object _
     "error")))

(defn private-badge! [ctx id]
  (try+
   (update-badge-visibility! {:id id} (get-db ctx))
   "success"
   (catch Object _
     "error")))


(defn private-badges! [ctx badge_content_id]
  (try+
   (update-badges-visibility!{:badge_content_id badge_content_id} (get-db ctx))
   "success"
   (catch Object _
     "error")))



(defn private-page! [ctx id]
  (try+
   (update-page-visibility! {:id id} (get-db ctx))
   "success"
   (catch Object _
     "error")))

(defn private-user! [ctx id]
  (try+
   (update-user-visibility! {:id id} (get-db ctx))
   "success"
   (catch Object _
     "error"
     )))
