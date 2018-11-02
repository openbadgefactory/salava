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

(defn- expand-required-badges [ctx badges]
  (mapv (fn [b]
          (if-let [badge-info (public-badge-info ctx b)]
            (assoc b :badge-info badge-info))) badges))

(defn- process-metabadge [ctx metabadge]
  (let [content (:metabadge metabadge)
        badges (-> content first :required_badges)
        processed-required-badges (expand-required-badges ctx badges)
        processed-milestone-badge (public-badge-info ctx (-> content first :badge))]
    (assoc metabadge :metabadge (conj [] (-> metabadge :metabadge first (assoc :required_badges processed-required-badges)))
                     :milestone_badge processed-milestone-badge)))

(defn- meta-or-required [assertion-url metabadge-assertion]
  (identical? assertion-url metabadge-assertion)
  )

(defn check-metabadge [ctx assertion-url]
  (log/info "check if badge is a metabadge")
  (let [meta-data-url (str (get-in ctx [:config :factory :url]) "/v1/assertion/metabadge/?url=" (u/url-encode assertion-url))]
    (try
      (let [metabadge (fetch-json-data meta-data-url)
            milestone-badge-assertion (-> metabadge :metabadge first :badge :url)
            processed-metabadge (process-metabadge ctx metabadge)
            is-milestone? (= assertion-url milestone-badge-assertion)]
        (assoc processed-metabadge :milestone? is-milestone? :obf-url (get-in ctx [:config :factory :url])))
      (catch Exception _
        (log/error (str "Did not get metabadge information: " assertion-url))
        {}))))

  (defn is-metabadge? [ctx assertion-url]
    (let [metabadge (check-metabadge ctx assertion-url)]
      (not (empty? (:metabadge metabadge)))))

  (defn metabadge [ctx assertion-url]
    )
