(ns salava.gallery.db
  (:require [clojure.tools.logging :as log]
            [clojure.pprint :refer [pprint]]
            [yesql.core :refer [defqueries]]
            [clojure.set :refer [rename-keys]]
            [clojure.java.jdbc :as jdbc]
            [salava.core.helper :refer [dump string->number]]
            [salava.core.util :refer [get-db get-db-col get-db-1 md->html plugin-fun get-plugins]]
            [salava.core.countries :refer [all-countries sort-countries]]
            [salava.page.main :as p]
            ;[salava.social.db :as so]
            [clojure.set :refer [subset?]]
            [clojure.string :as string]
            [schema.core :as schema]
            [salava.gallery.schemas :as g]
            [salava.badge.main :as b]
            [clojure.tools.logging :as log]))

(defqueries "sql/gallery/queries.sql")

(defn- badge-count [search page_count]
  (let [limit 20
        badges-left (- (count search) (* limit (+ page_count 1)))]
    (if (pos? badges-left)
      badges-left
      0)))

(defn- select-badges [ctx gallery_ids order page_count]
  (let [limit 20
        offset (* limit page_count)]
    (if (not-empty gallery_ids)
      (case order
        "recipients"          (select-gallery-badges-order-by-recipients {:gallery_ids gallery_ids :limit limit :offset offset} (get-db ctx))
        "issuer_content_name" (select-gallery-badges-order-by-ic-name {:gallery_ids gallery_ids :limit limit :offset offset} (get-db ctx))
        "name"                (select-gallery-badges-order-by-name {:gallery_ids gallery_ids :limit limit :offset offset} (get-db ctx))
        (select-gallery-badges-order-by-ctime {:gallery_ids gallery_ids :limit limit :offset offset} (get-db ctx))))))


