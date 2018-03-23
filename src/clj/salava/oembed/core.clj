(ns salava.oembed.core
  (:require [clojure.tools.logging :as log]
            [clojure.string :as string]
            [yesql.core :refer [defqueries]]
            [salava.core.util :as u]))

(defqueries "sql/oembed/queries.sql")

(def iframe
  "<iframe frameborder='0' scrolling='no' src='%s/%s/badge/info/%d/embed' width='%d' height='%d'></iframe>")

(defn- url->id [url]
  (when-let [[_ id] (re-find #"/badge/info/([0-9]+)$" url)]
    {:user_badge_id id}))


(defn- badge->response [badge provider w h referrer]
  {:type "rich"
   :version 1.0
   :title (:name badge)
   :provider_name (:name provider)
   :provider_url  (:url provider)
   :referrer referrer
   :width w
   :height h
   :html (format iframe (:url provider) (:path provider) (:id badge) w h)})

(defn badge [ctx url w h referrer]
  (let [provider {:name (get-in ctx [:config :core :site-name])
                  :url (get-in ctx [:config :core :site-url])
                  :path (get-in ctx [:config :core :site-path])}]
    (some-> url
            url->id
            (select-public-badge (u/get-db ctx))
            first
            (badge->response provider w h referrer))))
