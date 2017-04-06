(ns salava.extra.factory.db
  (:require [clojure.tools.logging :as log]
            [yesql.core :refer [defqueries]]
            [clojure.tools.logging :as log]
            [clojure.java.jdbc :as jdbc]
            [slingshot.slingshot :refer :all]
            [salava.core.util :refer [get-db]]
            [salava.badge.assertion :as a]
            [salava.badge.main :as b]
            [salava.user.db :as u]))

(defqueries "sql/extra/factory/queries.sql")

(defn get-uids-by-emails [ctx emails]
  (let [email-chunks (partition-all 100 emails)
        result (map #(select-uids-emails-by-emails {:emails %} (get-db ctx)) email-chunks)]
    (reduce #(assoc %1 (:email %2) (:user_id %2)) {} (flatten result))))

(defn backpack-emails-by-uids [ctx uids]
  (let [uid-chunks (partition-all 100 uids)
        result (map #(reverse (select-backback-emails-by-uids {:user_ids %} (get-db ctx))) uid-chunks)]
    (reduce #(assoc %1 (:user_id %2) (:email %2)) {} (flatten result))))

(defn get-user-emails
  ""
  [ctx emails]
  (let [emails-uids (get-uids-by-emails ctx emails)
        backpack-uids-emails (backpack-emails-by-uids ctx (vals emails-uids))]
    (reduce #(assoc %1 %2 (->> %2 (get emails-uids) (get backpack-uids-emails))) {} emails)))

(defn save-assertions-for-emails
  ""
  [ctx emails-assertions]
  (log/info "save-assertions-for-emails: got" (count emails-assertions) "recipients")
  (try
    (jdbc/with-db-transaction [tx {:datasource (:db ctx)}]
      (doseq [email (keys emails-assertions)]
        (let [assertions (get emails-assertions email)]
          (doseq [assertion assertions]
            (insert-pending-badge-for-email! {:assertion_url assertion :email (name email)} {:connection tx})))))
    true
    (catch Throwable ex
      (log/error "save-assertions-for-emails: transaction failed")
      (log/error (.toString ex))
      false)))

(defn save-factory-badge [ctx assertion-url user-id emails]
  (let [assertion (a/create-assertion assertion-url {})]
    ;(b/save-badge-from-assertion! ctx {:assertion assertion} user-id emails)
    ))

(defn save-pending-assertions
  ""
  [ctx user-id]
  (let [pending-assertions (select-pending-badges-by-user {:user_id user-id} (get-db ctx))
        emails (u/verified-email-addresses ctx user-id)]
    (doseq [pending-assertion pending-assertions]
      (log/info "try to save pending assertion: " pending-assertion)
      (try+
        (save-factory-badge ctx (:assertion_url pending-assertion) user-id emails)
        (delete-duplicate-pending-badges! pending-assertion (get-db ctx))
        (catch Object _
          (log/error "save-pending-assertions: " _)
          nil)))))

(defn get-badge-updates
  ""
  [ctx user-id badge-id]
  (let [{:keys [id user_id] :as badge-updates} (select-badge-updates {:user_id user-id :id badge-id} (into {:result-set-fn first} (get-db ctx)))]
    (if (and id user_id)
      {"user" {user_id {"badge" {id badge-updates}}}})))
