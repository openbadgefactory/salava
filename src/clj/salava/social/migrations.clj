(ns salava.social.migrations
  (:require [salava.core.util :refer [get-db]]
            [clojure.tools.logging :as log]
            [yesql.core :refer [defqueries]]))

(defqueries "sql/social/queries.sql")

(defn social-edit-column-endorsement-up [config]
  (modify-social-event-table-endorsement! {} {:connection (:conn config)}))

(defn social-edit-column-endorsement-down [config])
