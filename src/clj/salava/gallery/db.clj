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

(defn filter-tags [search tags]
  (remove (fn [advert] (not (contains-tag? tags  (tag-parser (:tags advert))))) search))


(defn map-collection
  ([where value]
   (map-collection where value true))
  ([where value fn]
   (if (and where value fn)
     {(str where)  value})))



(defn badge-adverts-where-params [country name issuer-name recipient-name]
  (let [where-params {}]
    
    (-> where-params
                                        ;(conj (map-collection " and ba.id = ? " id ))
        (conj (map-collection  " AND CONCAT(u.first_name,' ',u.last_name) LIKE ?" (if recipient-name (str "%" recipient-name "%")) ))
        (conj (map-collection " and u.country = ? " country (not= country "all")))
        (conj (map-collection " AND bc.name LIKE ? " (if name (str "%" name "%"))))
        (conj (map-collection " AND ic.name LIKE ? " (if issuer-name (str "%" issuer-name "%"))))
        
        )))

 
(def ctx {:config {:core {:site-name "Perus salava"
 
                          :share {:site-name "jeejjoee"
                                  :hashtag "KovisKisko"}
                          
                          :site-url "http://localhost:3000"
                          
                          :base-path "/app"
                          
                          :asset-version 2
                          
                          :languages [:en :fi]
                          
                          :plugins [:badge :page :gallery :file :user :oauth :admin :social :registerlink :mail]

                          :http {:host "localhost" :port 3000 :max-body 100000000}
                          :mail-sender "sender@example.com"}
                   :user {:email-notifications true}}
          :db (hikari-cp.core/make-datasource {:adapter "mysql",
                                               :username "root",
                                               :password "isokala",
                                               :database-name "salavareal",
                                               :server-name "localhost"})})





(def hakujenhaku
  "select distinct  bc.name, ic.name AS issuer_content_name, count(distinct b.id) as owners, MAX(b.ctime) AS ctime
FROM badge_content as bc 
INNER JOIN badge as b on bc.id = b.badge_content_id AND  b.status = 'accepted' AND b.deleted = 0 AND b.revoked = 0 AND (b.expires_on IS NULL OR b.expires_on > UNIX_TIMESTAMP())
INNER JOIN issuer_content AS ic ON b.issuer_content_id = ic.id 
group by bc.id
ORDER BY owners desc")

(defn get-badge-adverts [ctx country tags badge-name issuer-name order id recipient-name]
  (let [where-params (badge-adverts-where-params country badge-name issuer-name recipient-name)
        where  (apply str (keys where-params))
        params  (vec (vals where-params))  ;add user-id to params 
        ;tags (vec (vals tags))
        order (cond
                ;(= order "mtime") "ORDER BY ba.mtime DESC"
                (= order "name") "ORDER BY bc.name"
                (= order "issuer_content_name") "ORDER BY ic.name"
                (= order "owners") "ORDER BY owners DESC"
                :else "ORDER BY ctime DESC") 
        query (str
               "select bc.id, bc.name, ic.name AS issuer_content_name, count(distinct b.id) as owners, MAX(b.ctime) AS ctime
FROM badge_content as bc 
INNER JOIN badge as b on bc.id = b.badge_content_id AND  b.status = 'accepted' AND b.deleted = 0 AND b.revoked = 0 AND (b.expires_on IS NULL OR b.expires_on > UNIX_TIMESTAMP())
INNER JOIN issuer_content AS ic ON b.issuer_content_id = ic.id
INNER JOIN user as u on b.user_id =  u.id
WHERE bc.id IN
	(SELECT DISTINCT badge_content_id FROM badge WHERE visibility != 'private' AND  status = 'accepted' AND deleted = 0 AND revoked = 0 AND (expires_on IS NULL OR expires_on > UNIX_TIMESTAMP())) 
 "

                 

                  where
                   
                   "GROUP BY bc.id "
                   order
                   )
        search (jdbc/with-db-connection
                 [conn (:connection (get-db ctx))]
                 (jdbc/query conn (into [query] params)))]
    (dump where)
    search
    #_(if (not-empty tags)
      (filter-tags search tags)
      search)))


