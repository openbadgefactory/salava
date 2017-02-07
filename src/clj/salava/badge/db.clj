(ns salava.badge.db
  (:require [yesql.core :refer [defqueries]]
            [clojure.java.jdbc :as jdbc]
            [clojure.set :refer [rename-keys]]
            [clojure.string :refer [blank? split upper-case lower-case capitalize]]
            [clojure.data.json :as json]
            [clojure.string :as string]
            [salava.core.time :refer [unix-time date-from-unix-time]]
            [clojure.tools.logging :as log]
            [clojure.xml :as xml]
            [schema.core :as s]
            [salava.badge.schemas :as schemas] ;cljc
            [digest :as d]
            [bencode.core :refer  [bencode]]
            [buddy.sign.jws :as jws]
            [buddy.core.keys :as keys]
            [salava.core.util :as u])
  (:import [ar.com.hjg.pngj PngReader]))

(defqueries "sql/badge/main.sql")

(defn- content-id [data]
  (d/sha-256 (bencode (assoc data :id ""))))

(defn save-badge-content! [ctx data]
  (s/validate schemas/BadgeContent data)
  (let [id (content-id data)]
    (jdbc/with-db-transaction  [t-con (u/get-db ctx)]
      (insert-badge-content! (assoc data :id id) t-con)
      (doseq [tag (:tags data)]
        (insert-badge-content-tag! {:badge-content-id id :tag tag} t-con))
      (doseq [a (:alignment data)]
        (insert-badge-content-alignment! (assoc a :badge-content-id id) t-con)))
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
