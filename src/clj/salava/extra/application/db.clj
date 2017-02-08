(ns salava.extra.application.db
  (:require [clojure.tools.logging :as log]
            [clojure.java.jdbc :as jdbc]
            [clojure.string :as string]
            [yesql.core :refer [defqueries]]
            [clojure.tools.logging :as log]
            [salava.core.helper :refer [dump]]
            [salava.badge.db :as b]
            [salava.badge.assertion :as a]
            [salava.core.countries :refer [all-countries sort-countries]]
            [slingshot.slingshot :refer :all]
            [salava.core.util :as u]))

(defqueries "sql/extra/application/queries.sql")

;;temp advert
(def advert
  {:remote_url "http://www.google.fi"
   :remote_id "352"
   :remote_issuer_id "22"
   :info "" 
   :application_url "https://openbadgefactory.com/c/earnablebadge/NM6JZVe7HCeH/apply"
   :issuer_content_id "92feb6b74b24f02fddac55bdd11a985e7726a6808b23368e0692752e29a13f8e"
   :badge_content_id "3368c6f212f79433f2e0c912edb82ae36cf980fa1608fb22d2467769b980b9eb"
   :criteria_content_id "0f26821bd6f068b3676058fb1bb59a5bae2c6121eec71e1bda0fbfb5615ef4cc" 
   :kind "application"
   :country "fi"
   :not_before nil
   :not_after nil})


(defn add-badge-advert [ctx advert]
  (let [{:keys [remote_url remote_id remote_issuer_id info application_url issuer_content_id badge_content_id criteria_content_id kind country not_before not_after]} advert]
    (insert-badge-advert<! {:remote_url remote_url :remote_id remote_id :remote_issuer_id remote_issuer_id :info info :application_url application_url :issuer_content_id issuer_content_id :badge_content_id badge_content_id :criteria_content_id criteria_content_id :kind kind :country country :not_before not_before :not_after not_after} (u/get-db ctx))))


(defn get-badge-adverts [ctx country name_tag issuer-name order]
  (let [where ""
        params []
        [where params] (if-not (or (empty? country) (= country "all"))
                         [(str where " AND ba.country = ?") (conj params country)]
                         [where params])
        [where params] (if-not (empty? name_tag)
                         [(str where "AND bct.tag LIKE ? OR bc.name LIKE ?" )  (conj params (str "%" name_tag "%") (str "%" name_tag "%") )]
                         [where params])
        [where params] (if-not (empty? issuer-name)
                         [(str where " AND ic.name LIKE ?") (conj params (str "%" issuer-name "%"))]
                         [where params])
        order (cond
                (= order "mtime") "ORDER BY ba.mtime DESC"
                (= order "name") "ORDER BY bc.name"
                (= order "issuer_content_name") "ORDER BY ic.name"
                :else "") 
        query (str "SELECT DISTINCT ba.id, ba.country, bc.name, ba.info, bc.image_file, ic.name AS issuer_content_name, ic.url AS issuer_content_url,GROUP_CONCAT( bct.tag) AS tags, ba.mtime FROM badge_advert AS ba
       JOIN badge_content AS bc ON (bc.id = ba.badge_content_id)
       JOIN issuer_content AS ic ON (ic.id = ba.issuer_content_id)
       LEFT JOIN badge_content_tag AS bct ON (bct.badge_content_id = ba.badge_content_id) where ba.deleted = 0
       "
                   where
                   "GROUP BY ba.id "
                   order)
        search (jdbc/with-db-connection
                 [conn (:connection (u/get-db ctx))]
                 (jdbc/query conn (into [query] params)))]
    search))

(defn user-country
  "Return user's country id"
  [ctx user-id]
  (select-user-country {:id user-id} (into {:row-fn :country :result-set-fn first} (u/get-db ctx))))

(defn badge-adverts-countries
  "Return user's country id and list of all countries which have badge adverts"
  [ctx user-id]
  (let [current-country (user-country ctx user-id)
        countries (select-badge-advert-countries {} (into {:row-fn :country} (u/get-db ctx)))]
    (hash-map :countries (-> all-countries
                             (select-keys (conj countries current-country))
                             (sort-countries)
                             (seq))
              :user-country current-country)))


(defn get-autocomplete [ctx name]
  (let [tags (select-badge-content-tags {} (u/get-db ctx))
        names (select-badge-names {} (u/get-db ctx))]
    {:tags tags
     :names names}))


(defn publish-badge [ctx data]
  (try
      {:success (= 1 (replace-badge-advert! (merge data
                                                   (b/save-badge-content!  ctx (:badge data))
                                                   (b/save-issuer-content! ctx (:client data)))
                                            (u/get-db ctx)))}
    (catch Exception ex
      (log/error "publish-badge: failed to save badge advert")
      (log/error (.toString ex))
      {:success false})))


(defn unpublish-badge [ctx data]
  (try
    {:success (= 1 (unpublish-badge-advert-by-remote! data (u/get-db ctx)))}
    (catch Exception ex
      (log/error "unpublish-badge: failed to remove badge advert")
      (log/error (.toString ex))
      {:success false})))

