(ns salava.badgeIssuer.db
 (:require
  [clojure.string :refer [blank?]]
  [clojure.tools.logging :as log]
  [salava.badgeIssuer.util :refer [selfie-id]]
  [salava.core.util :refer [get-db]]
  [slingshot.slingshot :refer :all]
  [yesql.core :refer [defqueries]]))

(defqueries "sql/badgeIssuer/main.sql")

(defn save-selfie-badge [ctx data user-id]
  (try+
    (let [id (if-not (blank? (:id data))
               (:id data)
               (selfie-id))]
      (insert-selfie-badge<! (assoc data :id id :creator_id user-id) (get-db ctx))
      {:status "success" :id id})
    (catch Object _
      (log/error (.getMessage _))
      {:status "error" :message "Error occured when saving badge!" :id "-1"})))


(defn user-selfie-badges [ctx user-id]
  (get-user-selfie-badges {:creator_id user-id} (get-db ctx)))

(defn user-selfie-badge [ctx user-id id]
  (get-selfie-badge {:id id} (get-db ctx)))
