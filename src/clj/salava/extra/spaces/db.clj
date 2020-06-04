(ns salava.extra.spaces.db
  (:require [yesql.core :refer [defqueries]]
            [salava.core.util :as u]
            [clojure.java.jdbc :as jdbc]
            [clojure.data.json :as json]
            [salava.extra.spaces.util :refer [save-image!]]
            [clojure.tools.logging :as log]
            [slingshot.slingshot :refer :all]
            [clojure.string :refer [blank?]]))

(defqueries "sql/extra/spaces/main.sql")
(defrecord Space_member [id user_id space_id role default_space])
(defrecord Pending_admin [id space_id email])

(defn space-property [ctx id property]
  (when-let [value (select-space-property {:id id :name property} (into {:result-set-fn first :row-fn :value} (u/get-db ctx)))]
    (json/read-str value :key-fn keyword)))

(defn save-space-property [ctx id property value]
  (insert-space-property! {:space_id id :name property :value (json/write-str value)} (u/get-db ctx)))

(defn space-admins [ctx id]
  (select-space-admins {:space_id id} (u/get-db ctx)))

(defn space-members [ctx id]
 (select-space-members {:space_id id} (u/get-db ctx)))

(defn create-space-admin!
  "Adds existing user as a space member and sets role to admin
   Otherwise, adds email as pending admin"
  [ctx space-id email]
  (log/info "Creating space admin " email)
  (if-let [user-id (select-email-address {:email email} (into {:result-set-fn first :row-fn :user_id} (u/get-db ctx)))]
   (create-space-member! (->Space_member nil user-id space-id "admin" 0) (u/get-db ctx))
   (create-pending-space-admin! (->Pending_admin nil space-id email) (u/get-db ctx)))
  (log/info "Space admin " email " created!"))

(defn add-space-admins [ctx id admins]
 (try+
  (doseq [admin admins
          :let [email (if (number? admin) (select-primary-address {:id admin} (into {:result-set-fn first :row-fn :email} (u/get-db ctx))))]]
   (if (some #(= admin (:id %)) (space-members ctx id))
     (upgrade-member-to-admin! {:admin admin :id id} (u/get-db ctx))
     (create-space-admin! ctx id email)))
  {:status "success"}
  (catch Object _
   (log/error _)
   {:status "error"})))

(defn space-exists?
  "check if space name already exists"
  [ctx space]
  (if (empty? (select-space-by-name {:name (:name space)} (u/get-db ctx))) false true))

(defn alias-exists?
  "check if space alias already exists"
  [ctx alias]
  (if (blank? (select-space-by-alias {:alias alias} (into {:result-set-fn first :row-fn :alias} (u/get-db ctx)))) false true))

(defn get-space-information [ctx id]
  (assoc (select-space-by-id {:id id} (into {:result-set-fn first} (u/get-db ctx)))
    :css (space-property ctx id "css")
    :admins (space-admins ctx id)))

(defn create-new-space!
 "Initializes space and creates admins"
 [ctx space]
 ;(try+
 (log/info "Creating space" (:name space))
 (log/info "Space exists? " (space-exists? ctx space))
 (when (empty? (:admins space)) (throw+ "extra-spaces/Adminerror"))
 (when (space-exists? ctx space) (throw+ "extra-spaces/Nameexistserror"))
 (when (alias-exists? ctx (:alias space)) (throw+ "extra-spaces/Aliasexistserror"))
 (jdbc/with-db-transaction [tx (:connection (u/get-db ctx))]
  (let [space_id (-> space
                   (dissoc :id :admins)
                   (assoc :logo (save-image! ctx (:logo space)) :banner (save-image! ctx (:banner space)))
                   (create-space<! {:connection tx})
                   :generated_key)]
   (doseq [$ (:admins space)
            :let [email (if (number? $) (select-primary-address {:id $} (into {:result-set-fn first :row-fn :email} (u/get-db ctx))))]]
          (create-space-admin! ctx space_id email))
   (when (:css space) (save-space-property ctx space_id "css" (:css space)))

   (log/info "Finished creating space!"))))

(defn update-space-info [ctx id space user-id]
  (let [data (assoc space
               :id id :user_id user-id
               :last_modified_by user-id
               :logo (if (and (not (blank? (:logo space))) (re-find #"^data:image" (:logo space)))
                         (save-image! ctx (:logo space))
                         (:logo space))
               :banner (if (and (not (blank? (:banner space))) (re-find #"^data:image" (:banner space)))
                         (save-image! ctx (:banner space))
                         (:banner space)))]
    (update-space-information! data (u/get-db ctx))
    (when (:css space) (save-space-property ctx id "css" (:css data)))))

(defn space-id [ctx id]
 (if (uuid? (java.util.UUID/fromString id)) (some-> (select-space-by-uuid {:uuid id} (u/get-db ctx)) :id) id))

(defn clear-space-data!
  "Clear out space information"
  [ctx id]
  ;(let [id (space-id ctx id)]
  (jdbc/with-db-transaction [tx (:connection (u/get-db ctx))]
                            (delete-space! {:id id} {:connection tx})
                            (delete-space-members! {:space_id id} {:connection tx})
                            (delete-space-properties! {:space_id id} {:connection tx})))

(defn soft-delete [ctx id user-id]
  (soft-delete-space! {:id id :user_id user-id} (u/get-db ctx)))

(defn all-spaces [ctx]
 (mapv #(assoc % :member_count (count-space-members {:id (:id %)} (into {:result-set-fn first :row-fn :count} (u/get-db ctx))))(select-all-spaces {} (u/get-db ctx))))

(defn active-spaces [ctx]
  (select-all-active-spaces {} (u/get-db ctx)))

(defn suspended-spaces [ctx])

(defn deleted-spaces [ctx]
  (select-deleted-spaces {} (u/get-db ctx))) 

(defn get-user-spaces [ctx user-id]
  (select-user-spaces {:id user-id} (u/get-db ctx)))
