(ns salava.extra.mobile.db
  (:require [clojure.tools.logging :as log]
            [clojure.string :as string]
            [clojure.pprint :refer [pprint]]
            [clojure.java.jdbc :as jdbc]
            [clojure.data.json :as json]
            [buddy.hashers :as hashers]
            [yesql.core :refer [defqueries]]
            [salava.factory.db :as factory]
            [salava.core.util :as u]))


(defqueries "sql/extra/mobile/main.sql")

(defn- png-convert-url [ctx image]
  (if (string/blank? image)
    ""
    (if (re-find #"\w+\.svg$" image)
      (str (u/get-full-path ctx) "/obpv1/file/as-png?image=" image)
      (str (u/get-site-url ctx) "/" image))))

(defonce temp-secret (u/random-token))

(defn temp-session [ctx user-id path]
  (let [payload (->> {:path (string/replace-first path #"^[^/]*/" "/")
                     :id user-id
                     :exp (+ (System/currentTimeMillis) 60000)}
                    json/write-str (map byte) byte-array u/bytes->base64-url)
        checksum (u/hmac-sha256-hex payload temp-secret)]
    (str payload "!!" checksum)))

(defn temp-session-verify [ctx token]
  (let [[payload checksum] (string/split token #"!!" 2)
        input (some-> payload u/url-base64->str (json/read-str :key-fn keyword))
        now (long (/ (System/currentTimeMillis) 1000))
        expires (+ now 3600)]
    (when (and input
               (> (:exp input) (System/currentTimeMillis))
               (= checksum (u/hmac-sha256-hex payload temp-secret)))
      {:path (:path input)
       :session {:id (:id input) :role "user" :private false :activated true :last-visited now :expires expires}})))


(defn user-badges-all [ctx user_id]
  {:badges (->> (select-user-badges-all {:user_id user_id} (u/get-db ctx))
                (map  (fn [b]
                        (-> b
                            (update :tags #(if (string/blank? %) [] (string/split % #",")))
                            (update :revoked pos?)
                            (update :image_file #(png-convert-url ctx %))
                            (update :issuer_image_file #(png-convert-url ctx %))
                            (update :creator_image_file #(png-convert-url ctx %))
                            ))))})

(defn user-badge
  "Get badge by id"
  [ctx user-badge-id user_id]
  (let [badge (select-user-badge {:id user-badge-id :user_id user_id} (u/get-db-1 ctx))]
    (some-> badge
            (merge (select-badge-detail-count {:id user-badge-id} (u/get-db-1 ctx)))
            (assoc :content (->> (select-badge-content {:badge_id (:badge_id badge)} (u/get-db ctx))
                                 (map (fn [c]
                                        (assoc c :alignment (select-badge-content-alignments
                                                              {:badge_id (:badge_id badge)
                                                               :language (:language_code c)}
                                                              (u/get-db ctx)))))))
            (update :tags #(if (string/blank? %) [] (string/split % #",")))
            (update :revoked pos?)
            (update :show_recipient_name pos?)
            (update :image_file #(png-convert-url ctx %))
            (update :issuer_image_file #(png-convert-url ctx %))
            (update :creator_image_file #(png-convert-url ctx %))

            (assoc :share_url (str (u/get-full-path ctx) "/badge/info/" user-badge-id))
            )))


(defn pending-badges-first [ctx user_id]
  (factory/save-pending-assertion-first ctx user_id)
  {:id (some->> (select-user-badges-all {:user_id user_id} (u/get-db ctx))
                (filter #(= "pending" (:status %)))
                first :id)})


(defn user-badge-endorsements [ctx user-badge-id user_id]
  {:badge  (->> (select-user-badge-endorsements {:id user-badge-id :user_id user_id} (u/get-db ctx))
                (map  (fn [b]
                        (-> b
                            (update :issuer_image_file #(png-convert-url ctx %))))))

   :issuer (->> (select-user-issuer-endorsements {:id user-badge-id :user_id user_id} (u/get-db ctx))
                (map  (fn [b]
                        (-> b
                            (update :issuer_image_file #(png-convert-url ctx %))))))

   :user   (->> (select-user-endorsements {:id user-badge-id :user_id user_id} (u/get-db ctx))
                (map  (fn [b]
                        (-> b
                            (update :id str)
                            (update :issuer_image_file #(png-convert-url ctx %))
                            (assoc :issuer_email "")
                            (assoc :issuer_description "")))))})

(defn user-badge-evidence
  "Get badge evidence"
  [ctx badge-id user-id]
  {:evidence (select-user-badge-evidence {:user_badge_id badge-id :owner user-id} (u/get-db ctx))})


(defn user-badge-congratulations
  "Get badge congratulations"
  [ctx badge-id user-id]
  {:congratulations (->> (select-user-badge-congratulations
                           {:user_badge_id badge-id :owner user-id} (u/get-db ctx))
                         (map (fn [c]
                                (-> c
                                    (update :profile_picture #(if (string/blank? %) % (str (u/get-site-url ctx) "/" %)))
                                    ))))})


;;;

(defn- gallery-badge-query-country [[join where args] country]
  (if (= country "all")
    [join where args]
    [(str join  " INNER JOIN user u ON ub.user_id = u.id")
     (str where " AND u.country = ?")
     (conj args country)]))

(defn- gallery-badge-query-name [[join where args] name]
  (if (string/blank? name)
    [join where args]
    (let [name-parts (->> (string/split (str name) #"\s+") (remove string/blank?) (map #(str "%" % "%")))
          name-where (map (constantly " AND (g.badge_name LIKE ? OR g.issuer_name LIKE ?)") name-parts)]
      [join
       (str where (apply str (interpose " " name-where)))
       (into args (mapcat #(list % %) name-parts))])))

(defn- gallery-badge-query [country name]
  (let [[join where args] (-> ["INNER JOIN user_badge ub ON g.id = ub.gallery_id"
                               (str "WHERE ub.status = 'accepted' AND ub.deleted = 0 AND ub.visibility != 'private'"
                                    " AND ub.revoked = 0 AND (ub.expires_on IS NULL OR ub.expires_on > UNIX_TIMESTAMP())")
                               []]
                              (gallery-badge-query-country country)
                              (gallery-badge-query-name name))]
    [(str "FROM gallery g " join " " where) args]))


(defn gallery-badge-search [ctx {:keys [country name order offset]}]
  (let [[from-where args] (gallery-badge-query country name)

        order-by (if (= order "name") "ORDER BY g.badge_name" "ORDER BY ub.id DESC")
        offset (if (re-find #"^[0-9]+$" offset) (str "OFFSET " (* 20 (Long. offset))) "OFFSET 0")

        sql-count (str "SELECT COUNT(DISTINCT g.id) as total " from-where)
        sql-data  (str "SELECT DISTINCT g.id AS gallery_id, NULL as advert_id,"
                       " g.badge_id, g.badge_name, g.issuer_name, g.badge_image AS image_file "
                       from-where " " order-by " LIMIT 20 " offset)
        conn (:connection (u/get-db ctx))]
    {:badges (->> (jdbc/query conn (into [sql-data] args))
                  (map #(update % :image_file (partial png-convert-url ctx))))
     :total (-> (jdbc/query conn (into [sql-count] args)) first (get :total 0))}))

;;;

(defn- gallery-earnable-badge-query-country [[join where args] country]
  (if (= country "all")
    [join where args]
    [join
     (str where " AND a.country = ?")
     (conj args country)]))

(defn- gallery-earnable-badge-query-name [[join where args] name]
  (if (string/blank? name)
    [join where args]
    (let [name-parts (->> (string/split (str name) #"\s+") (remove string/blank?) (map #(str "%" % "%")))
          name-where (map (constantly " AND (bc.name LIKE ? OR ic.name LIKE ? OR a.info LIKE ?)") name-parts)]
      [join
       (str where (apply str (interpose " " name-where)))
       (into args (mapcat #(list % % %) name-parts))])))

(defn- gallery-earnable-badge-query [country name]
  (let [[join where args] (-> [(str "INNER JOIN badge_content bc ON a.badge_content_id = bc.id "
                                    "INNER JOIN issuer_content ic ON a.issuer_content_id = ic.id")
                               (str "WHERE a.deleted = 0"
                                    " AND (a.not_before IS NULL OR a.not_before = 0 OR a.not_before <  UNIX_TIMESTAMP())"
                                    " AND (a.not_after  IS NULL OR a.not_after  = 0 OR a.not_after  >= UNIX_TIMESTAMP())")
                               []]
                              (gallery-earnable-badge-query-country country)
                              (gallery-earnable-badge-query-name name))]
    [(str "FROM badge_advert a " join " " where) args]))

(defn gallery-earnable-badge-search [ctx {:keys [country name order offset]}]
  (let [[from-where args] (gallery-earnable-badge-query country name)

        order-by (if (= order "name") "ORDER BY badge_name" "ORDER BY a.mtime DESC")
        offset (if (re-find #"^[0-9]+$" offset) (str "OFFSET " (* 20 (Long. offset))) "OFFSET 0")

        sql-count (str "SELECT COUNT(DISTINCT a.id) as total " from-where)
        sql-data  (str "SELECT DISTINCT a.id as advert_id, NULL AS gallery_id,"
                       " NULL AS badge_id, bc.name AS badge_name, ic.name AS issuer_name, bc.image_file "
                       from-where  " " order-by " LIMIT 20 " offset)
        conn (:connection (u/get-db ctx))]

    {:badges (->> (jdbc/query conn (into [sql-data] args))
                  (map #(update % :image_file (partial png-convert-url ctx))))
     :total (-> (jdbc/query conn (into [sql-count] args)) first (get :total 0))}))

;;;

(defn gallery-badge [ctx gallery_id badge_id]
  (let [badge (select-gallery-badge {:gallery_id gallery_id :badge_id badge_id} (u/get-db-1 ctx))]
    (some-> badge
            (assoc :content (->> (select-badge-content {:badge_id (:badge_id badge)} (u/get-db ctx))
                                 (map (fn [c]
                                        (assoc c :alignment (select-badge-content-alignments
                                                              {:badge_id (:badge_id badge)
                                                               :language (:language_code c)}
                                                              (u/get-db ctx)))))))
            (update :image_file #(png-convert-url ctx %))
            (update :issuer_image_file #(png-convert-url ctx %))
            (update :creator_image_file #(png-convert-url ctx %))
            )))