(time  (count (get-badge-adverts ctx nil nil nil nil "owners" nil nil) ))

(:name (first (get-badge-adverts ctx nil nil nil nil "mtime" nil nil nil nil)))

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

(dump )
(time (count (public-badges ctx nil nil nil nil)))
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


 id: ffb7074f567518bf28aee587e032a0023652289273f044c04d2a8fcab8d82e20
       messagecount: 2
         ownercount: 2
               name: Peukaloija
         image_file: file/a/9/c/0/a9c0fcac0b074362e21b9707b78e2a8f208e1bbe016b0ee8a391afc4b1376b96.svg
        description: Niille jotka eivät vain voi pitää näppejä erossa.
issuer_content_name: Example organisation
 issuer_content_url: http://www.example.com
              ctime: 1491893984

                id: ffb7074f567518bf28aee587e032a0023652289273f044c04d2a8fcab8d82e20
               name: Peukaloija
         image_file: file/a/9/c/0/a9c0fcac0b074362e21b9707b78e2a8f208e1bbe016b0ee8a391afc4b1376b96.svg
        description: Niille jotka eivät vain voi pitää näppejä erossa.
issuer_content_name: Example organisation
 issuer_content_url: http://www.example.com
              ctime: 1491893984
   badge_content_id: ffb7074f567518bf28aee587e032a0023652289273f044c04d2a8fcab8d82e20





explain select distinct bc.id, count(distinct bm.id) as messagecount, count(distinct b.id) as ownercount,  bc.name, bc.image_file, bc.description, ic.name AS issuer_content_name, ic.url AS issuer_content_url, MAX(b.ctime) AS ctime from badge_content as bc 
  join badge as b on bc.id = b.badge_content_id AND  b.status = 'accepted' AND b.deleted = 0 AND b.revoked = 0
  inner join badge_message as bm on bc.id = bm.badge_content_id and bm.deleted = 0
  JOIN issuer_content AS ic ON b.issuer_content_id = ic.id 
group by bc.id;

explain select distinct bc.id, count(distinct bm.id) as messagecount, count(distinct b.id) as ownercount,  bc.name, bc.image_file, bc.description, ic.name AS issuer_content_name, ic.url AS issuer_content_url, MAX(b.ctime) AS ctime from badge as b 
  inner join  badge_content as bc  on  b.badge_content_id = bc.id
  inner join badge_message as bm on b.badge_content_id = bm.badge_content_id and bm.deleted = 0
inner JOIN issuer_content AS ic ON b.issuer_content_id = ic.id
WHERE  b.status = 'accepted' AND b.deleted = 0 AND b.revoked = 0
group by bc.id;


ALTER TABLE `badge_message` ADD INDEX `badge_content_id` (`badge_content_id`)
ALTER TABLE `badge` ADD INDEX `badge_content_id` (`badge_content_id`)
ALTER TABLE `badge` ADD INDEX `issuer_content_id` (`issuer_content_id`)
name


;; select badge_content and messages
;; here can add "in this month"

select count(distinct bm.id) as count, bm.badge_content_id from badge_message as bm group by bm.badge_content_id

;; select owners and badge_content

;; - here can add "in this month"

select count(distinct b.id) as count, b.badge_content_id from badge as b group by b.badge_content_id ORDER by count desc


select count(distinct bm.id) as count, b.badge_content_id from badge as b
join badge_message as bm on b.badge_content_id = bm.badge_content_id and bm.deleted = 0
GROUP BY b.badge_content_id;

;;tarvitaan:
;; badge_content_id, bc.name,bc.image_file, ic_name

select 

select distinct  bc.name, ic.name AS issuer_content_name, count(distinct b.id) as owners, MAX(b.ctime) AS ctime
FROM badge_content as bc 
INNER JOIN badge as b on bc.id = b.badge_content_id AND  b.status = 'accepted' AND b.deleted = 0 AND b.revoked = 0
INNER JOIN issuer_content AS ic ON b.issuer_content_id = ic.id 
group by bc.id
ORDER BY owners desc
                                      


(defn get-stuff [ctx]
  )
