(ns salava.extra.spaces.space
 (:require
  [clojure.tools.logging :as log]
  [yesql.core :refer [defqueries]]
  [salava.extra.spaces.db :as db]
  [salava.extra.spaces.util :as u]
  [slingshot.slingshot :refer :all]
  [salava.core.time :refer [get-date-from-today]]
  [salava.core.util :refer [get-db]]))

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

#_(defn delete!
   "Delete space by space id or uuid.
    ids can be a sequable collection"
   [ctx ids]
   (if (seq ids)
    (doseq [id ids]
     (db/clear-space-data! ctx id))
    (db/clear-space-data! ctx ids)))

(defn delete!
  "Delete space by id, soft delete if space has more than one member"
  [ctx id user-id]
  (try+
    (if-let [check (> (count-space-members {:id id} (into {:result-set-fn first :row-fn :count} (get-db ctx))) 1)]
      (db/soft-delete ctx id user-id)
      (db/clear-space-data! ctx id))
    {:status "success"}
    (catch Object _
      (log/error _)
      {:status "error"})))

(defn edit! [ctx id space user-id]
  (try+
    (let [{:keys [id name description status visibility logo banner alias valid_until css]} space
          data (dissoc space :id)]
      (db/update-space-info ctx id data user-id)
      {:status "success"})
    (catch Object _
      (log/error "error: " _)
      {:status "error"})))

(defn downgrade! [ctx id admin-id]
 (try+
  (downgrade-to-member! {:id id :admin admin-id} (get-db ctx))
  {:status "success"}
  (catch Object _
   (log/error _)
   {:status "error"})))

#_(defn suspend! [ctx space-id admin-id]
    (suspend-space! {:id space-id :user_id admin-id} (get-db ctx)))

(defn update-status! [ctx space-id status admin-id]
 (try+
  (update-space-status! {:id space-id :status status :user_id admin-id} (get-db ctx))
  {:status "success"}
  (catch Object _
   (log/error _)
   {:status "error"})))

(defn switch! [space-id])

(defn leave! [ctx space-id user-id]
 (try+
  (remove-user-from-space! {:space_id space-id :user_id user-id} (get-db ctx))
  {:status "success"}
  (catch Object _
    (log/error _)
    {:status "error"})))

(defn get-space [ctx id]
  (db/get-space-information ctx id))

(def space [:id :uuid :name :description :logo :banner :status :visibility :ctime :mtime :last-modified-by])
(def user_space [:id :user_id :space_id :role :default_space :ctime :mtime])
(def space_properties [:space_id :name :space]) ;;blocks, config, theme etc..
(def space_admin_pending [:id :space_id :email :ctime])
