(ns salava.badge.db
  (:require [clojure.tools.logging :as log]
            [yesql.core :refer [defqueries]]
            [clojure.java.jdbc :as jdbc]
            [schema.core :as s]
            [salava.badge.schemas :as schemas] ;cljc
            [salava.core.util :as u]))

(defqueries "sql/badge/main.sql")

(defn- content-id [data]
  (u/map-sha256 (assoc data :id "")))

(defn save-badge-content! [ctx data]
  (s/validate schemas/BadgeContent data)
  (let [id (content-id data)]
    (jdbc/with-db-transaction  [t-con (:connection (u/get-db ctx))]
      (insert-badge-content! (assoc data :id id) {:connection t-con})
      (doseq [tag (:tags data)]
        (insert-badge-content-tag! {:badge-content-id id :tag tag} {:connection t-con}))
      (doseq [a (:alignment data)]
        (insert-badge-content-alignment! (assoc a :badge-content-id id) {:connection t-con})))
    id))

(defn save-criteria-content! [ctx data]
  (s/validate schemas/CriteriaContent data)
  (let [id (content-id data)]
    (insert-criteria-content! (assoc data :id id) (u/get-db ctx))
    id))

(defn save-issuer-content! [ctx data]
  (s/validate schemas/IssuerContent data)
  (let [id (content-id data)]
    (insert-issuer-content! (assoc data :id id) (u/get-db ctx))
    id))

(defn save-creator-content! [ctx data]
  (s/validate schemas/CreatorContent data)
  (let [id (content-id data)]
    (insert-creator-content! (assoc data :id id) (u/get-db ctx))
    id))
