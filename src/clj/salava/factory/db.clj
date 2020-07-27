(ns salava.factory.db
  (:require [clojure.tools.logging :as log]
            [clojure.string :as string]
            [clojure.pprint :refer [pprint]]
            [clojure.data.json :as json]
            [yesql.core :refer [defqueries]]
            [clojure.java.jdbc :as jdbc]
            [salava.core.util :as u]
            [salava.core.http :as http]
            [salava.badge.parse :as p]
            [salava.badge.main :as b]
            [salava.badge.db :as db]
            [salava.user.db :as user]
            [clj-time.local :as l]
            [clj-time.coerce :as c]))

(defqueries "sql/factory/queries.sql")

(defn get-uids-by-emails [ctx emails]
  (let [email-chunks (partition-all 100 emails)
        result (map #(select-uids-emails-by-emails {:emails %} (u/get-db ctx)) email-chunks)]
    (reduce (fn [coll v] (assoc coll (string/lower-case (:email v)) (:user_id v))) {} (flatten result))))

(defn primary-emails-by-uids [ctx uids]
  (let [uid-chunks (partition-all 100 uids)
        result (map #(select-primary-emails-by-uids {:user_ids %} (u/get-db ctx)) uid-chunks)]
    (reduce (fn [coll v] (assoc coll (:user_id v) (string/lower-case (:email v)))) {} (flatten result))))

(defn get-user-emails
  ""
  [ctx emails]
  (let [emails-uids (get-uids-by-emails ctx emails)
        primary-uids-emails (primary-emails-by-uids ctx (vals emails-uids))]
    (reduce (fn [coll v] (assoc coll v (->> v (get emails-uids) (get primary-uids-emails)))) {} emails)))


(defn save-assertions-for-emails
  ""
  [ctx emails-assertions]
  (log/info "save-assertions-for-emails: got" (count emails-assertions) "recipients")
  (try
    (jdbc/with-db-transaction [tx {:datasource (:db ctx)}]
      (doseq [email (keys emails-assertions)]
        (doseq [assertion (get emails-assertions email)]
          (insert-pending-badge-for-email! {:assertion_url assertion :email (name email)} {:connection tx}))))

    (u/publish ctx :new-factory-badge emails-assertions)

    true
    (catch Throwable ex
      (log/error "save-assertions-for-emails: transaction failed")
      (log/error (.toString ex))
      false)))

(defn save-pending-assertion
  ""
  [ctx tx user-id pending-assertion]
  (log/info "try to save pending assertion: " pending-assertion)
  (try
    (let [user-emails (user/verified-email-addresses ctx user-id)
          user-badge (p/str->badge {:id user-id :emails user-emails} (:assertion_url pending-assertion))
          id (db/save-user-badge! ctx user-badge tx)]
      (delete-duplicate-pending-badges! (assoc pending-assertion :user_id user-id) {:connection tx})
      (u/publish ctx :new-pending-badge (assoc pending-assertion :id id))
      id)
    (catch Exception ex
      (log/error "save-pending-assertions: failed to save badge")
      (log/error ex)
      nil)))

(defn save-pending-assertion-first
  "Save one of user's pending assertions"
  [ctx user-id]
  (jdbc/with-db-transaction [tx {:datasource (:db ctx)}]
    (loop [pending-assertions (select-pending-badges-by-user {:user_id user-id} {:connection tx})]
      (when-let [pending-assertion (first pending-assertions)]
        (if-let [id (save-pending-assertion ctx tx user-id pending-assertion)]
          [id pending-assertion]
          (recur (rest pending-assertions)))))))


(defn save-pending-assertions
  "Save all pending assertions"
  [ctx user-id]
  (jdbc/with-db-transaction [tx {:datasource (:db ctx)}]
    (doseq [pending-assertion (select-pending-badges-by-user {:user_id user-id} {:connection tx})]
      (save-pending-assertion ctx tx user-id pending-assertion))))


#_(defn get-badge-updates
    ""
    [ctx user-id badge-id]
    (if-let [badge-updates (select-badge-updates {:user_id user-id :id badge-id} (u/get-db-1 ctx))]
      {"user" {user-id {"badge" {badge-id badge-updates}}}}))

(defn endorsement->endorsement-class
  "Conforms endorsment to specification: https://www.imsglobal.org/sites/default/files/Badges/OBv2p0Final/index.html#Endorsement
  Endorser profile link is used as issuer id"
  [ctx coll]
  (mapv (fn [endorsement]
          (-> endorsement
              (dissoc :content :issuer_id :issuer_name :issuer_url :mtime :assertion_url)
              (assoc :issuer {:id (:issuer_url endorsement)
                              :name (:issuer_name endorsement)
                              :type "Issuer"}
                     :claim {:id (:assertion_url endorsement)
                             :endorsementComment (:content endorsement)}
                     :issuedOn (:mtime endorsement) #_(str (l/to-local-date-time (long (* (:mtime endorsement) 1000))))))) coll))

(defn get-badge-updates
  ""
  [ctx user-id badge-id]
  (let [badge-updates (select-badge-updates {:user_id user-id :id badge-id} (u/get-db-1 ctx))
        evidence (select-user-badge-evidence {:id badge-id} (u/get-db ctx))
        endorsements (some->> (select-user-badge-endorsements {:id badge-id} (u/get-db ctx)) (concat (select-user-badge-endorsements-ext {:id badge-id} (u/get-db ctx))) (endorsement->endorsement-class ctx))]
    {"user" {user-id {"badge" {badge-id (assoc badge-updates :evidence evidence :endorsement endorsements)}}}}))

(defn- issued-by-factory [ctx badge]
  (boolean
   (try
     (let [url-match (partial re-find (re-pattern (str "^" (get-in ctx [:config :factory :url] "-"))))]
       (or (url-match (get badge :assertion_url ""))
           (some-> (:assertion_url badge)
                   http/json-get
                   (get-in [:related 0 :id])
                   http/json-get
                   (get :id)
                   url-match)))
     (catch Exception _))))

(defn- verified-by-factory [ctx badge]
  (boolean
   (try
     (some-> (:assertion_url badge)
             http/json-get
             (get-in [:badge :issuer])
             (string/replace #"&event=.+" "")
             http/json-get
             :verified)
     (catch Exception _))))

(defn issuer-info [ctx badge]
  (let [issued (issued-by-factory ctx badge)]
    {:obf_url (get-in ctx [:config :factory :url] "")
     :issued_by_factory issued
     :verified_by_factory (and issued (verified-by-factory ctx badge))}))

(defn receive-banner [banner]
  (cond
    (string/blank? banner) ""
    (> (count banner) 100) ""
    (re-find #"[^0-9a-z\.]" banner) ""
    :else banner))

(defn receive-badge-json [ctx e k t]
  (let [receive-url (str (get-in ctx [:config :factory :url]) "/c/receive/check.json"
                         "?t=" t "&k=" k "&e=" (u/url-encode e))]
    (http/json-get receive-url)))

(defn receive-badge [ctx {:keys [email assertion_url user_id]}]
  (let [metabadge-fn (first (u/plugin-fun (u/get-plugins ctx) "metabadge" "pending-metabadge?"))]
    (try
      (if (and email assertion_url)
        (if-let [id (select-badge-by-assertion {:email email :url assertion_url} (u/get-db-1 ctx))]
          (:id id)
          (db/save-user-badge! ctx (p/str->badge {:id (or user_id 0) :emails [email]} assertion_url)))
        (log/error "receive-badge: failed to fetch pending badge"))
      (catch Exception ex
        (log/error "receive-badge: failed to fetch pending badge")
        (log/error (.toString ex))))))

(defn reject-badge! [ctx user-badge-id user-id]
  (log/info "Rejected badge id: " user-badge-id)
  (let [assertion-info (select-pending-assertion-by-badge-id {:id user-badge-id} (u/get-db-1 ctx))
        {:keys [email assertion_url]} assertion-info]
    (delete-pending-user-badge! {:id user-badge-id :user_id (or user-id 0)} (u/get-db ctx))
    (delete-pending-factory-assertion! {:e email :url assertion_url} (u/get-db ctx))
    (log/info (str "Deleted user-badge id:" user-badge-id ", and pending-factory-badge " assertion-info))
    {:success true}))

(defn get-pdf-cert-list [ctx current-user user-badge-id]
  (let [obf-url (get-in ctx [:config :core :obf :url])
        badge (select-badge-by-id {:id user-badge-id} (u/get-db-1 ctx))]
    (if (and (string? obf-url) (issued-by-factory ctx badge))
      (let [a (:assertion_url badge)
            s (get-in ctx [:config :core :site-url])
            t (u/hmac-sha256-hex (str a (:email badge)) (get-in ctx [:config :factory :secret]))]
        (http/json-get (str obf-url "/c/badge/pdf_cert?a=" (u/url-encode a) "&s=" (u/url-encode s) "&t=" t)))
      {:cert []})))

(defn new-pdf-cert-request [ctx current-user user-badge-id message]
  (let [obf-url (get-in ctx [:config :core :obf :url])
        badge (select-badge-by-id {:id user-badge-id} (u/get-db-1 ctx))]
    (if (and (string? obf-url) (issued-by-factory ctx badge) (= (:visibility badge) "public"))
      (let [a (:assertion_url badge)
            s (get-in ctx [:config :core :site-url])
            t (u/hmac-sha256-hex (str a (:email badge)) (get-in ctx [:config :factory :secret]))
            uri (str (b/badge-url ctx user-badge-id) "/full/embed")]

        (http/http-post (str obf-url "/c/badge/pdf_cert_request") {:form-params {:message message :badge_uri uri :passport_url s :assertion_url a :token t}})
        {:request-sent true})
      {:request-sent false})))
