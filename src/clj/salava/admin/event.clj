(ns salava.admin.event
  (:require [salava.admin.db :as db]))


(defn owners [ctx data]
  (if (= "admin" (:type data))
    (db/get-owners ctx)))


(defn events [ctx user_id]
  (db/get-admin-events ctx user_id))
