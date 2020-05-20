(ns salava.badgeIssuer.event
  (:require
    [salava.badgeIssuer.db :as db]))

(defn events [ctx user-id]
  (db/get-selfie-events ctx user-id))
