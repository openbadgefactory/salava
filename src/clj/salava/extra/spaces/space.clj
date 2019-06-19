(ns salava.extra.spaces.space
 (:require [yesql.core :refer [defqueries]]
           [salava.extra.spaces.db :as db]
           [salava.extra.spaces.util :as u]))


(defqueries "sql/extra/spaces/main.sql")

(defrecord Space [id uuid name description status visibility logo banner])

(defn uuid [] (str (java.util.UUID/randomUUID)))

(def test-data [{:uuid (uuid)
                 :name "Hpass"
                 :description ""
                 :status "active"
                 :visibility "open"
                 :logo "https://techcrunch.com/wp-content/uploads/2018/07/logo-2.png?w=300"
                 :banner ""
                 :admin ["isaac.ogunlolu@discendum.com", "isaac.ogunlolu+test100000@discendum.com"]}
                {:uuid (uuid)
                 :name "msftembo"
                 :status "active"
                 :visibility "open"
                 :logo "https://brandmark.io/logo-rank/random/pepsi.png"
                 :banner ""
                 :admin ["isaac.ogunlolu@discendum.com", "isaac.ogunlolu+test000@discendum.com"]}])

;;validate data with specs?

(defn create! [ctx space-coll]
 (doseq [space space-coll
         :let [{:keys [name description status visibility logo banner admin]} space
               new-space (->Space nil uuid name description status visibility logo banner)]]
  (db/create-new-space! ctx (assoc new-space :admin admin))))

(defn delete! [space-id])
(defn edit! [space-id])
(defn suspend! [space-id])
(defn switch [space-id])

(def space [:id :uuid :name :description :logo :banner :status :visibility :ctime :mtime :last-modified-by])
(def user_space [:id :user_id :space_id :role :default_space :ctime :mtime])
(def space_properties [:space_id :name :space]) ;;blocks, config, theme etc..
(def space_admin_pending [:id :space_id :email :ctime])
