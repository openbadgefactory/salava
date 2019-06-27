(ns salava.extra.spaces.space
 (:require [yesql.core :refer [defqueries]]
           [salava.extra.spaces.db :as db]
           [salava.extra.spaces.util :as u]))

(defqueries "sql/extra/spaces/main.sql")

(defrecord Space [id uuid name description status visibility logo banner])

(defn uuid [] (str (java.util.UUID/randomUUID)))

;;validate data with specs?

(defn create! [ctx space]
 (let [{:keys [name uuid description status visibility logo banner admin]} space
       space (->Space nil uuid name description status visibility logo banner)]
  (db/create-new-space! ctx (assoc space :admin admin))))

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


(def space [:id :uuid :name :description :logo :banner :status :visibility :ctime :mtime :last-modified-by])
(def user_space [:id :user_id :space_id :role :default_space :ctime :mtime])
(def space_properties [:space_id :name :space]) ;;blocks, config, theme etc..
(def space_admin_pending [:id :space_id :email :ctime])
