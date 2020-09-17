(ns salava.extra.spaces.db
  (:require [yesql.core :refer [defqueries]]
            [salava.core.util :as u]
            [clojure.java.jdbc :as jdbc]
            [clojure.data.json :as json]
            [salava.extra.spaces.util :refer [save-image!]]
            [clojure.tools.logging :as log]
            [slingshot.slingshot :refer :all]
            [clojure.string :refer [blank?]]
            [salava.profile.db :refer [profile-metrics]]
            [salava.core.helper :refer [string->number]]
            [hiccup.page :refer [html5]]
            [postal.core :refer [send-message]]
            [salava.admin.helper :refer [make-csv]]
            [salava.core.i18n :refer [t]]
            [salava.core.time :refer [date-from-unix-time]]
            [salava.extra.customField.db :refer [custom-field-value]]
            [clojure.core.reducers :as r]))

(defqueries "sql/extra/spaces/main.sql")
(defqueries "sql/extra/spaces/report.sql")
(defqueries "sql/extra/spaces/stats.sql")

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
    :admins (space-admins ctx id)
    :message-tool-enabled (or (some-> (select-space-property {:id id :name "message_enabled"} (into {:result-set-fn first :row-fn :value} (u/get-db ctx))) (string->number) (pos?)) false)))

(defn update-message-tool-setting [ctx id enabled? issuer-coll all-issuers-enabled?]
 (try+
   (insert-space-property! {:space_id id :name "message_enabled" :value enabled?} (u/get-db ctx))
   (clear-enabled-issuers-list! {:space_id id} (u/get-db ctx))
   (insert-space-property! {:space_id id :name "all_issuers_enabled" :value all-issuers-enabled?} (u/get-db ctx))
   (when (seq issuer-coll)
     (doseq [issuer issuer-coll]
       (update-message-issuers-list! {:space_id id :issuer issuer} (u/get-db ctx))))
   {:status "success"}
  (catch Object _
    (log/error _)
    {:status "error"})))

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
   (update-message-tool-setting ctx space_id (get-in space [:messages :messages_enabled] false) (get-in space [:messages :enabled_issuers] []) (get-in space [:messages :all_issuers_enabled] false))

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
    #_(update-message-tool-setting ctx id (get-in space [:messages :messages_enabled] false) (get-in space [:messages :enabled_issuers] []))))

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

(defn get-badges [ctx user-id badge-ids]
  (when (seq badge-ids)
   (select-user-badges-report {:ids badge-ids :user_id user-id} (u/get-db ctx))))

(defn- process-filters [ctx filters]
  (let [{:keys [users badges space-id]} filters
        ;badges+ (if (seq badges) badges (all-gallery-badges {} (u/get-db-col ctx :id)))
        users+  (if (seq users) users (select-user-ids-space-report {:space_id space-id} (u/get-db-col ctx :user_id)))]

    (assoc filters :badges badges :users users+)))

(defn- select-users [ctx user-ids page_count]
  (let [limit 50
        offset (* limit page_count)]
    (select-users-for-report-limit-fix {:ids user-ids :limit limit :offset offset} (u/get-db ctx))))

(defn- user-count [remaining page_count]
 (let [limit 50
       users-left (- remaining (* limit (inc page_count)))]
    (if (pos? users-left)
      users-left
      0)))
 
