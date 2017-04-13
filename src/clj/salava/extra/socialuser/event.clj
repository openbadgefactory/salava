(ns salava.extra.socialuser.event
  (:require[salava.extra.socialuser.db :as db]))


(defn owners [ctx data]
  (if (= "user" (:type data))
    (db/get-owners ctx (:object data) true)
    (db/get-owners ctx (:subject data) false)))


(defn events [ctx user_id]
  (db/get-user-events ctx user_id))
