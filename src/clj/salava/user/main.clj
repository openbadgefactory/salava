(ns salava.user.main
  (:require [yesql.core :refer [defqueries]]
            [salava.core.util :refer [get-db]]))

(defqueries "sql/user/main.sql")

(defn user-backpack-emails
  "Get list of user email addresses by user id"
  [ctx user-id]
  (map :email (select-user-email-addresses {:userid user-id} (get-db ctx))))

(defn primary-email
  "Get user's primary email address"
  [ctx user-id]
  (select-user-primary-email-addresses {:userid user-id} (into {:result-set-fn first :row-fn :email} (get-db ctx))))