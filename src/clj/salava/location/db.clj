(ns salava.location.db
  (:require [clojure.tools.logging :as log]
            [clojure.string :as string]
            [clojure.pprint :refer [pprint]]
            [yesql.core :refer [defqueries]]
            [salava.core.util :as u]
            [salava.location.country :as c]
            ))

(defqueries "sql/location/queries.sql")

(defn set-location-public [ctx user-id public?]
  )

(defn set-user-badge-location [ctx user-id user-badge-id lat lng]
  {:success (boolean (update-user-badge-location! {:user user-id :badge user-badge-id :lat lat :lng lng} (u/get-db ctx)))})


(defn user-badge-location [ctx user-id user-badge-id]
  (if-let [loc (select-user-badge-location {:user user-id :badge user-badge-id} (u/get-db-1 ctx))]
    loc
    {:lat nil :lng nil}))

(defn set-user-location [ctx user-id lat lng]
  {:success (boolean (update-user-location! {:user user-id :lat lat :lng lng} (u/get-db ctx)))})


(defn user-location [ctx user-id]
  (let [country (select-user-country {:user user-id} (into {:row-fn :country} (u/get-db-1 ctx)))]
    (or
      (select-user-location {:user user-id} (u/get-db-1 ctx))
      (get c/lat-lng (keyword country))
      {:lat nil :lng nil})))

(defn explore-list [ctx user-id]
  {;:users  (select-explore-users  {} (u/get-db ctx))
   :badges (select-explore-badges {} (u/get-db ctx))
   })
