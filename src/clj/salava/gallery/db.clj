(ns salava.gallery.db
  (:require [yesql.core :refer [defqueries]]
            [clojure.set :refer [rename-keys]]
            [clojure.java.jdbc :as jdbc]
            [salava.core.helper :refer [dump string->number]]
            [salava.core.util :refer [get-db md->html]]
            [salava.core.countries :refer [all-countries sort-countries]]
            [salava.page.main :as p]
            [salava.social.db :as so]
            [clojure.set :refer [subset?]]
            [clojure.string :as string]
            [salava.badge.main :as b]))

(defqueries "sql/gallery/queries.sql")



(def badge-content-fi
  {:language-code "fi"
   :language-name  "Finnish"
   :name           "Open Badge Passport - Jäsen"
   :description    "Merkki on myönnetty henkilölle, joka on ottanut Open Badge Passport -palvelun käyttöön."
   :tags           ["jäsen" "osaamismerkit"]
   :criteria_content       "Tämä merkki on myönnetty henkilölle, joka on ottanut Open Badge Passport -palvelun käyttöön.

Open Badge Passport on Discendum Oy:n kehittämä ilmainen palvelu, johon käyttäjä voi tallentaa ansaitsemansa Open Badges -osaamismerkit. Palvelussa käyttäjä voi jakaa merkkejä sekä palvelun sisällä muille käyttäjille että palvelun ulkopuolelle sosiaalisen median eri palveluihin. Lisäksi palvelusta löytyy työkalu, jolla käyttäjä voi luoda merkeistä ja muista sisällöistä miniportfoliosivuja, joita voi julkaista muille käyttäjille ja internetiin.

**Hyödylliset linkit**

[Open Badge Passport ](http://openbadgepassport.com)

[Lisätietoa Open Badge Passport -palvelusta](https://openbadgepassport.com/fi/about) 

[Luo oma Open Badge Passport -tunnus](https://openbadgepassport.com/fi/user/register)

[Lisätietoja Open Badges -konseptista](http://openbadges.org/)
"
   :default false
   })

(def badge-content-en
  {:language-code "en"
   :language-name  "English"
   :name           "Open Badge Passport - Member"
   :description    "his Open Badge has been issued to a person, who has created an account to Open Badge Passport / who has taken Open Badge Passport into usage."
   :tags           ["member" "openbadges"]
   :criteria_content     "This Open Badge has been issued to a person, who has created an account to Open Badge Passport / who has taken Open Badge Passport into usage. 

Open Badge Passport is a free service developed by Discendum, where the user can save / store the Open Badges they’ve earned. The user can share their Open Badges to other users inside the service as well as outside the service to different social media services. Open Badge Passport has also a tool with which the user can create miniportfolio pages consisting of their Open Badges and other content and publish those pages to other users as well as to the internet. 

**Helpful links**

[More information about Open Badge Passport ](https://openbadgepassport.com/en/about)

[More information about the Open Badges -concept](http://openbadges.org)

[Create your own Open Badge Passport account](https://openbadgepassport.com/en/user/register)
"
   :default true
   })


(def dummy-badge
  {:id                  19
   :content             [(update badge-content-en :criteria_content md->html) (update badge-content-fi :criteria_content md->html)]
   :image_file          "file/e/4/3/c/e43c48360a8a0a6b75caf225d2fab021b3812dc5032b158fa834ae658a3d9b04.png"
   :issued_on           1493942400
   :rating_count 0
   :average_rating nil
   :ctime               1495430902
   :badge_content_id    "d38193c6a77672357edbc147caa4c2ed3e3e6ffe486c7667ad83c39d4aa5146f"
   :issuer_url          "http://localhost:5000/v1/client/?key=OK9XPAaLWVa1&event=OPH43Aa4RGa3&v=1.1"
   :badge_url           "http://localhost:5000/v1/badge/_/OPH42Fa4RGa1.json?v=1.1&event=OPH43Aa4RGa3"
   :obf_url             "http://localhost:5000"
   :issued_by_obf       false
   :verified_by_obf     false
   :issuer_verified     false
   :issuer_content_name "Discendum Oy"
   :issuer_content_url  "http://www.discendum.com"
   })


(defn contains-tag? [query-tags tags]
  (subset? (set query-tags) (set tags)))

(defn tag-parser [tags]
  (if tags
    (string/split tags #",")))

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
        (conj (if-not (string/blank? recipient-name) (map-collection  " AND CONCAT(u.first_name,' ',u.last_name) LIKE ?" (str "%" recipient-name "%") )))
        (conj (map-collection " and u.country = ? " country (not= country "all")))
        (conj (if-not (string/blank? name) (map-collection " AND bc.name LIKE ? " (str "%" name "%"))))
        (conj (if-not (string/blank? issuer-name) (map-collection " AND ic.name LIKE ? " (str "%" issuer-name "%")))))))


(defn filter-ids [ids] (reduce (fn [x y] (filter (set x) (set y))) (first ids) (rest ids)))

(defn tags-id-parser [tags]
  (let [parsed-ids  (filter-ids (map #(tag-parser %) (vec (vals tags))))]
    parsed-ids))

#_(defn get-badge-adverts [ctx country tags badge-name issuer-name order recipient-name tags-ids]
  
  (let [where-params (badge-adverts-where-params country badge-name issuer-name recipient-name)
        where  (apply str (keys where-params))
        params  (vec (vals where-params))  ;add user-id to params 
        tags (vec (vals tags))
        tags-ids (tags-id-parser tags-ids)
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

(def join-badge
  "INNER JOIN badge as b on bc.id = b.badge_content_id AND  b.status = 'accepted' AND b.deleted = 0 AND b.revoked = 0 AND (b.expires_on IS NULL OR b.expires_on > UNIX_TIMESTAMP()) ")
(def join-user
  "INNER JOIN user as u on b.user_id =  u.id ")
(def join-issuer
  "INNER JOIN issuer_content AS ic ON b.issuer_content_id = ic.id ")

(defn select-tags [ctx badge_content_ids]
  
  (if (not-empty badge_content_ids)
    (select-gallery-tags {:badge_content_ids badge_content_ids} (get-db ctx))
    
    ))

(defn str-ids [ids]
  (if (empty? ids)
      "()"
      (str "(" (reduce (fn [x y] (str x ",'" y "'"  )) (str "'"(first ids)"'") (rest ids)) ")")))

(defn badge-count [search page_count]
  (let [limit 48
        badges-left (- (count search) (* limit (+ page_count 1)))]
    (if (pos? badges-left)
      badges-left
      0)))

(defn select-badges [ctx badge_content_ids order page_count]
  (let [limit 48
        offset (* limit page_count)]
    (if (not-empty badge_content_ids)
      (case order
        "recipients"          (select-gallery-badges-order-by-recipients {:badge_content_ids badge_content_ids :limit limit :offset offset} (get-db ctx))
        "issuer_content_name" (select-gallery-badges-order-by-ic-name {:badge_content_ids badge_content_ids :limit limit :offset offset} (get-db ctx))
        "name"                (select-gallery-badges-order-by-name {:badge_content_ids badge_content_ids :limit limit :offset offset} (get-db ctx))
        (select-gallery-badges-order-by-ctime {:badge_content_ids badge_content_ids :limit limit :offset offset} (get-db ctx)))))
  )

(defn get-badge-adverts [ctx country tags badge-name issuer-name order recipient-name tags-ids page_count]
  (let [where-params (badge-adverts-where-params country badge-name issuer-name recipient-name)
        where        (apply str (keys where-params))
        params       (vec (vals where-params))
        ids          (tags-id-parser tags-ids)
        in-params (if (not-empty ids)
                    (str-ids ids)
                    "(SELECT DISTINCT badge_content_id FROM badge WHERE visibility != 'private' AND  status = 'accepted' AND deleted = 0 AND revoked = 0 AND (expires_on IS NULL OR expires_on > UNIX_TIMESTAMP()))")
        query (str
               "select bc.id FROM badge_content as bc 
"
               (if (or (not (string/blank? issuer-name))
                       (not (string/blank? badge-name))
                       (not (string/blank? country))
                       (not (string/blank? recipient-name)))
                 join-badge)
               (if (or (not (string/blank? country))
                       (not (string/blank? recipient-name)))
                 join-user)
               (if (not (string/blank? issuer-name))
                 join-issuer)
               " WHERE bc.id IN "
               in-params
               " "
               where
               "GROUP BY bc.id")
        search (if (and (not-empty ids) (string/blank? issuer-name) (string/blank? badge-name) (string/blank? country))
                 ids
                 (jdbc/with-db-connection
                   [conn (:connection (get-db ctx))]
                   (jdbc/query conn (into [query] params) {:row-fn :id})
                   ))]
    
    {:badges (select-badges ctx search order page_count)
     :tags (select-tags ctx search)
     :badge_count (badge-count search page_count) }))




(defn tags-where-params [country]
  (let [where-params {}]
    (if-not (= country "all")
      (merge where-params (map-collection " and u.country = ? " country )))))

#_(defn select-tags [ctx badge_content_ids]
  (select-gallery-tags {:badge_content_ids badge_content_ids} (get-db ctx)))

#_(defn get-tags [ctx country]
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
                    ORDER BY tag") 
        search (jdbc/with-db-connection
                 [conn (:connection (get-db ctx))]
                 (jdbc/query conn (into [query] params))) ]
    search))

(defn get-tags [ctx country badge_content_ids]
  (let [where-params (tags-where-params country)
        ids (tags-id-parser badge_content_ids)
        where  (apply str (keys where-params))
        params (vec (vals where-params))
        query (str "SELECT DISTINCT bct.badge_content_id from badge_content_tag AS bct 
         JOIN badge_content AS bc ON (bct.badge_content_id = bc.id)
         INNER JOIN badge as b on bc.id = b.badge_content_id AND  b.status = 'accepted' AND b.deleted = 0 AND b.revoked = 0 AND (b.expires_on IS NULL OR b.expires_on > UNIX_TIMESTAMP())
         INNER JOIN user as u on b.user_id =  u.id
         WHERE bct.badge_content_id IN
	(SELECT DISTINCT badge_content_id FROM badge WHERE visibility != 'private' AND  status = 'accepted' AND deleted = 0 AND revoked = 0 AND (expires_on IS NULL OR expires_on > UNIX_TIMESTAMP())) 
         " 
                   where
                   ) 
        search (if (not-empty ids)
                 ids
                 (time (jdbc/with-db-connection
                         [conn (:connection (get-db ctx))]
                         (jdbc/query conn (into [query] params) {:row-fn :badge_content_id})))) ]
    (if (not-empty search)
      (select-tags ctx search)
      search)
    
    ))




(defn get-autocomplete [ctx name country badge_content_ids]
  (let [tags (get-tags ctx country badge_content_ids)]
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
    (hash-map :badge dummy-badge;(b/badge-issued-and-verified-by-obf ctx badge)
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
                    LIMIT 75")
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
                    WHERE (profile_visibility = 'public' OR profile_visibility = 'internal') AND deleted = 0 AND activated = 1"
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

