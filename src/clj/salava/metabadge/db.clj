(ns salava.metabadge.db
  (:require [yesql.core :refer [defqueries]]
            [salava.core.util :as u]))

(defqueries "sql/metabadge/queries.sql")

(defn user-badge-by-assertion
  "in case of multiple hits return the most recent"
  [ctx assertion_url]
  (some-> (select-user-badge-by-assertion-url {:assertion_url assertion_url} (u/get-db ctx)) first))

(defn metabadges [ctx]

  )
