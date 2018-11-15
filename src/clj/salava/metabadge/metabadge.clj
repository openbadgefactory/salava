(ns salava.metabadge.metabadge
  (:require [salava.core.http :as http]
            [salava.core.util :as u]
            [clojure.tools.logging :as log]
            [clojure.core.cache :as cache]
            [clojure.string :refer [escape]]
            [slingshot.slingshot :refer :all]
            [pantomime.mime :refer [extension-for-name mime-type-of]]
            [clojure.core.reducers :as r]
            [salava.metabadge.db :as db]))

(def no-of-days-in-cache 30)

(defonce metabadge-cache-storage (atom (-> {} (cache/lru-cache-factory :threshold 100) (cache/ttl-cache-factory :ttl (* (* 86400 1000) no-of-days-in-cache)))))

(defn url? [s]
  (not (clojure.string/blank? (re-find #"^http" (str s)))))

(defn- file-extension [filename]
  (try+
    (let [file (if (re-find #"https?" (str filename)) (java.net.URL. filename) filename)]
      (-> file mime-type-of))
    (catch Object _
      (throw+ (str "Could not get extension for file: " filename)))))

(defn- fetch-json-data [url]
  (try
    (http/http-get url {:as :json :accept :json :throw-entire-message? true})
    (catch Exception e
      (log/error "could not fetch-json-data: " url)
      (log/error (.getMessage e))
      {})))

(defn fetch-image [ctx url]
  (let [ext (file-extension url)]
    (try
      (if-let [image (u/bytes->base64 (http/http-get url {:as :byte-array :max-redirects 5}))]
        (str "data:" ext ";base64, " image))
      (catch Exception _
        (log/error (str "Could not fetch image: " url))
        url))))

(defn- public-badge-info [ctx id]
  (let [url (str (get-in ctx [:config :factory :url]) "/v1/badge/_/" id ".json?v=2.0")]
    (try
      (if-let [data (fetch-json-data url)]
        (if (url? (:image data))
          (-> data (assoc :image (fetch-image ctx (:image data))))
          data)
        )
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

#_(defn- expand-required-badges [ctx badges assertion-url]
    (r/reduce (fn [result b]
                (if-let [badge-info (get-badge-data ctx b)]
                  (conj result (-> b (assoc :badge-info badge-info
                                       :current (= (:url b) assertion-url)))))) [] badges))

(defn- expand-required-badges [ctx badges assertion-url] ;;experiment with reducers -> improved speed
  (->> badges
       (r/map (fn [b] (-> b (assoc :badge-info (get-badge-data ctx b)
                              :current (= (:url b) assertion-url)
                              :user_badge_id (db/badge-id-by-assertion ctx (:url b))))) )
       (r/foldcat)))


#_(defn- process-metabadge [ctx metabadge assertion-url]
    (let [content (:metabadge metabadge)]
      (assoc metabadge :metabadge
        (r/reduce
          (fn [result m]
            (conj result (-> m
                             (assoc :required_badges (expand-required-badges ctx (:required_badges m) assertion-url) :milestone? (= assertion-url (get-in m [:badge :url])))
                             (update-in [:badge] merge (get-badge-data ctx (:badge m)) ))))
          []
          content))))

(defn- process-metabadge [ctx metabadge assertion-url] ;;experiment with reducers
  (->> (:metabadge metabadge)
       (r/map (fn [m] (-> m
                          (assoc :required_badges (expand-required-badges ctx (:required_badges m) assertion-url)
                            :milestone? (= assertion-url (get-in m [:badge :url])))
                          (update-in [:badge] merge (get-badge-data ctx (:badge m)) ))))
       (r/foldcat)
       (assoc metabadge :metabadge)))

(defn check-metabadge [ctx assertion-url]
  (let [meta-data-url (str (get-in ctx [:config :factory :url]) "/v1/assertion/metabadge/?url=" (u/url-encode assertion-url))]
    (try
      (if-let [metabadge (fetch-json-data meta-data-url)]
        (let [processed-metabadge (process-metabadge ctx  metabadge assertion-url)]
          (if (empty? (:metabadge processed-metabadge)) nil processed-metabadge)))
      (catch Exception e
        (log/error (str "Did not get metabadge information: " assertion-url))
        (log/error (.getMessage e))
        nil))))


