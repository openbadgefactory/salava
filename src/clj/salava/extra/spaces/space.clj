(ns salava.extra.spaces.space
 (:require
  [clojure.tools.logging :as log]
  [yesql.core :refer [defqueries]]
  [salava.extra.spaces.db :as db]
  [salava.extra.spaces.util :as u]
  [slingshot.slingshot :refer :all]
  [salava.core.time :refer [get-date-from-today iso8601-to-unix-time]]
  [salava.core.util :refer [get-db get-db-1 now get-plugins plugin-fun]]))

(defqueries "sql/extra/spaces/main.sql")

(defrecord Space [id uuid name description status visibility logo banner alias valid_until url])

(defn create! [ctx space]
 (try+
   (let [{:keys [name description status visibility logo banner admins alias valid_until css url messages]} space
         valid_until (if-not (clojure.string/blank? valid_until) (iso8601-to-unix-time valid_until)  (get-date-from-today 12 0 0))
         space (->Space nil (u/uuid) name description "active" "private" logo banner alias valid_until url)]
    (db/create-new-space! ctx (assoc space :admins admins :css css :messages messages)) ;:admin admin))
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
    (let [{:keys [id name description status visibility logo banner alias valid_until css messages]} space
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
 (if-let [token (select-space-property {:id space-id :name "token"} (into {:result-set-fn first :row-fn :value} (get-db ctx)))]
   token
   (do
    (insert-space-property! {:space_id space-id :name "token" :value (str (java.util.UUID/randomUUID))} (get-db ctx))
    (space-token ctx space-id))))

(defn invite-link-status [ctx space-id]
 (if-let [x (some-> (select-space-property {:id space-id :name "invite_link"} (into {:result-set-fn first :row-fn :value} (get-db ctx))) (read-string) (pos?))]
   true false))


(defn update-visibility! [ctx space-id visibility admin-id]
 (try+
  (update-space-visibility! {:id space-id :v visibility :user_id admin-id} (get-db ctx))
  #_(when (and (= visibility "private") (clojure.string/blank? (space-token ctx space-id)))
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

(defn- active-space? [ctx id]
  (let [{:keys [valid_until status]} (get-space ctx id)]
    (and (= status "active") (or (nil? valid_until) (> valid_until (now))))))

(defn switch! [ctx ok-status current-user space-id]
  (assoc-in ok-status [:session :identity] (assoc current-user :current-space (if (active-space? ctx space-id) (dissoc (get-space ctx space-id (:id current-user)) :admins) nil))))

(defn reset-switch! [ctx ok-status current-user]
  (assoc-in ok-status [:session :identity] (dissoc current-user :current-space)))

(defn leave!
 ([ctx space-id user-id]
  (try+
   (remove-user-from-space! {:space_id space-id :user_id user-id} (get-db ctx))
   {:status "success"}
   (catch Object _
     (log/error _)
     {:status "error"})))
 ([ctx space-id current-user ok-status]
  (leave! ctx space-id (:id current-user))
  (reset-switch! ctx ok-status current-user)))

(defn join! [ctx space-id user-id]
 (try+
  (db/new-space-member ctx space-id user-id)
  {:status "success"}
  (catch Object _
    (log/error _)
    {:status "error"})))

(defn join-via-invitation! [ctx space-id user-id]
  (try+
   (create-space-member! (db/->Space_member user-id space-id "member" "accepted" 0) (get-db ctx))
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


#_(defn members [ctx space-id]
    (select-space-members-all {:space_id space-id} (get-db ctx)))

(defn members [ctx space-id]
 (let [custom-fields (map :name (get-in ctx [:config :extra/customField :fields]))
       members (select-space-members-all {:space_id space-id} (get-db ctx))]
  (if (seq custom-fields)
    (reduce
      (fn [m v]
       (conj m
        (into {}
         (for [f custom-fields
               :let [val (as-> (first (plugin-fun (get-plugins ctx) "db" "custom-field-value")) $
                               (when $ ($ ctx f (:id v))))]]
           (assoc v f (or val "notset"))))))

      []
      members)
    members)))

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
      (join-via-invitation! ctx space-id user-id)
      ;(join! ctx space-id user-id)
      (switch! ctx ok-status current-user space-id)))))

(defn set-user-space [ctx invitation user-id]
  (let [{:keys [token id alias]} invitation
        already-member? (= user-id (:user_id (is-member? ctx id user-id)))
        d-space (default-space ctx user-id)]
    (if invitation
     (if already-member?
       (when (active-space? ctx id) (get-space ctx id user-id))
       (when (active-space? ctx id)
        (join-via-invitation! ctx id user-id)
        (get-space ctx id user-id)))
     (when (active-space? ctx (:id d-space))
       (get-space ctx (:id d-space) user-id)))))


(defn invite-user [ctx alias invite-token]
  (let [space-id (alias->id ctx alias)
         {:keys [status token]} (invite-link-info ctx space-id)]

     (if (and status (= invite-token token)  (active-space? ctx space-id))
       {:status "success" :id space-id}
       {:status "error" :id space-id :message "extra-spaces/Invitelinkeerror"})))

(defn reset-theme! [ctx id admin-id]
 (try+
   (delete-space-property! {:space_id id :name "css"} (get-db ctx))
   (update-last-modifier! {:id id :admin admin-id} (get-db ctx))
   {:status "success"}
   (catch Object _
     (log/error _)
     {:status "error"})))

#_(defn- extend-time [valid_time]
     (let [extension (get-date-from-today 12 0 0)]
      (if-let [x (and valid_time (> valid_time (now)))]
        (+ valid_time (- extension valid_time))
        extension)))

#_(defn extend-space-validity! [ctx id admin-id]
     (try+
      (let [valid_until (:valid_until (get-space ctx id))
            extension (int (extend-time valid_until))]
        (extend-space-subscription! {:id id :time extension :admin admin-id} (get-db ctx)))
      {:status "success"}
      (catch Object _
       (log/error _)
       {:status "error"})))

(defn extend-space-validity! [ctx id valid_until admin-id]
  (try+
    (extend-space-subscription! {:id id :time valid_until :admin admin-id} (get-db ctx))
    {:status "success"}
    (catch Object _
     (log/error _)
     {:status "error"})))

(defn populate-space [ctx space-id users admin-id]
  (try+
   (doseq [id users]
     (create-space-member! (db/->Space_member id space-id "member" "accepted" 0) (get-db ctx)))
   (update-last-modifier! {:id space-id :admin admin-id} (get-db ctx))
   {:status "success"}
   (catch Object _
     (log/error _)
     {:status "error"})))
