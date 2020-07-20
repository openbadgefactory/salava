(ns salava.extra.spaces.db
  (:require [yesql.core :refer [defqueries]]
            [salava.core.util :as u]
            [clojure.java.jdbc :as jdbc]
            [clojure.data.json :as json]
            [salava.extra.spaces.util :refer [save-image!]]
            [clojure.tools.logging :as log]
            [slingshot.slingshot :refer :all]
            [clojure.string :refer [blank?]]
            [salava.profile.db :refer [profile-metrics]]))

(defqueries "sql/extra/spaces/main.sql")
(defqueries "sql/extra/spaces/report.sql")

(defrecord Space_member [user_id space_id role status default_space])
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

(defn new-space-member [ctx space-id user-id]
 (let [sv (select-space-visibility {:id space-id} (into {:result-set-fn first :row-fn :visibility} (u/get-db ctx)))
       status (if (or (= "open" sv) (= "private" sv)) "accepted" "pending")]
  (create-space-member! (->Space_member user-id space-id "member" status 0) (u/get-db ctx))))

#_(defn create-space-admin!
    "Adds existing user as a space member and sets role to admin
   Otherwise, adds email as pending admin"
    [ctx space-id email]
    (log/info "Creating space admin " email)
    (if-let [user-id (select-email-address {:email email} (into {:result-set-fn first :row-fn :user_id} (u/get-db ctx)))]
     (create-space-member! (->Space_member user-id space-id "admin" 0) (u/get-db ctx))
     (create-pending-space-admin! (->Pending_admin nil space-id email) (u/get-db ctx)))
    (log/info "Space admin " email " created!"))

(defn create-space-admin!
 "Adds existing user as a space member and sets role to admin"
 [ctx space-id user-id]
 (log/info "Creating space admin id: " user-id)
 (create-space-member! (->Space_member user-id space-id "admin" "accepted" 0) (u/get-db ctx))
 (log/info "Space admin " user-id " created!"))

#_(defn add-space-admins [ctx id admins]
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

(defn add-space-admins [ctx id admins]
 (try+
  (doseq [admin admins]
   (if (some #(= admin (:id %)) (space-members ctx id))
     (upgrade-member-to-admin! {:admin admin :id id} (u/get-db ctx))
     (create-space-admin! ctx id admin)))
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

(defn update-message-setting [ctx id message-map]
  (let [{:keys [messages_enabled enabled_issuers]} message-map]
   (when-not (empty? message-map)
      (insert-space-property! {:space_id id :name "message_enabled" :value messages_enabled} (u/get-db ctx))
      (when (seq enabled_issuers)
        (clear-enabled-issuers-list! {:space_id id} (u/get-db ctx))
        (doseq [issuer enabled_issuers]
          (update-message-issuers-list! {:space_id id :issuer issuer} (u/get-db ctx)))))))

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
   (doseq [admin (:admins space)]
            ;:let [email (if (number? $) (select-primary-address {:id $} (into {:result-set-fn first :row-fn :email} (u/get-db ctx))))]]
          (create-space-admin! ctx space_id admin))
   (when (:css space) (save-space-property ctx space_id "css" (:css space)))
   (update-message-setting ctx space_id (:messages space))

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
    (when (:css space) (save-space-property ctx id "css" (:css data)))
    (update-message-setting ctx id (:messages space))))

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

(defn expired-spaces [ctx]
  (select-expired-spaces {} (u/get-db ctx)))

(defn get-user-spaces [ctx user-id]
 (map #(assoc % :css (space-property ctx (:id %) "css")) (select-user-spaces {:id user-id} (u/get-db ctx))))

(defn- space-count [remaining page_count]
  (let [limit 20
        spaces-left (- remaining (* limit (inc page_count)))]
    (if (pos? spaces-left)
      spaces-left
      0)))

(defn get-space-ids [ctx name order]
 (let [filters
       (cond-> []
        (not (clojure.string/blank? name))
        (conj (set (select-gallery-spaces-ids-name {:name (str "%" name "%")} (u/get-db-col ctx :id)))))]
   (when (seq filters)
     (into [] (reduce clojure.set/intersection (first filters) (rest filters))))))

(defn get-gallery-spaces [ctx name order page_count]
 (let [limit 20
       offset (* limit page_count)
       space-ids (get-space-ids ctx name order)
       spaces (if (nil? space-ids)
                  (select-gallery-spaces {:order order :limit limit :offset offset} (u/get-db ctx))
                  (if (empty? space-ids) [] (select-gallery-spaces-filtered {:limit limit :offset offset :order order :space_ids space-ids} (u/get-db ctx))))]
   {:spaces spaces
    :space_count (space-count (if (nil? space-ids)
                                  (select-gallery-spaces-count {} (into {:result-set-fn first :row-fn :total} (u/get-db ctx)))
                                  (count space-ids))
                              offset)}))

(defn set-default-space [ctx space-id user-id]
 (try+
  (reset-default-space-value! {:user_id user-id} (u/get-db ctx))
  (set-default-space! {:space_id space-id :user_id user-id} (u/get-db ctx))
  {:status "success"}
  (catch Object _
    (log/error _)
    {:status "error"})))

(defn user-ids [ctx filters]
 (let [{:keys [badges users to from space-id]} filters
       filters-col
       (cond-> []
        ;(and space-id (pos? space-id))
        ;(conj (set (select-user-ids-space-report {:space_id space-id} (u/get-db-col ctx :user_id))))
        (seq badges)
        (conj (set (select-user-ids-badge {:badge_ids badges :to to :from from :expected_count (count badges) :space_id space-id} (u/get-db-col ctx :user_id))))
        (seq users)
        (conj (set users)))]

   (when (seq filters-col)
     (into [] (reduce clojure.set/intersection (first filters-col) (rest filters-col))))))

(defn badge-ids [ctx filters]

  (let [{:keys [badges users to from]} filters
        filters-col
        (cond-> []
         (seq badges)
         (conj (set badges))
         (seq users)
         (conj (set (select-badge-ids-report {:ids users :to to :from from} (u/get-db-col ctx :gallery_id)))))]
      (when (seq filters-col)
        (into [] (reduce clojure.set/intersection (first filters-col) (rest filters-col))))))

(defn get-badges [ctx badge-ids]
  (when (seq badge-ids)
   (select-user-badges-report {:ids badge-ids} (u/get-db ctx))))

(defn report!
  [ctx filters admin-id]
  (let [user-ids (user-ids ctx filters)
        users (when (seq user-ids) (some->> (select-users-for-report {:ids user-ids} (u/get-db ctx))
                                            (mapv #(assoc % :completion_percentage (:completion_percentage (profile-metrics ctx (:id %)))))))
        users-with-badges (reduce
                            (fn [r user]
                              (conj r
                               (assoc user :badges (some->> (badge-ids ctx (assoc filters :users [(:id user)]))
                                                            (get-badges ctx)))))
                            []
                            users)]
    {:users users-with-badges}))

(defn enabled-issuers [ctx space-id]
  (select-enabled-issuers-list {:space_id space-id} (u/get-db ctx)))

(defn all-issuers [ctx space-id]
 (if (and space-id (pos? space-id))
   (some->> (select-issuer-list {} (u/get-db ctx))
            (mapv #(assoc % :enabled (or (some (fn [i] (= (:issuer_name %) (:issuer_name i))) (enabled-issuers ctx space-id)) false))))

  (select-issuer-list {} (u/get-db ctx))))