(defn get-gallery-ids
  "Get gallery-ids with search params"
  [ctx country tags badge-name issuer-name recipient-name]
  (let [;; Get all public gallery ids (optionally filtered by country)
          gallery-ids (if (= country "all")
                          (select-gallery-ids {} (get-db-col ctx :gallery_id))
                          (select-gallery-ids-country {:country country} (get-db-col ctx :gallery_id)))
          ;; Build a list of filter functions from query parameters
          filters
          (cond-> []
            (not (string/blank? badge-name))
            (conj (fn [id-set]
                    (clojure.set/intersection id-set (set (select-gallery-ids-badge {:badge (str "%" badge-name "%")} (get-db-col ctx :gallery_id))))))

            (not (string/blank? issuer-name))
            (conj (fn [id-set]
                    (clojure.set/intersection id-set (set (select-gallery-ids-issuer {:issuer (str "%" issuer-name "%")} (get-db-col ctx :gallery_id))))))

            (not (string/blank? recipient-name))
            (conj (fn [id-set]
                    (clojure.set/intersection id-set (set (select-gallery-ids-recipient {:recipient (str "%" recipient-name "%")} (get-db-col ctx :gallery_id))))))

            (not (string/blank? tags))
            (conj (fn [id-set]
                    (clojure.set/intersection id-set (set (select-gallery-ids-tags {:tags (->> (string/split tags #",") (map string/trim))} (get-db-col ctx :gallery_id)))))))]
      ;; Get final filtered gallery_id list
      (when (seq gallery-ids)
        (into [] (reduce (fn [coll f] (if (seq coll) (f coll) #{})) (set gallery-ids) filters)))))

(defn badge-checker [badges]
  (map (fn [b]
         (if (nil? (schema/check g/GalleryBadges b))
           b
           (do
             (log/error (str "Gallery Badge Error: ") (into (sorted-map) (assoc (schema/check g/GalleryBadges b) :badge_id (:badge_id b))))
             nil)
           )) badges))

(defn gallery-badges
  "Get badges for gallery grid"
  [ctx {:keys [country tags badge-name issuer-name order recipient-name tags-ids page_count]}]
  (let [offset (string->number page_count)
        gallery-ids (get-gallery-ids ctx country tags badge-name issuer-name recipient-name)
        badges (some->> (select-badges ctx gallery-ids order offset) (badge-checker) (remove nil?))]
    {:badges badges
     :badge_count (badge-count (if-not (= (count gallery-ids) (count badges)) gallery-ids badges) offset) }))

(defn public-badges-by-user
  "Return user's public badges"
  ([ctx user-id] (public-badges-by-user ctx user-id "public"))
  ([ctx user-id visibility]
   (select-users-public-badges {:user_id user-id :visibility visibility} (get-db ctx))))


(defn user-country
  "Return user's country id"
  [ctx user-id]
  (select-user-country {:id user-id} (into {:row-fn :country :result-set-fn first} (get-db ctx))))

(defn badge-tags
  "Get list of public badge tags for gallery grid"
  [ctx]
  {:tags (select-gallery-tags {} (get-db-col ctx :tag))})

(defn badge-countries
  "Return user's country id and list of all countries which users have public badges"
  [ctx]
  (let [;current-country (user-country ctx user-id)
        countries (select-badge-countries {} (get-db-col ctx :country))]
    (hash-map :countries (-> all-countries
                             (select-keys countries)
                             (sort-countries)
                             (seq))
              ;:user-country current-country
              )))

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

(defn- process-recipients
  "Remove duplicates from recipient list, prioritize published."
  [coll]
  (->> coll
       (group-by :id)
       (reduce-kv
         (fn [r k v]
           (conj r (if (empty? (rest v))
                     (first v)
                     (if (some #(= (:visibility %) "public") v)
                       (->> v (filter #(= "public" (:visibility %))) first)
                       (first v))) )) [])))


(defn badge-gallery-id [ctx badge_id]
  (some-> (select-gallery-id {:badge_id badge_id} (get-db ctx)) first :gallery_id))

(defn user-owns-badge? [ctx user-id badge_id]
 (let [gallery-id (badge-gallery-id ctx badge_id)
       id  (select-user-owns-badge-id {:user_id user-id :gallery_id gallery-id} (into {:result-set-fn first :row-fn :user_id} (get-db ctx)))]
  (= user-id id)))

(defn public-multilanguage-badge-content
    "Return data of the public badge by gallery and badge ids. Fetch badge criteria and issuer, uses most recent data.
    Fetch also average rating of the badge, rating count and recipient count"
  ([ctx badge-id]
   (map (fn [content]
          (-> content
              (update :criteria_content md->html)
              (assoc  :alignment (b/select-alignment-content {:badge_content_id (:badge_content_id content)} (get-db ctx)))
              (dissoc :badge_content_id)))
        (select-multi-language-badge-content {:id badge-id} (get-db ctx))))

  ([ctx badge-id user-id] (public-multilanguage-badge-content ctx badge-id user-id (badge-gallery-id ctx badge-id)))

  ([ctx badge-id user-id gallery-id]
   (let [badge-content (public-multilanguage-badge-content ctx badge-id)
         {:keys [badge_id remote_url issuer_verified endorsement_count]} (first badge-content)
         {:keys [issuer_content_name description name]} (first (filter #(= (:language_code %) (:default_language_code %)) badge-content))
         rating (select-common-badge-rating-g {:gallery_id gallery-id} (get-db-1 ctx))
         recipients (when user-id
                      (some->> (select-badge-recipients-g {:gallery_id gallery-id} (get-db ctx)) process-recipients))
         badge (merge {:badge_id badge_id :gallery_id gallery-id :remote_url remote_url :issuer_verified issuer_verified :endorsement_count endorsement_count} {:content badge-content} rating)]
     (hash-map :badge (b/badge-issued-and-verified-by-obf ctx badge)
               :public_users (->> recipients
                                  (filter #(not= (:visibility %) "private"))
                                  (map #(dissoc % :visibility))
                                  distinct)
               :private_user_count (->> recipients
                                        (filter #(= (:visibility %) "private"))
                                        count)))))

(defn public-multilanguage-badge-content-p
  ([ctx badge-id]
   (map (fn [content]
          (-> content
              (update :criteria_content md->html)
              (assoc  :alignment (b/select-alignment-content {:badge_content_id (:badge_content_id content)} (get-db ctx)))
              (dissoc :badge_content_id)))
        (select-multi-language-badge-content-p {:id badge-id} (get-db ctx))))
  ([ctx badge-id user-id] (public-multilanguage-badge-content-p ctx badge-id user-id (badge-gallery-id ctx badge-id)))
  ([ctx badge-id user-id gallery-id]
   (let [badge-content (public-multilanguage-badge-content-p ctx badge-id)
         badge {:badge_id badge-id :gallery_id gallery-id :content badge-content}]
     (hash-map :badge badge))))

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

(defn public-pages-p [ctx country owner]
  (->> (public-pages ctx country owner)
       (clojure.core.reducers/map #(-> % (dissoc :first_name :last_name :profile_picture)))
       (clojure.core.reducers/foldcat)))

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

(defn public-profiles-context
  "Search public user profiles by user's name and country, include extra data based on user badge's context when call was made."
  [ctx search-params user-id user_badge_id context]
  (let [profiles (public-profiles ctx search-params user-id)
        data-fn (case context
                  "endorsement" (as-> (first (plugin-fun (get-plugins ctx) "endorsement" "user-endorsements-status")) f)
                  nil)]
    (map #(assoc % (keyword context) (if data-fn (data-fn ctx user_badge_id user-id (:id %)) {})) profiles)))

(defn meta-tags [ctx badge-content-id]
  (let [badge-content (select-common-badge-content {:id badge-content-id} (into {:result-set-fn first} (get-db ctx)))]
    (rename-keys badge-content {:image_file :image :name :title})))

(defn gallery-stats [ctx last-login user-id]
  {:profiles {:all (gallery-profiles-count {} (into {:result-set-fn first :row-fn :profiles_count}(get-db ctx)))
              :since-last-visited (gallery-profiles-count-since-last-login {:last_login last-login} (into {:result-set-fn first :row-fn :profiles_count}(get-db ctx)))}
   :pages {:all (gallery-pages-count {} (into {:result-set-fn first :row-fn :pages_count}(get-db ctx)))
           :since-last-visited (gallery-pages-count-since-last-login {:user_id user-id :last_login last-login} (into {:result-set-fn first :row-fn :pages_count}(get-db ctx)))
           }
   :badges {:all (gallery-badges-count {} (into {:result-set-fn first :row-fn :badges_count}(get-db ctx)))
            :since-last-visited (gallery-badges-count-since-last-login {:user_id user-id :last_login last-login} (into {:result-set-fn first :row-fn :badges_count} (get-db ctx)))}
   :map {:all (all-users-on-map-count {} (into {:result-set-fn first :row-fn :users_count} (get-db ctx)))}
   })

(defn public-by-user [ctx kind user-id current-user-id]
  (let [visibility (if current-user-id "internal" "public")]
    (case kind
      "badges" {:badges (public-badges-by-user ctx user-id visibility)}
      "pages"  {:pages (public-pages-by-user ctx user-id visibility)}
      {})))

(defn badge-recipients "Return badge recipients" [ctx user-id gallery-id]
  (let [recipients (when user-id (some->> (select-badge-recipients-g {:gallery_id gallery-id} (get-db ctx)) process-recipients))]
   (hash-map :public_users (->> recipients
                                (filter #(not= (:visibility %) "private"))
                                (map #(dissoc % :visibility))
                                distinct)
             :private_user_count (->> recipients
                                      (filter #(= (:visibility %) "private"))
                                      count)
             :all_recipients_count (count recipients))))

(defn badge-rating [ctx user-id gallery-id]
 (select-common-badge-rating-g {:gallery_id gallery-id} (get-db-1 ctx)))
