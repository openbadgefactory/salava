(ns salava.extra.spaces.space
 (:require
  [clojure.tools.logging :as log]
  [yesql.core :refer [defqueries]]
  [salava.extra.spaces.db :as db]
  [salava.extra.spaces.util :as u]
  [slingshot.slingshot :refer :all]
  [salava.core.time :refer [get-date-from-today]]))

(defqueries "sql/extra/spaces/main.sql")

(defrecord Space [id uuid name description status visibility logo banner alias valid_until])

(defn create! [ctx space]
 (try+
   (let [{:keys [name description status visibility logo banner admins alias valid_until css]} space
         valid_until (get-date-from-today 12 0 0)
         space (->Space nil (u/uuid) name description "active" "public" logo banner alias valid_until)]
    (db/create-new-space! ctx (assoc space :admins admins :css css)) ;:admin admin))
    {:status "success"})
   (catch Object _
     (log/error "error: " _) ;(.getMessage _))
     {:status "error" :message _})))

(defn delete!
 "Delete space by space id or uuid.
  ids can be a sequable collection"
 [ctx ids]
 (if (seq ids)
  (doseq [id ids]
   (db/clear-space-data! ctx id))
  (db/clear-space-data! ctx ids)))

(defn edit! [space-id])
(defn suspend! [ctx space-id])
(defn switch [space-id])
(defn get-space [ctx id]
  (db/get-space-information ctx id))


(def space [:id :uuid :name :description :logo :banner :status :visibility :ctime :mtime :last-modified-by])
(def user_space [:id :user_id :space_id :role :default_space :ctime :mtime])
(def space_properties [:space_id :name :space]) ;;blocks, config, theme etc..
(def space_admin_pending [:id :space_id :email :ctime])
