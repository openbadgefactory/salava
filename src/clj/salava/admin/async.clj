(ns salava.admin.async
  (:require [salava.admin.db :as db]))

(defn first-user [ctx user-id]
  (and (= 1 user-id)
       (= 0 (db/admin-count ctx))))

(defn subscribe [ctx]
  {:new-user (fn [data]
               (if (first-user ctx (:user-id data))
                 (db/update-user-to-admin ctx (:user-id data))))})
