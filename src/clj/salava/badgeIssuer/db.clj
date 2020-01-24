(ns salava.badgeIssuer.db
 (:require
  [clojure.tools.logging :as log]
  [salava.core.util :refer [get-db]]
  [slingshot.slingshot :refer :all]
  [yesql.core :refer [defqueries]]))

(defqueries "sql/badgeIssuer/main.sql")

(defn save-selfie-badge [ctx data]
  (insert-selfie-badge! data (get-db ctx)))
