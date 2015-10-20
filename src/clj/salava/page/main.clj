(ns salava.page.main
  (:require [yesql.core :refer [defqueries]]
            [salava.core.time :refer [unix-time]]
            [salava.core.i18n :refer [t]]
            [salava.core.helper :refer [dump]]
            [salava.core.util :refer [get-db]]))

(defqueries "sql/page/main.sql")

(defn user-pages-all [ctx user-id]
  "Get all user pages"
  (select-user-pages {:user_id user-id} (get-db ctx)))

(defn create-empty-page! [ctx user-id]
  (:generated_key (insert-empty-page<! {:user_id user-id
                                        :name    (t :page/Untitled)} (get-db ctx))))