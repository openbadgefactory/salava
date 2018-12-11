(ns salava.metabadge.metabadge
  (:require [salava.core.http :as http]
            [salava.core.util :as u]
            [clojure.tools.logging :as log]
            [clojure.core.cache :as cache]
            [clojure.string :refer [escape]]
            [slingshot.slingshot :refer :all]
            [pantomime.mime :refer [extension-for-name mime-type-of]]
            [clojure.core.reducers :as r]
            [salava.metabadge.db :as db]
            [clojure.string :as string]
            [salava.badge.main :as b]
            [salava.core.helper :refer [dump]]
            [clojure.set :as s]
            [medley.core :refer [distinct-by]]))

(def no-of-days-in-cache 30)

(defonce metabadge-cache-storage (atom (-> {} (cache/lru-cache-factory :threshold 100) (cache/ttl-cache-factory :ttl (* (* 86400 1000) no-of-days-in-cache)))))
(defonce user-badge-cache-storage (atom (-> {} (cache/lru-cache-factory :threshold 500) (cache/ttl-cache-factory :ttl (* 86400 1000)))))

(defn url? [s]
  (not (clojure.string/blank? (re-find #"^http" (str s)))))

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
              data (-> response (assoc :criteria criteria)) ]
          (if (url? (:image data))
            (-> data (assoc :image (fetch-image ctx (:image data))))
            data)))
      (catch Exception _
        (log/error (str "Did not get required badge information: " id))
        {}))))


(defn get-badge-data [ctx badge]
  (let [id (:id badge)
        key (keyword id)]
    (cache/lookup (swap! metabadge-cache-storage
                         #(if (cache/has? % key)
                            (cache/hit % key)
                            (cache/miss % key (public-badge-info ctx id)))) key)))

(defn- expand-required-badges [ctx badges assertion-url]
  (->> badges
       (r/map (fn [b] (-> b (assoc :badge-info (dissoc (get-badge-data ctx b) :language :alt_language :tags :id)
                              :current (= (:url b) assertion-url)
                              :user_badge (db/user-badge-by-assertion ctx (:url b))))) )
       (r/foldcat)))


(defn- process-metabadge [ctx metabadge assertion-url]
  (->> (:metabadge metabadge)
       (r/map (fn [m] (-> m
                          (assoc :required_badges (expand-required-badges ctx (:required_badges m) assertion-url)
                            :milestone? (= assertion-url (get-in m [:badge :url])))
                          (update-in [:badge] merge (dissoc (get-badge-data ctx (:badge m)) :language :alt_language :tags :id)))))
       (r/foldcat)
       (assoc metabadge :metabadge)))

(defn check-metabadge [ctx assertion-url]
  (let [meta-data-url (str (get-in ctx [:config :factory :url]) "/v1/assertion/metabadge/?url=" (u/url-encode assertion-url))]

    (if-let [check (string/starts-with? assertion-url (get-in ctx [:config :factory :url]))]
      (try
        (if-let [metabadge (fetch-json-data meta-data-url)]
          (let [processed-metabadge (process-metabadge ctx  metabadge assertion-url)]
            (if (empty? (:metabadge processed-metabadge)) nil processed-metabadge))
          nil)
        (catch Exception e
          (log/error (str "Did not get metabadge information: " assertion-url))
          (log/error (.getMessage e))
          nil))
      nil)))

(defn quick-check-metabadge
  "check if badge is metabadge without processing"
  [ctx assertion-url]
  (let [meta-data-url (str (get-in ctx [:config :factory :url]) "/v1/assertion/metabadge/?url=" (u/url-encode assertion-url))]
    (prn meta-data-url)
    (if-let [metabadge (fetch-json-data meta-data-url)]
      (if (empty? metabadge)
        {}
        (r/reduce (fn [r m]
                    (conj r (-> m (assoc :milestone? (= assertion-url (get-in m [:badge :url])))) )) [] (:metabadge metabadge)))

      {})))


(defn milestone? [ctx user-id user_badge_id]
  "check if badge is a milestone badge or a required badge. If both (= required_badge)"
  (let [assertion-url (:assertion_url (b/get-badge ctx user_badge_id user-id))]
    (when-not (clojure.string/blank? assertion-url)
      (if-let [check (string/starts-with? assertion-url (get-in ctx [:config :factory :url]))]
        (let [metabadge (quick-check-metabadge ctx assertion-url)]
          (if-not (empty? metabadge)
            (if (some false? (keys (group-by :milestone? metabadge))) {:required_badge true} {:milestone true})))
        {}))))

(defn get-user-badge-data [ctx user-id user_badge_id]
  (let [key user_badge_id]
    (cache/lookup (swap! user-badge-cache-storage
                         #(if (cache/has? % key)
                            (cache/hit % key)
                            (cache/miss % key (milestone? ctx user-id user_badge_id)))) key)))


#_(defn all-metabadges
  "build metabadges from user badges"
  [ctx user]
  (let [obf-url (get-in ctx [:config :factory :url])
        all-badges (filter (fn [b] (if-not (:assertion_url b) false (-> b :assertion_url (string/starts-with? obf-url)) ))(b/user-badges-all ctx (:id user)))
        metabadges (r/reduce (fn [r m ]
                               (if-let [check (string/starts-with? (:assertion_url m) obf-url)]
                                 (conj r  (check-metabadge ctx (:assertion_url m)))
                                 )) [] all-badges)]

    (reduce-kv
      (fn [r k v] (distinct-by :id (into [] (r/flatten
                                              (conj r  (->> v
                                                            :metabadge
                                                            (map (fn [m]
                                                                   (let [amount_received (count (filter :received (-> m :required_badges)))]
                                                                     (assoc m :completion_status (%completed (-> m :min_required ) amount_received)))))))))))
      []
      metabadges)))


(defn all-metabadges [ctx user]
  (db/all-metabadges ctx (:id user))
  )


