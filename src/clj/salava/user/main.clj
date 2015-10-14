(ns salava.user.main
  (:require [yesql.core :refer [defqueries]]
            [salava.core.util :refer [get-db]]))

(defqueries "sql/user/main.sql")

(defn user-backpack-emails
  "Get list of user email addresses by user id"
  [ctx userid]
  (map :email (select-user-email-addresses {:userid userid} (get-db ctx))))