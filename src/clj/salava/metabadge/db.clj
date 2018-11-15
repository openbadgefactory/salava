(ns salava.metabadge.db
  (:require [yesql.core :refer [defqueries]]
            [salava.core.util :as u]))

(defqueries "sql/metabadge/queries.sql")

(defn badge-id-by-assertion
  "in case of multiple hits return the most recent"
  [ctx assertion_url]
  (some-> (select-user-badge-id-by-assertion-url {:assertion_url assertion_url} (u/get-db ctx)) #_(into {:result-set-fn first :row-fn :id} (u/get-db ctx)) first :id))
