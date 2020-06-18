(ns salava.extra.spaces.space
 (:require
  [clojure.tools.logging :as log]
  [yesql.core :refer [defqueries]]
  [salava.extra.spaces.db :as db]
  [salava.extra.spaces.util :as u]
  [slingshot.slingshot :refer :all]
  [salava.core.time :refer [get-date-from-today]]
  [salava.core.util :refer [get-db get-db-1]]
  [ring.util.http-response :refer [not-found]]))


(defqueries "sql/extra/spaces/main.sql")

(defrecord Space [id uuid name description status visibility logo banner alias valid_until url])

(defn create! [ctx space]
 (try+
   (let [{:keys [name description status visibility logo banner admins alias valid_until css url]} space
         valid_until (get-date-from-today 12 0 0)
         space (->Space nil (u/uuid) name description "active" "private" logo banner alias valid_until url)]
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

(defn upgrade! [ctx id admin-id]
 (try+
  (upgrade-member-to-admin! {:id id :admin admin-id} (get-db ctx))
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

(defn space-token [ctx space-id]
  (select-space-property {:id space-id :name "token"} (into {:result-set-fn first :row-fn :value} (get-db ctx))))

(defn invite-link-status [ctx space-id]
 (if-let [x (pos? (some-> (select-space-property {:id space-id :name "invite_link"} (into {:result-set-fn first :row-fn :value} (get-db ctx)))
                          (read-string)))] true false))


(defn update-visibility! [ctx space-id visibility admin-id]
 (try+
  (update-space-visibility! {:id space-id :v visibility :user_id admin-id} (get-db ctx))
  (when (and (= visibility "private") (clojure.string/blank? (space-token ctx space-id)))
    (insert-space-property! {:space_id space-id :name "token" :value (str (java.util.UUID/randomUUID))} (get-db ctx)))
  {:status "success"}
  (catch Object _
   (log/error _)
   {:status "error"})))

(defn get-space
 ([ctx id]
  (db/get-space-information ctx id))
 ([ctx id user-id]
  (assoc (get-space ctx id) :role (select-user-space-role {:user_id user-id :space_id id} (into {:result-set-fn first :row-fn :role} (get-db ctx))))))

(defn switch! [ctx ok-status current-user space-id]
  (assoc-in ok-status [:session :identity] (assoc current-user :current-space (dissoc (get-space ctx space-id (:id current-user)) :admins))))

(defn reset-switch! [ctx ok-status current-user]
  (assoc-in ok-status [:session :identity] (dissoc current-user :current-space)))

(defn leave! [ctx space-id user-id]
 (try+
  (remove-user-from-space! {:space_id space-id :user_id user-id} (get-db ctx))
  {:status "success"}
  (catch Object _
    (log/error _)
    {:status "error"})))

(defn join! [ctx space-id user-id]
 (try+
  (db/new-space-member ctx space-id user-id)
  {:status "success"}
  (catch Object _
    (log/error _)
    {:status "error"})))

(defn accept! [ctx space-id user-id]
 (try+
  (update-membership-status! {:status "accepted" :id space-id :user_id user-id} (get-db ctx))
  {:status "success"}
  (catch Object _
    (log/error _)
    {:status "error"})))


(defn members [ctx space-id]
  (select-space-members-all {:space_id space-id} (get-db ctx)))


(defn is-member? [ctx space-id user-id]
  (check-space-member {:id space-id :user_id user-id} (get-db-1 ctx)))

(defn update-link-status [ctx space-id status]
 (try+
  (insert-space-property! {:space_id space-id :name "invite_link" :value status} (get-db ctx))
  {:status "success"}
  (catch Object _
    (log/error _)
    {:status "error"})))

(defn refresh-token [ctx space-id]
 (try+
  (insert-space-property! {:space_id space-id :name "token" :value (str (java.util.UUID/randomUUID))} (get-db ctx))
  {:status "success"}
  (catch Object _
    (log/error _)
    {:status "error"})))

(defn invite-link-info [ctx space-id]
  {:status (invite-link-status ctx space-id)
   :token (space-token ctx space-id)})

(defn alias->id [ctx alias]
  (select-space-by-alias {:alias alias} (into {:result-set-fn first :row-fn :id} (get-db ctx))))

(defn default-space [ctx user-id]
  (select-default-space {:user_id user-id} (get-db-1 ctx)))

(defn set-space-session [ctx space-id ok-status current-user]
  (let [user-id (:id current-user)
        already-member? (= user-id (:user_id (is-member? ctx space-id user-id)))]
    (if already-member?
     (switch! ctx ok-status current-user space-id)
     (do
      (join! ctx space-id user-id)
      (switch! ctx ok-status current-user space-id)))))

(defn set-user-space [ctx invitation user-id]
  (let [{:keys [token id alias]} invitation
        already-member? (= user-id (:user_id (is-member? ctx id user-id)))]
    (if invitation
     (if already-member?
       (get-space ctx id user-id)
       (do
        (join! ctx id user-id)
        (get-space ctx id user-id)))
     (get-space ctx (:id (default-space ctx user-id)) user-id))))


(defn invite-user [ctx alias invite-token]
  (let [space-id (alias->id ctx alias)
         {:keys [status token]} (invite-link-info ctx space-id)]

     (if (and status (= invite-token token))
       {:status "success" :id space-id}
       {:status "error" :id space-id :message "extra-spaces/Invitelinkeerror"})))


(def space [:id :uuid :name :description :logo :banner :status :visibility :ctime :mtime :last-modified-by])
(def user_space [:id :user_id :space_id :role :default_space :ctime :mtime])
(def space_properties [:space_id :name :space]) ;;blocks, config, theme etc..
(def space_admin_pending [:id :space_id :email :ctime])
