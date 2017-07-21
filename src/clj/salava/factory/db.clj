(ns salava.factory.db
  (:require [clojure.tools.logging :as log]
            [clojure.string :as string]
            [yesql.core :refer [defqueries]]
            [clojure.java.jdbc :as jdbc]
            [salava.core.util :as u]
            [salava.badge.parse :as p]
            [salava.badge.main :as b]
            [salava.badge.db :as db]
            [salava.user.db :as user]))

(defqueries "sql/factory/queries.sql")

(defn get-uids-by-emails [ctx emails]
  (let [email-chunks (partition-all 100 emails)
        result (map #(select-uids-emails-by-emails {:emails %} (u/get-db ctx)) email-chunks)]
    (reduce #(assoc %1 (:email %2) (:user_id %2)) {} (flatten result))))

(defn backpack-emails-by-uids [ctx uids]
  (let [uid-chunks (partition-all 100 uids)
        result (map #(reverse (select-backback-emails-by-uids {:user_ids %} (u/get-db ctx))) uid-chunks)]
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
        (doseq [assertion (get emails-assertions email)]
          (insert-pending-badge-for-email! {:assertion_url assertion :email (name email)} {:connection tx}))))
    true
    (catch Throwable ex
      (log/error "save-assertions-for-emails: transaction failed")
      (log/error (.toString ex))
      false)))

(defn save-pending-assertions
  ""
  [ctx user-id]
  (doseq [pending-assertion (select-pending-badges-by-user {:user_id user-id} (u/get-db ctx))]
    (log/info "try to save pending assertion: " pending-assertion)
    (try
      (and
        (db/save-user-badge! ctx
                             (-> {:id user-id :emails (user/verified-email-addresses ctx user-id)}
                                 (p/str->badge (:assertion_url pending-assertion))))
        (delete-duplicate-pending-badges! pending-assertion (u/get-db ctx)))
      #_(catch Exception ex
        (log/error "save-pending-assertions: failed to save badge")
        (log/error (.toString ex))))))

(defn get-badge-updates
  ""
  [ctx user-id badge-id]
  (if-let [badge-updates (select-badge-updates {:user_id user-id :id badge-id} (u/get-db-1 ctx))]
    {"user" {user-id {"badge" {badge-id badge-updates}}}}))


(defn- issued-by-factory [ctx badge]
  (boolean
    (try
      (let [url-match (partial re-find (re-pattern (str "^" (get-in ctx [:config :factory :url] "-"))))]
        (or (url-match (get badge :assertion_url ""))
            (some-> (:assertion_url badge)
                    u/json-get
                    (get-in [:related :id])
                    u/json-get
                    (get-in [:verify :url])
                    url-match)))
      (catch Exception _))))

(defn- verified-by-factory [ctx badge]
  (boolean
    (try
      (some-> (:assertion_url badge)
              u/json-get
              (get-in [:badge :issuer])
              (string/replace #"&event=.+" "")
              u/json-get
              :verified)
      (catch Exception _))))

(defn issuer-info [ctx badge]
  (let [issued (issued-by-factory ctx badge)]
    {:obf_url (get-in ctx [:config :factory :url] "")
     :issued_by_factory issued
     :verified_by_factory (and issued (verified-by-factory ctx badge))}))

