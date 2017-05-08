(ns salava.gallery.db
  (:require [yesql.core :refer [defqueries]]
            [clojure.set :refer [rename-keys]]
            [clojure.java.jdbc :as jdbc]
            [salava.core.helper :refer [dump]]
            [salava.core.util :refer [get-db md->html]]
            [salava.core.countries :refer [all-countries sort-countries]]
            [salava.page.main :as p]
            [salava.social.db :as so]
            [clojure.set :refer [subset?]]
            [clojure.string :as string]
            [salava.badge.main :as b]))

(defqueries "sql/gallery/queries.sql")


(defn contains-tag? [query-tags tags]
  (subset? (set query-tags) (set tags)))

(defn tag-parser [tags]
  (if tags
    (string/split tags #",")))

(set (mapcat #(tag-parser %) ["82e9715b8938a11845d563be82b755d767b9d6ef1fd2f14fd2bd9f74525466a0,392533871608845892b1df118eb414493693df1fefd66b1544bd0d7dc5ee7f69" "82e9715b8938a11845d563be82b755d767b9d6ef1fd2f14fd2bd9f74525466a0"]))

(defn filter-tags [search tags]
  (remove (fn [badge] (not (contains-tag? tags  (tag-parser (:tags badge))))) search))


(defn map-collection
  ([where value]
   (map-collection where value true))
  ([where value fn]
   (if (and where value fn)
     {(str where) value})))



(defn badge-adverts-where-params [country name issuer-name recipient-name]
  (let [where-params {}]
    
    (-> where-params
                                        ;(conj (map-collection " and ba.id = ? " id ))
        (conj (map-collection  " AND CONCAT(u.first_name,' ',u.last_name) LIKE ?" (if recipient-name (str "%" recipient-name "%")) ))
        (conj (map-collection " and u.country = ? " country (not= country "all")))
        (conj (map-collection " AND bc.name LIKE ? " (if name (str "%" name "%"))))
        (conj (map-collection " AND ic.name LIKE ? " (if issuer-name (str "%" issuer-name "%")))))))

(defn tags-id-parser [tags]
  (let [parsed-ids (mapcat #("'"(tag-parser %) "'") (vec (vals tags)))]
    (dump parsed-ids)
    parsed-ids))

(defn get-badge-adverts [ctx country tags badge-name issuer-name order recipient-name tags-ids]
  
  (let [where-params (badge-adverts-where-params country badge-name issuer-name recipient-name)
        where  (apply str (keys where-params))
        params  (vec (vals where-params))  ;add user-id to params 
        tags (vec (vals tags))
        ;tags-ids (tags-id-parser tags-ids)
        texxt (if false ;(not-empty tags-ids)
                (str "(" (map #(str "'" % "'") tags-ids)")")
                "(SELECT DISTINCT badge_content_id FROM badge WHERE visibility != 'private' AND  status = 'accepted' AND deleted = 0 AND revoked = 0 AND (expires_on IS NULL OR expires_on > UNIX_TIMESTAMP()))")
        order (cond
                ;(= order "mtime") "ORDER BY ba.mtime DESC"
                (= order "name") "ORDER BY bc.name"
                (= order "issuer_content_name") "ORDER BY ic.name"
                (= order "recipients") "ORDER BY recipients DESC"
                :else "ORDER BY ctime DESC") 
        query (str
               "select bc.id, bc.name, bc.image_file, ic.name AS issuer_content_name, count(distinct b.id) as recipients, MAX(b.ctime) AS ctime, GROUP_CONCAT(distinct bct.tag) AS tags
FROM badge_content as bc 
INNER JOIN badge as b on bc.id = b.badge_content_id AND  b.status = 'accepted' AND b.deleted = 0 AND b.revoked = 0 AND (b.expires_on IS NULL OR b.expires_on > UNIX_TIMESTAMP())
INNER JOIN issuer_content AS ic ON b.issuer_content_id = ic.id
INNER JOIN user as u on b.user_id =  u.id
LEFT JOIN badge_content_tag AS bct ON (bc.id = bct.badge_content_id)
WHERE bc.id IN 
 "
              texxt
                  where
                   "GROUP BY bc.id "
                   order
                   
                   )
        search (jdbc/with-db-connection
                 [conn (:connection (get-db ctx))]
                 (jdbc/query conn (into [query] params)))]
    #_(dump (if (not-empty tags)
      (filter-tags search tags)
      search))
    
    search
    #_(if (not-empty tags)
      (filter-tags search tags)
      search)))

(defn tags-where-params [country]
  (let [where-params {}]
    (if-not (= country "all")
      (merge where-params (map-collection " and ba.country = ? " country)))))

(defn get-tags [ctx country]
  (let [where-params (tags-where-params country)
        where  (apply str (keys where-params))
        params (vec (vals where-params))
        query (str "SELECT bct.tag, GROUP_CONCAT(bct.badge_content_id) AS badge_content_ids, COUNT(bct.badge_content_id) as badge_content_id_count 
                    from badge_content_tag AS bct 
         JOIN badge_content AS bc ON (bct.badge_content_id = bc.id)
         INNER JOIN badge as b on bc.id = b.badge_content_id AND  b.status = 'accepted' AND b.deleted = 0 AND b.revoked = 0 AND (b.expires_on IS NULL OR b.expires_on > UNIX_TIMESTAMP())
         INNER JOIN user as u on b.user_id =  u.id
         WHERE bct.badge_content_id IN
	(SELECT DISTINCT badge_content_id FROM badge WHERE visibility != 'private' AND  status = 'accepted' AND deleted = 0 AND revoked = 0 AND (expires_on IS NULL OR expires_on > UNIX_TIMESTAMP())) 
         " 
                   where
                   " GROUP BY bct.tag 
                    ORDER BY tag 
                    LIMIT 500") 
        search (jdbc/with-db-connection
                 [conn (:connection (get-db ctx))]
                 (jdbc/query conn (into [query] params))) ]
    search))


(defn get-autocomplete [ctx name country]
  (let [tags (get-tags ctx country)]
    {:tags tags}))





(defn public-badges-by-user
  "Return user's public badges"
  ([ctx user-id] (public-badges-by-user ctx user-id "public"))
  ([ctx user-id visibility]
   (select-users-public-badges {:user_id user-id :visibility visibility} (get-db ctx))))



(defn public-badges
  "Return badges visible in gallery. Badges can be searched by country ID, badge name, issuer name or recipient's first or last name"
  [ctx country badge-name issuer-name recipient-name]
  (let [where ""
        params []
        default-visibility " AND (b.visibility = 'public' OR b.visibility = 'internal')"
        [where params] (if-not (or (empty? country) (= country "all"))
                         [(str where " AND u.country = ?") (conj params country)]
                         [where params])
        [where params] (if-not (empty? badge-name)
                         [(str where " AND bc.name LIKE ?") (conj params (str "%" badge-name "%"))]
                         [where params])
        [where params] (if-not (empty? issuer-name)
                         [(str where " AND ic.name LIKE ?") (conj params (str "%" issuer-name "%"))]
                         [where params])
        [where params] (if-not (empty? recipient-name)
                         [(str where " AND CONCAT(u.first_name,' ',u.last_name) LIKE ?") (conj params (str "%" recipient-name "%"))]
                         [where params])
        [where params] (if (and (not (empty? issuer-name)) (empty? recipient-name)) ;if issuer name is present but recipient name is not, search also private badges
                         [where params]
                         [(str where default-visibility) params])
        query (str "SELECT bc.id, bc.name, bc.image_file, bc.description, ic.name AS issuer_content_name, ic.url AS issuer_content_url, MAX(b.ctime) AS ctime, badge_content_id  FROM badge AS b
                    JOIN badge_content AS bc ON b.badge_content_id = bc.id
                    JOIN issuer_content AS ic ON b.issuer_content_id = ic.id
                    LEFT JOIN user AS u ON b.user_id = u.id
                    WHERE b.status = 'accepted' AND b.deleted = 0 AND b.revoked = 0 AND (b.expires_on IS NULL OR b.expires_on > UNIX_TIMESTAMP())"
                   where
                   " GROUP BY bc.id, bc.name, bc.image_file, bc.description, ic.name, ic.url, b.badge_content_id
                    ORDER BY ctime DESC")
        badgesearch (jdbc/with-db-connection
                  [conn (:connection (get-db ctx))]
                  (jdbc/query conn (into [query] params)))
        badge_contents (map :badge_content_id badgesearch)
        recipients (if (not-empty badge_contents) (select-badges-recipients {:badge_content_ids badge_contents } (get-db ctx)))
        recipientsmap (reduce #(assoc %1 (:badge_content_id %2) (:recipients %2)) {} recipients)
        assochelper (fn [user recipients] (assoc user  :recipients (get recipientsmap (:badge_content_id user))))]
    (map assochelper badgesearch recipients)))



(defn user-country
  "Return user's country id"
  [ctx user-id]
  (select-user-country {:id user-id} (into {:row-fn :country :result-set-fn first} (get-db ctx))))

(defn badge-countries
  "Return user's country id and list of all countries which users have public badges"
  [ctx user-id]
  (let [current-country (user-country ctx user-id)
        countries (select-badge-countries {} (into {:row-fn :country} (get-db ctx)))]
    (hash-map :countries (-> all-countries
                             (select-keys (conj countries current-country))
                             (sort-countries)
                             (seq))
              :user-country current-country)))

(defn page-countries
  "Return user's country id and list of all countries which users have public pages"
  [ctx user-id]
  (let [current-country (user-country ctx user-id)
        countries (select-page-countries {} (into {:row-fn :country} (get-db ctx)))]
    (hash-map :countries (-> all-countries
                             (select-keys (conj countries current-country))
                             (sort-countries)
                             (seq))
              :user-country current-country)))

(defn profile-countries [ctx user-id]
  (let [current-country (user-country ctx user-id)
        countries (select-profile-countries {} (into {:row-fn :country} (get-db ctx)))]
    (-> all-countries
        (select-keys (conj countries current-country))
        (sort-countries)
        (seq))))

(defn public-badge-content
  "Return data of the public badge by badge-content-id. Fetch badge criteria and issuer data. If user has not received the badge use most recent criteria and issuer. Fetch also average rating of the badge, rating count and recipient count"
  [ctx badge-content-id user-id]
  (let [badge-content (select-common-badge-content {:id badge-content-id} (into {:result-set-fn first} (get-db ctx)))
        recipient-badge-data (select-badge-criteria-issuer-by-recipient {:badge_content_id badge-content-id :user_id user-id} (into {:result-set-fn first} (get-db ctx)))
        badge-data (or recipient-badge-data (select-badge-criteria-issuer-by-date {:badge_content_id badge-content-id} (into {:result-set-fn first} (get-db ctx))))
        rating (select-common-badge-rating {:badge_content_id badge-content-id} (into {:result-set-fn first} (get-db ctx)))
        recipients (if user-id (select-badge-recipients {:badge_content_id badge-content-id} (get-db ctx)))
        ;badge-message-count (if user-id {:message_count (so/get-badge-message-count ctx badge-content-id user-id)})
        ;followed? (if user-id {:followed? (so/is-connected? ctx user-id badge-content-id)})
        badge (merge badge-content (update badge-data :criteria_content md->html) rating ;badge-message-count ;followed?
                     )]
    (hash-map :badge (b/badge-issued-and-verified-by-obf ctx badge)
              :public_users (->> recipients
                                 (filter #(not= (:visibility %) "private"))
                                 (map #(dissoc % :visibility))
                                 distinct)
              :private_user_count (->> recipients
                                       (filter #(= (:visibility %) "private"))
                                       count))))

(defn public-pages-by-user
  "Return all public pages owned by user"
  ([ctx user-id] (public-badges-by-user ctx user-id "public"))
  ([ctx user-id visibility]
   (let [pages (select-users-public-pages {:user_id user-id :visibility visibility} (get-db ctx))]
     (p/page-badges ctx pages))))

(defn public-pages
  "Return public pages visible in gallery. Pages can be searched with page owner's name and/or country"
  [ctx country owner]
  
  (let [where ""
        params []
        [where params] (if-not (or (empty? country) (= country "all"))
                         [(str where " AND u.country = ?") (conj params country)]
                         [where params])
        [where params] (if-not (empty? owner)
                         [(str where " AND CONCAT(u.first_name,' ',u.last_name) LIKE ?") (conj params (str "%" owner "%"))]
                         [where params])
        query (str "SELECT p.id, p.ctime, p.mtime, user_id, name, description, u.first_name, u.last_name, u.profile_picture, GROUP_CONCAT(pb.badge_id) AS badges FROM page AS p
                    JOIN user AS u ON p.user_id = u.id
                    LEFT JOIN page_block_badge AS pb ON pb.page_id = p.id
                    WHERE (visibility = 'public' OR visibility = 'internal') AND p.deleted = 0"
                   where
                   " GROUP BY p.id, p.ctime, p.mtime, user_id, name, description, u.first_name, u.last_name, u.profile_picture
                    ORDER BY p.mtime DESC
                    LIMIT 100")
        pages (jdbc/with-db-connection
                [conn (:connection (get-db ctx))]
                (jdbc/query conn (into [query] params)))]
    (p/page-badges ctx pages)))

(defn public-profiles
  "Searcn public user profiles by user's name and country"
  [ctx search-params user-id]
  (let [{:keys [name country common_badges order_by]} search-params
        where ""
        order (case order_by
                "ctime" " ORDER BY ctime DESC"
                "name" " ORDER BY last_name, first_name"
                "")
        params []
        [where params] (if-not (or (empty? country) (= country "all"))
                         [(str where " AND country = ?") (conj params country)]
                         [where params])
        [where params] (if-not (empty? name)
                         [(str where " AND CONCAT(first_name,' ',last_name) LIKE ?") (conj params (str "%" name "%"))]
                         [where params])
        query (str "SELECT id, first_name, last_name, country, profile_picture, ctime
                    FROM user
                    WHERE (profile_visibility = 'public' OR profile_visibility = 'internal') AND deleted = 0"
                   where
                   order)
        profiles (jdbc/with-db-connection
                   [conn (:connection (get-db ctx))]
                   (jdbc/query conn (into [query] params)))
        common-badge-counts (if-not (empty? profiles)
                              (->>
                                (select-common-badge-counts {:user_id user-id :user_ids (map :id profiles)} (get-db ctx))
                                (reduce #(assoc %1 (:user_id %2) (:c %2)) {})))
        profiles-with-badges (map #(assoc % :common_badge_count (get common-badge-counts (:id %) 0)) profiles)
        visible-profiles (filter #(if common_badges
                                   (pos? (:common_badge_count %))
                                   identity) profiles-with-badges)]
    (if (= order_by "common_badge_count")
      (->> visible-profiles
           (sort-by :common_badge_count >)
           (take 100))
      (->> visible-profiles
           (take 100)))))

(defn meta-tags [ctx badge-content-id]
  (let [badge-content (select-common-badge-content {:id badge-content-id} (into {:result-set-fn first} (get-db ctx)))]
    (rename-keys badge-content {:image_file :image :name :title})))

