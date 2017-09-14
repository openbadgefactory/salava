(ns salava.badge.event
  (:require[salava.badge.db :as db]))


(defn owners [ctx data]
  (db/get-owners ctx (:object data)))


(defn events [ctx user_id]
  (db/get-badge-events ctx user_id))
