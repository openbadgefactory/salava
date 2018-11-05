(ns salava.metabadge.db
  (:require [salava.core.http :as http]
            [salava.core.util :as u]
            [clojure.tools.logging :as log]))

(defn- fetch-json-data [url]
  (log/info "fetch-json-data: GET" url)
  (http/http-get url {:as :json :accept :json :throw-entire-message? true}))

(defn- public-badge-info [ctx badge]
  (let [id (:id badge)
        url (str (get-in ctx [:config :factory :url]) "/v1/badge/_/" id ".json")]
    (log/info "fetch-public-badge-info: GET " url)
    (try
      (fetch-json-data url)
      (catch Exception _
        (log/error (str "Did not get badge information: " id))
        {}))))

(defn- expand-required-badges [ctx badges assertion-url]
  (mapv (fn [b]
          (if-let [badge-info (public-badge-info ctx b)]
            (assoc b :badge-info badge-info
                      :current (= (:url b) assertion-url)
              ))) badges))

(defn- process-metabadge [ctx metabadge assertion-url]
  (let [content (:metabadge metabadge)]
    (assoc metabadge :metabadge
      (mapv
        (fn [m]
          (let [required-badges (:required_badges m)
                milestone-badge (:badge m)
                processed-required-badges (expand-required-badges ctx required-badges assertion-url)
                milestone-badge-info (public-badge-info ctx milestone-badge)
                is-milestone? (= assertion-url (:url milestone-badge))]
            (-> m
                (assoc :required_badges processed-required-badges :milestone? is-milestone?)
                (update-in [:badge] merge milestone-badge-info))))
        content)
      )))


(defn check-metabadge [ctx assertion-url]
  (log/info "check if badge is a metabadge")
  (let [meta-data-url (str (get-in ctx [:config :factory :url]) "/v1/assertion/metabadge/?url=" (u/url-encode assertion-url))]
    (try
      (let [metabadge (fetch-json-data meta-data-url)
            processed-metabadge (process-metabadge ctx metabadge assertion-url)]
        (prn processed-metabadge)
        (if (empty? (:metabadge processed-metabadge))
          {}
          (assoc processed-metabadge :obf-url (get-in ctx [:config :factory :url]))))
      (catch Exception _
        (log/error (str "Did not get metabadge information: " assertion-url))
        {}))))

  (defn is-metabadge? [ctx assertion-url]
    (let [metabadge (check-metabadge ctx assertion-url)]
      (not (empty? (:metabadge metabadge)))))

  (defn metabadge [ctx assertion-url]
    )
