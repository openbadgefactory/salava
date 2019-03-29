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
  {:success (boolean (update-user-location-public! {:user user-id :pub (if public? 1 0)} (u/get-db ctx)))})

(defn set-user-badge-location [ctx user-id user-badge-id lat lng]
  {:success (boolean (update-user-badge-location! {:user user-id :badge user-badge-id :lat lat :lng lng} (u/get-db ctx)))})


(defn user-badge-location [ctx user-id user-badge-id]
  (if-let [loc (select-user-badge-location {:user user-id :badge user-badge-id} (u/get-db-1 ctx))]
    loc
    {:lat nil :lng nil}))

(defn set-user-location [ctx user-id lat lng]
  {:success (boolean (update-user-location! {:user user-id :lat lat :lng lng} (u/get-db ctx)))})


(defn user-enabled-location [ctx user-id logged-in?]
  (if logged-in?
    (or (select-user-location        {:user user-id} (u/get-db-1 ctx)) {:lat nil :lng nil})
    (or (select-user-location-public {:user user-id} (u/get-db-1 ctx)) {:lat nil :lng nil})))


(defn user-location [ctx user-id]
  (if-let [user (select-user {:user user-id} (u/get-db-1 ctx))]
    {:enabled (select-user-location {:user user-id} (u/get-db-1 ctx))
     :country (get c/lat-lng (keyword (:country user)) {:lat nil :lng nil})
     :public (pos? (:location_public user))}
    {:enabled nil
     :country {:lat nil :lng nil}
     :public false}))


(defn explore-badge [ctx badge-id]
  {:badges (select-explore-badge {:badge badge-id} (u/get-db ctx))})


;;TODO use logged-in status
(defn explore-list [ctx kind logged-in? opt]
  (case kind
    "users"
    {:users (select-explore-users (select-keys opt [:max_lat :max_lng :min_lat :min_lng]) (u/get-db ctx))}
    "badges"
    {:badges (select-explore-badges (select-keys opt [:max_lat :max_lng :min_lat :min_lng]) (u/get-db ctx))}
    {}))
