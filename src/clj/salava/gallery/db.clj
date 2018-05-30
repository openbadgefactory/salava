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
            [schema.core :as schema]
            [salava.gallery.schemas :as g]
            [salava.badge.main :as b]
            [clojure.tools.logging :as log]))

(defqueries "sql/gallery/queries.sql")


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

(defn badge-ids-where-params
  "Create map from search params
  Example:
  {' and u.country = ? ', 'fi'}"
  [country name issuer-name recipient-name]
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

(def join-badge
  "INNER JOIN user_badge as ub on b.id = ub.badge_id AND  ub.status = 'accepted' AND ub.deleted = 0 AND ub.revoked = 0 AND (ub.expires_on IS NULL OR ub.expires_on > UNIX_TIMESTAMP()) ")

(def join-user
  "INNER JOIN user as u on ub.user_id =  u.id ")

(def join-issuer
  (str
    "JOIN badge_issuer_content AS bic ON (bic.badge_id = b.id) "
    "JOIN issuer_content AS ic ON (ic.id = bic.issuer_content_id) AND ic.language_code = b.default_language_code "
    ))

(defn select-tags [ctx badge_ids]
  (if (not-empty badge_ids)
    (select-gallery-tags {:badge_ids badge_ids} (get-db ctx))))

(defn str-ids [ids]
  (str "('" (apply str (interpose "','" (map #(string/replace % #"\W" "") ids))) "')"))

(defn badge-count [search page_count]
  (let [limit 20
        badges-left (- (count search) (* limit (+ page_count 1)))]
    (if (pos? badges-left)
      badges-left
      0)))

(defn select-badges [ctx badge_ids order page_count]
  (let [limit 20
        offset (* limit page_count)]
    (if (not-empty badge_ids)
      (case order
        "recipients"          (select-gallery-badges-order-by-recipients {:badge_ids badge_ids :limit limit :offset offset} (get-db ctx))
        "issuer_content_name" (select-gallery-badges-order-by-ic-name {:badge_ids badge_ids :limit limit :offset offset} (get-db ctx))
        "name"                (select-gallery-badges-order-by-name {:badge_ids badge_ids :limit limit :offset offset} (get-db ctx))
        (select-gallery-badges-order-by-ctime {:badge_ids badge_ids :limit limit :offset offset} (get-db ctx))))))



(defn db-connect [ctx query params]
  (jdbc/with-db-connection
    [conn (:connection (get-db ctx))]
    (jdbc/query conn (into [query] params) {:row-fn :id})))


(defn get-badge-ids
  "Get badge-ids with search params"
  [ctx country tags badge-name issuer-name order recipient-name tags-ids]
  (let [where-params (badge-ids-where-params country badge-name issuer-name recipient-name)
        where        (apply str (keys where-params))
        params       (vec (vals where-params))
        ids          (tags-id-parser tags-ids)
        in-params (if (not-empty ids)
                    (str-ids ids)
                    "(SELECT DISTINCT id FROM badge WHERE published = 1 AND recipient_count > 0)")
        query (str
                "SELECT b.id FROM badge as b "
                "JOIN badge_badge_content AS bbc ON (bbc.badge_id = b.id) "
                "JOIN badge_content AS bc ON (bc.id = bbc.badge_content_id) AND bc.language_code = b.default_language_code "
                (if (or (not (string/blank? issuer-name))
                        (not (string/blank? badge-name))
                        (not (string/blank? country))
                        (not (string/blank? recipient-name)))
                  join-badge) ;import badge table to SQL query
                (if (or (not (string/blank? country))
                        (not (string/blank? recipient-name)))
                  join-user) ;import user table to SQL query
                (if (not (string/blank? issuer-name))
                  join-issuer)
                " WHERE b.id IN "
                in-params
                " "
                where
                "GROUP BY b.id")
        search (if (and (not-empty ids) (string/blank? issuer-name) (string/blank? badge-name) (string/blank? country))
                 ids
                 (db-connect ctx query params))]
    search))

(defn badge-checker [badges]
  (map (fn [b]
         (if (nil? (schema/check g/GalleryBadges b))
           b
           (do
             (log/debug (str "Gallery Badge Error: ") (into (sorted-map) (assoc (schema/check g/GalleryBadges b) :badge_id (:badge_id b))))
             nil)
           )) badges))

(defn get-gallery-badges
  "Get badge-ids"
  [ctx country tags badge-name issuer-name order recipient-name tags-ids page_count]
  (let [badge-ids (get-badge-ids ctx country tags badge-name issuer-name order recipient-name tags-ids)
        badges (remove nil? (badge-checker (select-badges ctx badge-ids order page_count)))]
    {:badges badges
     :tags (select-tags ctx badge-ids)
     :badge_count (badge-count (if-not (= (count badge-ids) (count badges)) badge-ids badges) page_count) }))




(defn tags-where-params [country]
  (let [where-params {}]
    (if-not (= country "all")
      (merge where-params (map-collection " and u.country = ? " country )))))


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
      search)))




(defn get-autocomplete [ctx name country badge_content_ids]
  (let [tags (get-tags ctx country badge_content_ids)]
    {:tags tags}))





(defn public-badges-by-user
  "Return user's public badges"
  ([ctx user-id] (public-badges-by-user ctx user-id "public"))
  ([ctx user-id visibility]
   (select-users-public-badges {:user_id user-id :visibility visibility} (get-db ctx))))


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


(defn public-multilanguage-badge-content
  "Return data of the public badge by badge-content-id. Fetch badge criteria and issuer data. If user has not received the badge use most recent criteria and issuer. Fetch also average rating of the badge, rating count and recipient count"
  [ctx badge-id user-id]
  (let [badge-content (map (fn [content]
                             (-> content
                                 (update :criteria_content md->html)
                                 (assoc  :alignment (b/select-alignment-content {:badge_content_id (:badge_content_id content)} (get-db ctx)))
                                 (dissoc :badge_content_id)))
                           (select-multi-language-badge-content {:id badge-id} (get-db ctx)))
        {:keys [badge_id remote_url issuer_verified endorsement_count]} (first badge-content)

        rating (select-common-badge-rating {:badge_id badge-id} (into {:result-set-fn first} (get-db ctx)))
        {:keys [issuer_content_name description name]} (first (filter #(= (:language_code %) (:default_language_code %)) badge-content))
        ;recipients (if user-id (select-badge-recipients {:badge_id badge-id} (get-db ctx)))
        recipients (if user-id (select-badge-recipients-fix {:issuer_content_name issuer_content_name :description description :name name} (get-db ctx)))
        badge (merge {:badge_id badge_id :remote_url remote_url :issuer_verified issuer_verified :endorsement_count endorsement_count} {:content badge-content} rating ;(update badge-data :criteria_content md->html)  ;badge-message-count ;followed?
                     )]
    (hash-map :badge (b/badge-issued-and-verified-by-obf ctx badge)
              :public_users (->> recipients
                                 (filter #(not= (:visibility %) "private"))
                                 (map #(dissoc % :visibility))
                                 distinct)
              :private_user_count (->> recipients
                                       (filter #(= (:visibility %) "private"))
                                       count))))


(defn public-badge-content
  "Return data of the public badge by badge-content-id. Fetch badge criteria and issuer data. If user has not received the badge use most recent criteria and issuer. Fetch also average rating of the badge, rating count and recipient count"
  [ctx badge-id user-id]
  (let [badge-content (select-common-badge-content {:id badge-id} (into {:result-set-fn first} (get-db ctx)))
        recipient-badge-data (select-badge-criteria-issuer-by-recipient {:badge_content_id badge-id :user_id user-id} (into {:result-set-fn first} (get-db ctx)))
        badge-data (or recipient-badge-data (select-badge-criteria-issuer-by-date {:badge_content_id badge-id} (into {:result-set-fn first} (get-db ctx))))
        rating (select-common-badge-rating {:badge_id badge-id} (into {:result-set-fn first} (get-db ctx)))
        recipients (if user-id (select-badge-recipients {:badge_id badge-id} (get-db ctx)))
        badge (merge badge-content (update badge-data :criteria_content md->html) rating)]
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
  [ctx user-id visibility]
  (let [pages (select-users-public-pages {:user_id user-id :visibility visibility} (get-db ctx))]
    (p/page-badges ctx pages)))

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
  "Search public user profiles by user's name and country"
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

