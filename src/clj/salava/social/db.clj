(ns salava.social.db
 (:require [yesql.core :refer [defqueries]]
            [clojure.set :refer [rename-keys]]
            [clojure.java.jdbc :as jdbc]
            [salava.core.helper :refer [dump]]
            [slingshot.slingshot :refer :all]
            [salava.core.util :refer [get-db]]
            [salava.core.time :refer [unix-time get-date-from-today]]))

(defqueries "sql/social/queries.sql")

(defn message! [ctx badge_content_id user_id message]
  (try+
   (insert-badge-message<! {:badge_content_id badge_content_id :user_id user_id :message message} (get-db ctx))
   "success"
   (catch Object _
     "error"
     )))

(defn get-badge-messages [ctx badge_content_id]
  (let [badge-messages (select-badge-messages {:badge_content_id badge_content_id} (get-db ctx))]
    badge-messages))
