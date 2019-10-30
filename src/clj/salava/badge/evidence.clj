(ns salava.badge.evidence
  (:require [clj-time.core :refer [today]]
            [clojure.data.json :as json]
            [clojure.java.jdbc :as jdbc]
            [clojure.tools.logging :as log]
            [pantomime.mime :refer [mime-type-of]]
            [salava.badge.main :refer [send-badge-info-to-obf badge-owner?]]
            [salava.core.util :as u]
            [slingshot.slingshot :refer [try+ throw+]]
            [yesql.core :refer [defqueries]]))

;(defqueries "sql/badge/main.sql")
(defqueries "sql/badge/evidence.sql")

(defn badge-evidence "get user-badge evidence"
  ([ctx badge-id]
   (select-user-badge-evidence {:user_badge_id badge-id} (u/get-db ctx)))
  ([ctx badge-id user-id]
   (let [evidences (badge-evidence ctx badge-id)]
    {:evidence
     (reduce (fn [r evidence]
               (let [property-name (str "evidence_id:" (:id evidence))
                     properties (some-> (select-user-evidence-property {:name property-name :user_id user-id} (into {:result-set-fn first :row-fn :value} (u/get-db ctx)))
                                        (json/read-str :key-fn keyword))]
                 (conj r (-> evidence (assoc :properties properties))))) [] evidences)}))
  ([ctx badge-id user-id markdown?] (map (fn [evidence] (-> evidence (update :narrative u/md->html))) (:evidence (badge-evidence ctx badge-id user-id)))))

(defn update-evidence! [ctx user-badge-id evidence-id evidence]
  (let [{:keys [name narrative url]} evidence
        site-url (u/get-site-url ctx)
        description (str "Added by badge recipient " (today)  " at " site-url)
        data {:user_badge_id user-badge-id :url url :name name :narrative narrative :description description}]
    (update-user-badge-evidence! (assoc data :id evidence-id) (u/get-db ctx))
    evidence-id))

(defn save-new-evidence! [ctx user-id user-badge-id evidence]
  (let [{:keys [name narrative url resource_id resource_type mime_type]} evidence
        site-url (u/get-site-url ctx)
        description (str "Added by badge recipient " (today)  " at " site-url)
        data {:user_badge_id user-badge-id :url url :name name :narrative narrative :description description}]
    (jdbc/with-db-transaction  [tx (:connection (u/get-db ctx))]
      (let [id (->> (insert-evidence<! data {:connection tx}) :generated_key)
            property-name (str "evidence_id:" id)
            properties (->> {:resource_id resource_id :resource_type resource_type :hidden false :mime_type mime_type}
                            (remove (comp nil? second))
                            (into {})
                            (json/write-str))]
        (insert-user-evidence-property! {:user_id user-id :value properties :name property-name} {:connection tx})
        id))))

(defn save-badge-evidence [ctx user-id user-badge-id evidence]
  (let [{:keys [id name narrative url resource_id resource_type mime_type]} evidence]
    (try+
      (if (badge-owner? ctx user-badge-id user-id)
        (do
          (if id
            (update-evidence! ctx user-badge-id id evidence)
            (save-new-evidence! ctx user-id user-badge-id evidence))

          (send-badge-info-to-obf ctx user-badge-id user-id)
          {:status "success"})

        (throw+ {:status "error"}))

      (catch Object _
        (log/error _)
        {:status "error"}))))

(defn delete-evidence! [ctx evidence-id user-badge-id user-id]
  (try+
    (if (badge-owner? ctx user-badge-id user-id)
      (do
        (jdbc/with-db-transaction  [tx (:connection (u/get-db ctx))]
          (let [property-name (str "evidence_id:" evidence-id)]
            (delete-user-badge-evidence! {:id evidence-id :user_badge_id user-badge-id} {:connection tx})
            (delete-user-evidence-property! {:user_id user-id :name property-name} {:connection tx})))

        ;;send badge info to factory
        (send-badge-info-to-obf ctx user-badge-id user-id)

        {:status "success"})
      {:status "error"})
    (catch Object _
      (log/error _)
      {:status "error"})))

(defn is-evidence? [ctx user-id opts]
  (let [{:keys [id resource-type]} opts
        url (case resource-type
              "page" (str (u/get-full-path ctx) "/page/view/" id)
              "badge" (str (u/get-full-path ctx) "/badge/info/" id)
              (str (u/get-full-path ctx) "/page/view/" id))]
    (if-let [check (empty? (select-user-evidence-by-url {:user_id user-id :url url} (u/get-db ctx)))] false true)))

#_(defn toggle-show-all-evidences!
    "Toggle evidence visibility"
    [ctx badge-id show-evidence user-id]
    (if (badge-owner? ctx badge-id user-id)
      (update-show-evidence! {:id badge-id :show_evidence show-evidence} (u/get-db ctx))))

(defn toggle-show-evidence! [ctx badge-id evidence-id show_evidence user-id]
  "Toggle evidence visibility"
  (try+
    (if (badge-owner? ctx badge-id user-id)
      (do
        (let [property-name (str "evidence_id:" evidence-id)
              metadata (some-> (select-user-evidence-property {:name property-name :user_id user-id} (into {:result-set-fn first :row-fn :value} (u/get-db ctx)))
                               (json/read-str :key-fn keyword))
              updated-metadata (-> (assoc metadata :hidden show_evidence)
                                   (json/write-str))]

          (insert-user-evidence-property! {:user_id user-id :value updated-metadata :name property-name} (u/get-db ctx)))
        (hash-map :status "success")))
    (catch Object _
      (log/error _)
      {:status "error"})))
