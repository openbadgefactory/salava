(ns salava.badge.migrations
  (:require [salava.core.util :refer [get-db]]
            [clojure.tools.logging :as log]
            [yesql.core :refer [defqueries]]))

(defqueries "sql/badge/endorsement.sql")

(defn request-endorsement-table-up [config]
  (create-request-endorsement-table! {} (get-db (:conn config))))

(defn request-endorsement-table-down [config]
  (drop-request-endorsement-table! {} (get-db (:conn config))))
