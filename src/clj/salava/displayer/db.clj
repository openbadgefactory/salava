(ns salava.displayer.db
  (:require [yesql.core :refer [defqueries]]
            [slingshot.slingshot :refer :all]
            [clj-time.format :as f]
            [clj-time.coerce :as c]
            [clojure.java.jdbc :as jdbc]
            [clojure.set :refer [rename-keys]]
            [clojure.walk :refer [postwalk]]
            [clojure.data.json :as json]
            [salava.core.util :refer [get-db]]))

(defqueries "sql/displayer/queries.sql")

(defn convert-email [ctx email]
  (select-verified-email-address {:email email} (into {:row-fn :user_id :result-set-fn first} (get-db ctx))))

(defn displayer-groups [ctx user-id]
  (let [public-badge-count (select-public-badge-count {:user_id user-id} (into {:row-fn :count :result-set-fn first} (get-db ctx)))
        public-groups (select-public-badge-groups {:user_id user-id} (get-db ctx))]
    (if (and public-badge-count (> public-badge-count 0))
      (->> public-groups
           (map #(rename-keys % {:id :groupId}))
           (cons {:groupId 0 :name "Public badges" :badges public-badge-count})))))

#_(defn fetch-badges [ctx user-id tag]
  (let [[where params] (if tag
                         ["JOIN badge_tag AS bt ON b.id = bt.badge_id WHERE b.user_id = ? AND bt.tag = ? AND b.visibility = 'public' AND b.deleted = 0" [user-id tag]]
                         ["WHERE b.user_id = ? AND b.visibility = 'public' AND b.deleted = 0" [user-id]])
        query (str "SELECT b.badge_url, b.criteria_url, b.issuer_url AS issuer_json_url, b.assertion_url, b.assertion_json, b.evidence_url, b.issued_on, b.expires_on, b.last_checked, b.ctime,
                           bc.name, bc.description, bc.image_file,
                           ic.name AS issuer_name, ic.url AS issuer_url, ic.email AS issuer_email, ic.description AS issuer_description, ic.image_file AS issuer_image FROM badge AS b
                    LEFT JOIN badge_content AS bc ON b.badge_content_id = bc.id
                    LEFT JOIN issuer_content AS ic ON b.issuer_content_id = ic.id "
                    where)]
    (jdbc/with-db-connection
      [conn (:connection (get-db ctx))]
      (jdbc/query conn (into [query] params)))))

(defn fetch-badges [ctx user-id tag]
  (if tag
   (select-user-badges-with-tag {:user_id user-id :tag tag} (get-db ctx))
   (select-all-user-badges {:user_id user-id} (get-db ctx))))

(defn remove-nil-values [map]
  (postwalk
    (fn [x]
      (if (map? x)
        (let [m (into {} (remove (comp nil? second) x))]
          (when (seq m)
            m))
        x))
    map))

(defn parse-badge [ctx badge]
  (try+
    (let [site-url (get-in ctx [:config :core :site-url])
          {:keys [name description image_file criteria_url assertion_json assertion_url last_checked ctime issued_on expires_on evidence_url issuer_name issuer_description issuer_email issuer_url issuer_image]} badge
          assertion (if-not (empty? assertion_json) (json/read-str assertion_json :key-fn keyword))
          badge_url (:badge assertion)
          last-validated (or last_checked ctime)
          recipient (if (string? (:recipient assertion)) (:recipient assertion) (get-in assertion [:recipient :identity]))
          salt (if (:salt assertion) (:salt assertion) (get-in assertion [:recipient :salt]))
          original-recipient (if (map? (:recipient assertion)) (:recipient assertion))
          displayer-badge {:lastValidated (if (and last-validated (= BigInteger (type last-validated))) (f/unparse (f/formatters :date-time) (c/from-long (* 1000 (.longValue last-validated)))))
                           :hostedUrl     (str assertion_url)
                           :imageUrl      (str site-url "/" image_file)
                           :assertion     {:badge              {:_location   (re-matches #"^http.*" (str badge_url))
                                                                :name        (str name)
                                                                :description (str description)
                                                                :image       (str site-url "/" image_file)
                                                                :criteria    (str criteria_url)
                                                                :issuer      {:name        (str issuer_name)
                                                                              :description (str issuer_description)
                                                                              :email       (str issuer_email)
                                                                              :url         (str issuer_url)
                                                                              :image       (if issuer_image (str site-url "/" issuer_image))
                                                                              :origin      (str issuer_url)}}
                                           :uid                (:uid assertion)
                                           :issued_on          issued_on
                                           :issuedOn           issued_on
                                           :expires            expires_on
                                           :evidence           evidence_url
                                           :verify             (:verify assertion)
                                           :recipient          recipient
                                           :salt               salt
                                           :_originalRecipient original-recipient}}]
      (remove-nil-values displayer-badge))
    (catch Object _)))

(defn displayer-badges [ctx user-id group-id]
  (let [tag-id (Integer. (or (re-find #"\d+" (str group-id)) 0))
        tag (if-not (= tag-id 0) (select-badge-tag {:id tag-id} (into {:result-set-fn first :row-fn :tag} (get-db ctx))))]
    (if (and (empty? tag) (not= tag-id 0))
      [nil tag-id]
      (let [badges (fetch-badges ctx user-id tag)
            parsed-badges (->> badges (map #(parse-badge ctx %)) (filter #(not (nil? %))))]
        [parsed-badges tag-id]))))
