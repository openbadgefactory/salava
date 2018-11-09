(ns salava.metabadge.metabadge
  (:require [salava.core.http :as http]
            [salava.core.util :as u]
            [clojure.tools.logging :as log]
            [clojure.core.cache :as cache]
            [clojure.string :refer [escape]]
            [slingshot.slingshot :refer :all]
            [pantomime.mime :refer [extension-for-name mime-type-of]]))

(defonce metabadge-cache-storage (atom (-> {} (cache/lru-cache-factory :threshold 100) (cache/ttl-cache-factory :ttl (* 3600 1000)))))

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

(defn- public-badge-info [ctx badge]
  (let [id (:id badge)
        url (str (get-in ctx [:config :factory :url]) "/v1/badge/_/" id ".json")]
    (try
      (fetch-json-data url)
      (catch Exception _
        (log/error (str "Did not get required badge information: " id))
        {}))))

(defn fetch-image [ctx url]
  (let [ext (file-extension url) #_(escape (file-extension url) {\. ""})]
    (try
      (if-let [image (u/bytes->base64 (http/http-get url {:as :byte-array :max-redirects 5}))]
        (str "data:" ext ";base64, " image))
      (catch Exception _
        (log/error (str "Could not fetch image: " url))
        url))))

(defn- expand-required-badges [ctx badges assertion-url]
  (mapv (fn [b]
          (if-let [badge-info (public-badge-info ctx b)]
              (-> b
                  (assoc :badge-info badge-info
                    :current (= (:url b) assertion-url))
                  (assoc-in [:badge-info :image] (fetch-image ctx (:image badge-info)))
                  ))) badges))

(defn- process-metabadge [ctx metabadge assertion-url]
  (let [content (:metabadge metabadge)]
    (assoc metabadge :metabadge
      (mapv
        (fn [m]
          (let [required-badges (:required_badges m)
                milestone-badge (:badge m)
                processed-required-badges (expand-required-badges ctx required-badges assertion-url)
                milestone-badge-info (if-let [info (public-badge-info ctx milestone-badge)]
                                       ;(let [image (or (fetch-image ctx (:image info) (:image info)))]
                                         (assoc info :image (fetch-image ctx (:image info))))
                ;milestone-badge-image (or (fetch-image ctx (:image milestone-badge-info) (:image milestone-badge-info)))
                is-milestone? (= assertion-url (:url milestone-badge))]
            (-> m
                (assoc :required_badges processed-required-badges :milestone? is-milestone?)
                (update-in [:badge] merge milestone-badge-info ))))
        content))))

(defn check-metabadge [ctx assertion-url]
  (let [meta-data-url (str (get-in ctx [:config :factory :url]) "/v1/assertion/metabadge/?url=" (u/url-encode assertion-url))]
    (try
      (if-let [metabadge (fetch-json-data meta-data-url)]
        (let [processed-metabadge (process-metabadge ctx  metabadge assertion-url)]
          (if (empty? (:metabadge processed-metabadge)) {} (assoc processed-metabadge :obf-url (get-in ctx [:config :factory :url])))))
      (catch Exception e
        (log/error (str "Did not get metabadge information: " assertion-url))
        (log/error (.getMessage e))
        {}))))

(defn get-data [ctx assertion-url]
  (let [key (keyword assertion-url)]
    (cache/lookup (swap! metabadge-cache-storage
                         #(if (cache/has? % key)
                            (cache/hit % key)
                            (cache/miss % key (check-metabadge ctx assertion-url)))) key)))
