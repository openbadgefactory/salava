(ns salava.extra.spaces.space
 (:require [yesql.core :refer [defqueries]]
           [salace.extra.spaces.db :as db]))

(defqueries "sql/spaces/main.sql")

(def test-data [{:uuid ""
                 :name "Hpass"
                 :description ""
                 :status "active"
                 :visibility "open"
                 :logo ""
                 :banner ""
                 :admin ["email"]}
                {:uuid ""
                 :name "Hpass"
                 :status "active"
                 :visibility "open"
                 :logo ""
                 :banner ""
                 :admin ["email"]}])

(defrecord Space [id uuid name description status visibility logo banner])

(defn uuid [] (str (java.util.UUID/randomUUID)))

(defn create! [ctx space-coll]
 (doseq [space space-coll
         :let [{:keys [name description status visibility logo banner admin]} space
               new-space (->Space nil (uuid) name description status visibility logo banner)]]
  (db/create-new-space! ctx (assoc new-space :admin admin))))



(defn delete! [space-id])
(defn edit! [space-id])
(defn suspend! [space-id])
(defn switch [space-id])

(def space [:id :uuid :name :description :logo :banner :status :visibility :ctime :mtime :last-modified-by])
(def user_space [:id :user_id :space_id :role :default_space :ctime :mtime])
(def space_properties [:space_id :name :space]) ;;blocks, config, theme etc..
(def space_admin_pending [:id :space_id :email :ctime])
