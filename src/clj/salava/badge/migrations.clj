(ns salava.badge.migrations
  (:require [salava.core.util :refer [get-db get-db-col]]
            [yesql.core :refer [defqueries]]))

(defqueries "sql/badge/main.sql")

(defn- existing-issuers [config]
  (select-badge-issuers {} (get-db-col {:connection (:conn config)} :name)))

(defn- new-issuer-trigger-up [config])