(defn- map-users-badges-count [ctx users]
  (let [ids (mapv :id users)
        shared_badgecount_col (count-shared-badges {:ids ids} (u/get-db ctx))
        total_badgecount_col  (count-all-user-badges {:ids ids} (u/get-db ctx))]
    (->> users
         (r/map #(assoc % :sharedbadges (or (some (fn [u] (when (= (:id %) (:user_id u)) (:count u))) shared_badgecount_col) 0)))
         (r/map #(assoc % :badgecount (or (some (fn [u] (when (= (:id %) (:user_id u))  (:count u) )) total_badgecount_col) 0)))
         (r/foldcat))))

(defn- map-users-completion% [ctx users]
  (let [ids (mapv :id users)
        coll (->> ids (r/map #(hash-map :user_id % :c (:completion_percentage (profile-metrics ctx %)))) (r/foldcat))]
    (->> users
         (r/map #(assoc % :completionPercentage (some (fn [u] (when (= (:id %) (:user_id u)) (:c u))) coll)))
         (r/foldcat))))

(defn badges-for-report [ctx filters]
  (let [user-ids (:users filters)]
    (->> user-ids
        (r/reduce (fn [r u] (conj r (hash-map :user_id u
                                              :badges (some->> (badge-ids ctx (assoc filters :users [u]))
                                                               (get-badges ctx u))))) []))))

(defn report!
  [ctx filters admin-id]
  (let [enabled-custom-fields (mapv :name (get-in ctx [:config :extra/customField :fields] nil))
        filters (process-filters ctx filters)
        user-ids (user-ids ctx filters)

        users (when (seq user-ids) (select-users ctx user-ids (:page_count filters)))
        users-with-badge-counts (when (seq users) (map-users-badges-count ctx users))
        users-with-completion% (when (seq users-with-badge-counts) (map-users-completion% ctx users-with-badge-counts))

        users-with-customfields (when (seq enabled-custom-fields)
                                     (some->> users-with-completion%
                                              (r/map #(merge % (r/reduce
                                                                  (fn [r field]
                                                                   (assoc r (keyword field) (or (custom-field-value ctx field (:id %)) (t :admin/notset))))
                                                                  {}
                                                                  enabled-custom-fields)))
                                              (r/foldcat)))]

    (if (empty? enabled-custom-fields)
        {:users users-with-completion% :user_count (user-count (count user-ids) (:page_count filters)) :total (count user-ids)}
        {:users users-with-customfields :total (count user-ids) :user_count (user-count (count user-ids) (:page_count filters))})))


(defn report-for-export!
  [ctx filters admin-id]
  (let [enabled-custom-fields (mapv :name (get-in ctx [:config :extra/customField :fields] nil))
        filters (process-filters ctx filters)
        user-ids (user-ids ctx filters)
        users (when (seq user-ids) (select-users-for-report-fix {:ids user-ids} (u/get-db ctx)))
        users-with-badge-counts (when (seq users) (map-users-badges-count ctx users))
        users-with-completion% (when (seq users-with-badge-counts) (map-users-completion% ctx users-with-badge-counts))
        users-with-customfields (when (seq enabled-custom-fields)
                                     (some->> users-with-completion%
                                              (r/map #(merge % (r/reduce
                                                                  (fn [r field]
                                                                   (assoc r (keyword field) (or (custom-field-value ctx field (:id %)) (t :admin/notset))))
                                                                  {}
                                                                  enabled-custom-fields)))
                                              (r/foldcat)))]

    (if (empty? enabled-custom-fields)
        {:users users-with-completion%}
        {:users users-with-customfields})))

(defn export-report [ctx users badges to from id current-user]
  (let [filters {:users (clojure.edn/read-string users)
                 :badges (clojure.edn/read-string badges)
                 :to (clojure.edn/read-string to)
                 :from (clojure.edn/read-string from)
                 :space-id id}
        ul (select-user-language {:id (:id current-user)} (into {:result-set-fn first :row-fn :language} (u/get-db ctx)))
        report (report-for-export! ctx filters (:id current-user))
        enabled-custom-fields (mapv :name (get-in ctx [:config :extra/customField :fields] nil))
        columns (if (seq enabled-custom-fields) (concat [:id :name] (mapv #(keyword %) enabled-custom-fields) [:activated :completionPercentage :badgecount :sharedbadges :joined :emailaddresses]) [:id :name :activated :completionPercentage :badgecount :sharedbadges :joined :emailaddresses])
        headers (map #(t (keyword (str "admin/" (name %))) ul) columns)
        result_users (->> (:users report) (mapv #(assoc % :joined (date-from-unix-time (long (* 1000 (:ctime %)))))))
        rows (mapv #(mapv % columns) result_users)
        report->csvformat  (cons headers rows)]
    (make-csv ctx report->csvformat \,)))

 ;;------ MESSAGE  TOOL ------------;;

(defn enabled-issuers [ctx space-id]
  (select-enabled-issuers-list {:space_id space-id} (u/get-db ctx)))

(defn all-issuers-enabled? [ctx space-id]
  (or (some-> (select-space-property {:id space-id :name "all_issuers_enabled"} (into {:result-set-fn first :row-fn :value} (u/get-db ctx))) (string->number)) 0))

(defn- select-badges [ctx issuers page_count params]
  (let [{:keys [name issuer]} params
        limit 20
        offset (* limit page_count)]
    (select-message-tool-badges-limit {:issuers issuers :limit limit :offset offset :name (str "%" name "%") :issuer (str "%" issuer "%")} (u/get-db ctx))))

(defn- badge-count [remaining page_count]
 (let [limit 20
       badges-left (- remaining (* limit (inc page_count)))]
    (if (pos? badges-left)
      badges-left
      0)))

(defn message-tool-settings [ctx space-id]
 {:messages_enabled (or (some-> (select-space-property {:id space-id :name "message_enabled"} (into {:result-set-fn first :row-fn :value} (u/get-db ctx))) (string->number) (pos?)) false)
  :issuers    (if (pos? (all-issuers-enabled? ctx space-id))
                (mapv #(assoc % :enabled true) (select-issuer-list {} (u/get-db ctx)))
                (some->> (select-issuer-list {} (u/get-db ctx))
                         (mapv #(assoc % :enabled (or (some (fn [i] (= (:issuer_name %) (:issuer_name i))) (enabled-issuers ctx space-id)) false)))))
  :all_issuers_enabled (all-issuers-enabled? ctx space-id)}) ;(or (some-> (select-space-property {:id space-id :name "all_issuers_enabled"} (into {:result-set-fn first :row-fn :value} (u/get-db ctx))) (string->number) (pos?)) false)})

(defn message-tool-badges [ctx space-id page_count params]
  (let [{:keys [name issuer]} params
        issuers (map :issuer_name (filter :enabled (:issuers (message-tool-settings ctx space-id))))
        badges (when (seq issuers) (select-badges ctx issuers page_count params))]
   {:badges badges :badge_count (badge-count (get (select-message-tool-badges-count {:issuers issuers :name (str "%" name "%") :issuer (str "%" issuer "%")} (u/get-db-1 ctx)) :total 0) page_count)
    #_(when (seq issuers)
          (select-message-tool-badges {:issuers issuers} (u/get-db ctx)))}))

(defn badge-earners [ctx ids all?]
  (let [assertions (when (seq ids) (select-assertions-from-galleryids {:ids ids} (u/get-db-col ctx :assertion_url)))]
    (when (seq assertions)
     (if all?
       (select-emails-from-assertions-all {:assertions assertions :expected_count (count ids)} (u/get-db-col ctx :email))
       (select-emails-from-assertions {:assertions assertions} (u/get-db-col ctx :email))))))

(defn send-message-to-earners [ctx message ids space-id message_language user_id all?]
  (let [{:keys [subject content]} message
        emails (badge-earners ctx ids all?)
        space-name (:name (get-space-information ctx space-id))
        badge-names (clojure.string/join ", " (select-gallery-badges {:ids ids} (u/get-db-col ctx :badge_name)))
        mail-host-config (get-in ctx [:config :core :mail-host-config])
        data {:from (get-in ctx [:config :core :mail-sender])
              :subject subject
              :body [{:type "text/plain; charset=utf-8"
                      :content (str  (t :extra-spaces/Messageintro message_language) " " (get-in ctx [:config :core :site-name] "Open badge passport") "\n\n"  content "\n\n" "- " space-name " - \n\n" (t :extra-spaces/Youarereceiving message_language) ":\n\n " badge-names "\n\n")}]}]
      (try+
       (doseq [to emails]
        (log/info "sending message to" to)
        (-> (if (nil? mail-host-config)
              (send-message (assoc data :to to))
              (send-message mail-host-config (assoc data :to to)))
            log/info))
       (log-sent-email-notification-to-db! {:space_id space-id :message (json/write-str {:subject subject :content content} ) :user_id user_id} (u/get-db ctx))
       {:status "success"}
       (catch Object ex
         (log/error ex)
         {:status "error"}))))
