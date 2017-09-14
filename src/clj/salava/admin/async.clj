(ns salava.admin.async
  (:require [salava.admin.db :as db]))



(defn subscribe [ctx]
  {:new-user (fn [data]
               (if (= 1 (:user-id data))
                 (db/update-user-to-admin ctx (:user-id data))) )})
