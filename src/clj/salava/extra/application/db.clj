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
            [clojure.set :refer [subset?]]
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




(defn contains-tag? [query-tags tags]
  (subset? (set query-tags) (set tags)))

(defn tag-parser [tags]
  (if tags
    (string/split tags #",")))

(defn filter-tags [search tags]
  (remove (fn [advert] (not (contains-tag? tags  (tag-parser (:tags advert))))) search))


(defn map-collection
  ([where value]
   (map-collection where value true))
  ([where value fn]
   (if (and where value fn)
     {(str where)  value})))



(defn badge-adverts-where-params [country name issuer-name id]
  (let [where-params {}]
    
    (-> where-params
        ;(conj (map-collection " and ba.id = ? " id ))
        (conj (map-collection " and ba.country = ? " country (not= country "all")))
        (conj (map-collection " AND bc.name LIKE ? " (if name (str "%" name "%"))))
        (conj (map-collection " AND ic.name LIKE ? " (if issuer-name (str "%" issuer-name "%"))))
        )))

 


(defn get-badge-adverts [ctx country tags name issuer-name order id user-id show-followed-only]
  (let [where-params (badge-adverts-where-params country name issuer-name id)
        user-id (or user-id "NULL") ;set null if nil
        where   (str (apply str (keys where-params))  (if show-followed-only  " AND scba.user_id IS NOT NULL ")) ;add IS NOT NULL when user want only followed adverts
        params (cons user-id (vec (vals where-params)))  ;add user-id to params 
        tags (vec (vals tags))
        order (cond
                (= order "mtime") "ORDER BY ba.mtime DESC"
                (= order "name") "ORDER BY bc.name"
                (= order "issuer_content_name") "ORDER BY ic.name"
                :else "ORDER BY ba.mtime DESC") 
        query (str "SELECT DISTINCT ba.id, ba.country, bc.name, ba.info, bc.image_file, ic.name AS issuer_content_name, ic.url AS issuer_content_url,GROUP_CONCAT( bct.tag) AS tags, ba.mtime, ba.not_before, ba.not_after, ba.kind, IF(scba.user_id, true, false) AS followed FROM badge_advert AS ba
       JOIN badge_content AS bc ON (bc.id = ba.badge_content_id)
       JOIN issuer_content AS ic ON (ic.id = ba.issuer_content_id)
       LEFT JOIN badge_content_tag AS bct ON (bct.badge_content_id = ba.badge_content_id)
       LEFT JOIN social_connections_badge_advert AS scba ON (scba.badge_advert_id = ba.id and scba.user_id = ?) 
       where ba.deleted = 0 AND (not_before = 0 OR not_before < UNIX_TIMESTAMP()) AND (not_after = 0 OR not_after > UNIX_TIMESTAMP()) 
       "

                   where
                   "GROUP BY ba.id "
                   order)
        search (jdbc/with-db-connection
                 [conn (:connection (u/get-db ctx))]
                 (jdbc/query conn (into [query] params)))]
    (if (not-empty tags)
      (filter-tags search tags)
      search)))






(defn get-badge-advert [ctx id user_id]
  (let [badge-advert (select-badge-advert {:id id :user_id user_id} (into {:result-set-fn first} (u/get-db ctx)))]
    (update badge-advert :info u/md->html)))

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



(defn tags-where-params [country]
  (let [where-params {}]
    (if-not (= country "all")
      (merge where-params (map-collection " and ba.country = ? " country)))))

(defn get-tags [ctx country]
  (let [where-params (tags-where-params country)
        where  (apply str (keys where-params))
        params (vec (vals where-params))
        query (str "SELECT bct.tag, GROUP_CONCAT(bct.badge_content_id) AS badge_content_ids, COUNT(bct.badge_content_id) as badge_content_id_count 
                    from badge_content_tag AS bct JOIN badge_advert AS ba
         ON (bct.badge_content_id = ba.badge_content_id) where ba.deleted = 0  AND (ba.not_before = 0 OR not_before < UNIX_TIMESTAMP()) AND (ba.not_after = 0 OR not_after > UNIX_TIMESTAMP()) " 
                   where
                   " GROUP BY bct.tag 
                    ORDER BY tag 
                    LIMIT 1000") 
        search (jdbc/with-db-connection
                 [conn (:connection (u/get-db ctx))]
                 (jdbc/query conn (into [query] params))) ]
    search))





(defn get-autocomplete [ctx name country]
  (let [tags (get-tags ctx country)
        ;names (select-badge-names {} (get-db ctx))
        ]
    {:tags tags}))


(defn- advert-content [ctx data]
  (let [badge (u/json-get (:badge data))
        client (u/json-get (:client data))
        criteria (u/http-get (:criteria_url data))]
    {:badge_content_id
     (b/save-badge-content! ctx
                            {:id ""
                             :name (:name badge)
                             :image_file (u/file-from-url ctx (:image badge))
                             :description (:description badge)
                             :alignment (get badge :alignment [])
                             :tags (get badge :tags [])})
     :issuer_content_id
     (b/save-issuer-content! ctx
                             {:id ""
                              :name (:name client)
                              :description (:description client)
                              :url (:url client)
                              :email (:email client)
                              :image_file (if-not (string/blank? (:image client))
                                            (u/file-from-url ctx (:image client)))
                              :revocation_list_url (:revocationList client)})
     :criteria_content_id
     (b/save-criteria-content! ctx
                               {:id ""
                                :html_content criteria
                                :markdown_content (u/alt-markdown criteria)})}))

(defn publish-badge [ctx data]
  (try
    (replace-badge-advert!
      (merge data (advert-content ctx data))
      (u/get-db ctx))
    {:success true}
    (catch Exception ex
      (log/error "publish-badge: failed to save badge advert")
      (log/error (.toString ex))
      {:success false})))


(defn unpublish-badge [ctx data]
  (try
    (unpublish-badge-advert-by-remote! data (u/get-db ctx))
    {:success true}
    (catch Exception ex
      (log/error "unpublish-badge: failed to remove badge advert")
      (log/error (.toString ex))
      {:success false})))

(defn create-connection-badge-advert! [ctx user_id badge_advert_id]
  (try+
    (insert-connect-badge-advert<! {:user_id user_id :badge_advert_id badge_advert_id}  (u/get-db ctx))
    {:status "success"}
    (catch Object _
      {:status "error"})))

(defn delete-connection-badge-advert! [ctx user_id  badge_advert_id]
  (try+
    (delete-connect-badge-advert! {:user_id user_id :badge_advert_id badge_advert_id} (u/get-db ctx))
    {:status "success" }
    (catch Object _
      {:status "error"})))
