(ns salava.oauth.block
  (:require [salava.oauth.db :as db]))

(defn user-information [ctx user-id]
  (db/get-user-information ctx user-id))
