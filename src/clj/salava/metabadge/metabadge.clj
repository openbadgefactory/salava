(ns salava.metabadge.metabadge
  (:require [salava.core.http :as http]
            [salava.core.util :as u]
            [clojure.tools.logging :as log]
            [clojure.core.cache :as cache]
            [slingshot.slingshot :refer :all]
            [pantomime.mime :refer [extension-for-name mime-type-of]]
            [clojure.core.reducers :as r]
            [salava.metabadge.db :as db]
            [clojure.string :as string]
            [yesql.core :refer [defqueries]]
            [clojure.data.json :as json]))

(defqueries "sql/metabadge/queries.sql")

(def no-of-days-in-cache 3)

(defonce metabadge-cache-storage (atom (-> {} (cache/lru-cache-factory :threshold 200) (cache/ttl-cache-factory :ttl (* (* 86400 1000) no-of-days-in-cache)))))

(defn url? [s]
  (not (string/blank? (re-find #"^http" (str s)))))

(defn %completed [req gotten]
  (let [calc (Math/round (double (* (/ gotten req) 100)))]
    (if (> calc 100) 100 calc)))

(defn- file-extension [filename]
  (try+
   (let [file (if (re-find #"https?" (str filename)) (java.net.URL. filename) filename)]
     (-> file mime-type-of))
   (catch Object _
     (throw+ (str "Could not get extension for file: " filename)))))

(defn- fetch-json-data [url]
  (try
    (http/http-get url {:as :json :accept :json})
    (catch Exception e
      (log/error (str "Could not fetch data: " url))
      {})))

(defn fetch-image [ctx url]
  (let [ext (file-extension url)]
    (try
      (if-let [image (u/bytes->base64 (http/http-get url {:as :byte-array}))]
        (str "data:" ext ";base64, " image))
      (catch Exception _
        (log/error (str "Could not fetch image: " url))
        url))))

(defn- public-badge-info [ctx id]
  (let [url (str (get-in ctx [:config :factory :url]) "/v1/badge/_/" id ".json?v=2.0")]
    (try
      (if-let [response (fetch-json-data url)]
        (let [criteria (u/md->html (:criteria response))
              data (-> response (assoc :criteria criteria))]
          (if (url? (:image data))
            (-> data (assoc :image (fetch-image ctx (:image data))))
            data)))
      (catch Exception _
        (log/error (str "Did not get required badge information: " id))
        {}))))

(defn get-badge-data [ctx badge]
  (let [key (:id badge)]
    (cache/lookup (swap! metabadge-cache-storage
                         #(if (cache/has? % key)
                            (cache/hit % key)
                            (cache/miss % key
                                        (-> (public-badge-info ctx key)
                                            (dissoc :language :alt_language :tags :id :alignment))))) key)))

(defn lookup-badge [ctx metabadge_id milestone badge]
  (if (:received badge) ;;:received is not a reliable flag!
    (let [badge-info (-> (db/user-badge-by-assertion ctx (:url badge)) first)
          factory_metabadge_required (->> (select-factory-metabadge-required {:metabadge_id metabadge_id :required_badge_id (:id badge)} (u/get-db ctx)) first)]
      (->> (if (empty? badge-info) (assoc (get-badge-data ctx badge) :received true :user_badge_id 0) (assoc badge-info :url (:url badge))) (merge factory_metabadge_required)))
    (if-not milestone
      (or (->> (select-factory-metabadge-required {:metabadge_id metabadge_id :required_badge_id (:id badge)} (u/get-db ctx)) first) (assoc (get-badge-data ctx badge) :received false))
      (or (->> (select-factory-metabadge {:id metabadge_id} (u/get-db ctx)) first) (assoc (get-badge-data ctx badge) :received false)))))

(defn get-required-badges [ctx user_id metabadge_id user_badge_id]
  (let [all-required-badges (select-all-required-badges {:metabadge_id metabadge_id} (u/get-db ctx))
        received-required-badges (some->> (select-received-required-badges {:metabadge_id metabadge_id :user_id user_id} (u/get-db ctx))
                                          (filter (fn [badge] (some #(= (select-keys badge [:name :required_badge_id]) (select-keys % [:name :required_badge_id])) all-required-badges)))
                                          (group-by :required_badge_id)
                                          (reduce-kv
                                           (fn [r k v]
                                             (conj r (if (empty? (rest v))
                                                       (first v)
                                                       (if (some #(= (str (:user_badge_id %)) (str user_badge_id)) v)
                                                         (->> (remove #(not (= (str user_badge_id) (str (:user_badge_id %)))) v) first)
                                                         (->> v (sort-by :issued_on <) (sort-by :deleted) first))))) []))
        not-received-required-badges (some->> all-required-badges
                                              (remove (fn [badge] (some #(= (select-keys badge [:required_badge_id]) (select-keys % [:required_badge_id])) received-required-badges))))]
    (assoc {} :required_badges ((comp flatten concat) received-required-badges not-received-required-badges)
           :received-badge-count (count received-required-badges))))

(defn expand-required-badges-db
  "build required badges from db"
  [ctx user_id user_badge_id coll]
  (reduce (fn [r m]
            (let [required-badges-map (get-required-badges ctx user_id (:metabadge_id m) user_badge_id)
                  min_required (if (pos? (:min_required m)) (:min_required m) (->> required-badges-map :required_badges count))]
              (conj r (assoc m :required_badges (->> required-badges-map :required_badges)
                             :min_required min_required
                             :completion_status (%completed min_required (->> required-badges-map :received-badge-count)))))) [] coll))

(defn expand-required-badges
  "use cache lookup to build required badges"
  [ctx metabadge_id badges assertion-url]
  (->> badges (r/map #(lookup-badge ctx metabadge_id nil %)) (r/foldcat)))

(defn- process-metabadge [ctx metabadge assertion-url]
  (->> (:metabadge metabadge)
       (r/map (fn [m]
                (let [required-badges (expand-required-badges ctx (:id m) (:required_badges m) assertion-url)
                      received-required-badges (filter (fn [b] (and (:user_badge_id b) (pos? (:user_badge_id b)))) required-badges)
                      min_required (if (pos? (:min_required m)) (:min_required m) (count (:required_badges m)))
                      saved-metabadge (select-factory-metabadge {:id (:id m)} (u/get-db ctx))]
                  (when (empty? saved-metabadge)
                    (do (log/info "Metabadge info not found in db, Getting metabadge info from factory")
                        (db/get-metabadge! ctx (get-in ctx [:config :factory :url]) {:assertion_url assertion-url})))
                  (-> m
                      (assoc :name (:name m) :min_required min_required)
                      (merge (dissoc (lookup-badge ctx (:id m) "milestone" (:badge m)) :name))
                      (assoc :required_badges required-badges
                             :completion_status (%completed min_required (count received-required-badges))
                             :milestone? (= assertion-url (get-in m [:badge :url])))
                      (dissoc :badge :remote_issuer_id :alignment)))))
       (r/foldcat)))

(defn assertion-url->related-id [ctx assertion-url]
  (if (string/blank? assertion-url)
    ""
    (-> (select-assertion-json-by-assertion-url {:url assertion-url} (u/get-db-col ctx :assertion_json))
        first
        (json/read-str :key-fn keyword)
        :related first :id)))

(defn check-metabadge [ctx assertion-url]
  (let [factory-url (get-in ctx [:config :factory :url])
        meta-data-url (str factory-url "/v1/assertion/metabadge/?url=" (u/url-encode assertion-url))
        related-id (assertion-url->related-id ctx assertion-url)]
    (if-let [check (or (string/starts-with? assertion-url factory-url) (string/starts-with? related-id factory-url))]
      (try
        (if-let [metabadge (fetch-json-data meta-data-url)]
          (let [processed-metabadge (process-metabadge ctx  metabadge assertion-url)]
            (if (empty? processed-metabadge) nil (->>  processed-metabadge
                                                       (group-by :milestone?)
                                                       (reduce-kv (fn [r k v]
                                                                    (if (true? k)
                                                                      (assoc r :milestones v) (assoc r :required-in v)))
                                                                  {}))))
          nil)
        (catch Exception e
          (log/error (str "Did not get metabadge information: " assertion-url))
          (log/error (.getMessage e))
          nil))
      nil)))

(defn metabadge->badge-map [ctx badge-id]
  (some-> (select-received-metabadge-by-badge-id {:id badge-id} (u/get-db ctx)) first))

(defn pending-metabadge?
  "pending-badge->metabadge"
  [ctx pending-assertion id]
  (if-let [is-metabadge? (db/metabadge?! ctx (get-in ctx [:config :factory :url]) (-> pending-assertion (assoc :id id)))]
    (let [badge (metabadge->badge-map ctx id)]
      (db/get-metabadge! ctx (get-in ctx [:config :factory :url]) badge))))

(defn completed-metabadges [ctx user_id]
  (let [metabadges (select-completed-metabadges {:user_id user_id} (u/get-db ctx))]
    (->> metabadges (expand-required-badges-db ctx user_id nil))))

(defn metabadges-in-progress [ctx user_id]
  (let [metabadges (->> (select-all-user-metabadges {:user_id user_id} (u/get-db ctx)) (expand-required-badges-db ctx user_id nil))
        completed-metabadges (completed-metabadges ctx user_id)]
    (remove (fn [m] (some #(= (:metabadge_id %) (:metabadge_id m)) completed-metabadges)) metabadges)))

(defn all-metabadges [ctx user_id]
  {:in_progress (metabadges-in-progress ctx user_id)
   :completed (completed-metabadges ctx user_id)})

(defn is-metabadge? [ctx user_badge_id]
  (let [x (some-> (select-metabadge-info-from-user-badge {:id user_badge_id} (u/get-db ctx)) first)]
    (reduce-kv (fn [r k v]
                 (assoc r k (if (string/blank? v) false true))) {} x)))

(defn is-received-metabadge? [ctx user_id metabadge_id]
  (let [check (select-user-received-metabadge-by-metabadge-id {:metabadge_id metabadge_id :user_id user_id} (u/get-db ctx))]
    (if (empty? check) false true)))

(defn metabadge-helper [ctx user_badge_id user_id metabadge_ids]
  (reduce (fn [r id]
            (conj r
                  (if-let [received-metabadge? (is-received-metabadge? ctx user_id id)]
                    (->> (select-completed-metabadge-by-metabadge-id {:user_id user_id :metabadge_id id} (u/get-db ctx)) (expand-required-badges-db ctx user_id user_badge_id))
                    (->> (select-factory-metabadge {:id id} (u/get-db ctx)) (expand-required-badges-db ctx user_id user_badge_id))))) [] metabadge_ids))

(defn get-metabadge [ctx user_badge_id user_id]
  (if-let [check (empty? (is-metabadge? ctx user_badge_id))]
    {}
    (let [user_id (if (nil? user_id) (some-> (select-user-id-by-user-badge-id {:id user_badge_id} (u/get-db ctx)) first :user_id) user_id)
          milestones (->> (select-completed-metabadge-by-badge-id {:user_id user_id :user_badge_id user_badge_id} (u/get-db ctx)) (expand-required-badges-db ctx user_id nil))
          required-in (->> (select-required-metatabadge-by-badge-id {:id user_badge_id} (u/get-db ctx))
                           (map :metabadge_id)
                           (metabadge-helper ctx user_badge_id user_id)
                           flatten)
          fetch-metabadge! (fn []
                             (when (and (empty? milestones) (empty? required-in))
                               (let [user-badge (->> (select-user-badge-by-id {:id user_badge_id} (u/get-db ctx)) first)]
                                 (do
                                   (log/info "Check for metabadge started!" user-badge)
                                   (db/metabadge?! ctx (get-in ctx [:config :factory :url]) user-badge)
                                   (let [milestones (->> (select-completed-metabadge-by-badge-id {:user_id user_id :user_badge_id user_badge_id} (u/get-db ctx)) (expand-required-badges-db ctx user_id nil))
                                         required-in (->> (select-required-metatabadge-by-badge-id {:id user_badge_id} (u/get-db ctx))
                                                          (map :metabadge_id)
                                                          (metabadge-helper ctx user_badge_id user_id)
                                                          flatten)] {:milestones milestones :required-in required-in})))))]

      (cond
        (or (seq milestones) (seq required-in)) {:milestones milestones :required-in required-in}
        (and (empty? milestones) (empty? required-in)) (fetch-metabadge!)
        :else {}))))
