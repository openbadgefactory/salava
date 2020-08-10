(ns salava.mobile.db
  (:require [clojure.tools.logging :as log]
            [clojure.string :as string]
            [clojure.pprint :refer [pprint]]
            [clojure.java.jdbc :as jdbc]
            [clojure.data.json :as json]
            [yesql.core :refer [defqueries]]
            [salava.factory.db :as factory]
            [salava.core.util :as u]))


(defqueries "sql/mobile/main.sql")

(defn- png-convert-url [ctx image]
  (if (string/blank? image)
    ""
    (if (re-find #"\w+\.svg$" image)
      (str (u/get-full-path ctx) "/obpv1/file/as-png?image=" image)
      (str (u/get-site-url ctx) "/" image))))



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



(defn gallery-badge-search [ctx {:keys [country name order offset]}]
  (let [conn (:connection (u/get-db ctx))

        country-where (if (= country "all") "'all' = ?" "u.country = ?")

        name-parts (->> (string/split (str name) #"\s+") (remove string/blank?) (map #(str "%" % "%")))
        name-where (map (constantly " AND (g.badge_name LIKE ? OR g.issuer_name LIKE ?)") name-parts)

        offset (if (re-find #"^[0-9]+$" offset) (str "offset " (* 20 (Long. offset))) "offset 0")

        join "INNER JOIN user_badge ub ON g.id = ub.gallery_id INNER JOIN user u ON ub.user_id = u.id"

        where (str "WHERE " country-where " " (apply str (interpose " " name-where))
                   " "
                   "AND ub.deleted = 0 AND ub.visibility != 'private' AND ub.revoked = 0"
                   " "
                   "AND (ub.expires_on IS NULL OR ub.expires_on > UNIX_TIMESTAMP())")

        order-by (if (= order "name") "ORDER BY g.badge_name" "ORDER BY ub.id DESC")

        sql-count (str "SELECT COUNT(g.id) as total FROM gallery g " join " " where)

        sql (str "SELECT g.id, g.badge_id, g.badge_name AS name, g.badge_image AS image_file FROM gallery g"
                 " "
                 join " " where " " order-by " LIMIT 20 " offset)

        args (into [country] (mapcat #(list % %) name-parts))]

    (println sql)
    (pprint args)

    {:badges (->> (jdbc/query conn (into [sql] args))
                 (map #(update % :image_file (partial png-convert-url ctx)))
                 )
     :total (-> (jdbc/query conn (into [sql-count] args)) first :total)}))


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
