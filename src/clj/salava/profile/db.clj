(ns salava.profile.db
  (:require [yesql.core :refer [defqueries]]
            [salava.core.util :refer [get-db get-plugins plugin-fun md->html file-from-url-fix url?]]
            [salava.core.helper :refer [dump private?]]
            [slingshot.slingshot :refer :all]
            [clojure.tools.logging :as log]
            [clojure.data.json :as json]
            [clojure.string :refer [blank?]]))

(defqueries "sql/profile/main.sql")

(defn user-badges [ctx user-id]
 (as-> (first (plugin-fun (get-plugins ctx) "main" "user-badges-all")) f (if f (-> (f ctx user-id) :badges) [])))

(defn user-published-badges [ctx user-id]
 (as-> (first (plugin-fun (get-plugins ctx) "db" "public-by-user")) f (if f (-> (f ctx "badges" user-id user-id) :badges) [])))


(defn user-published-pages [ctx user-id]
 (as-> (first (plugin-fun (get-plugins ctx) "db" "public-by-user")) f (if f (-> (f ctx "pages" user-id user-id) :pages) [])))

(defn user-information
  "Get user data by user-id"
  [ctx user-id]
  (let [select-user (select-user {:id user-id} (into {:result-set-fn first} (get-db ctx)))
        private (get-in ctx [:config :core :private] false)
        user (assoc select-user :private private)]
    user))

(defn user-profile
  "Get user profile fields"
  [ctx user-id]
  (select-user-profile-fields {:user_id user-id} (get-db ctx)))

(def default-profile-blocks
 [{:hidden false :block_order 1 :type "badges"}
  {:hidden false :block_order 2 :type "pages"}
  {:hidden false :block_order 0 :type "location"}])

(defn profile-properties [ctx user-id]
 (some-> (select-user-profile-properties {:user_id user-id} (into {:result-set-fn first :row-fn :value} (get-db ctx)))
     (json/read-str :key-fn keyword)))


(defn showcase-blocks [ctx user-id]
  (let [blocks (select-badge-showcase-blocks {:user_id user-id} (get-db ctx))]
   (map #(assoc % :badges (select-showcase-block-content {:block_id (:id %)} (get-db ctx))) blocks)))

(defn profile-blocks [ctx user-id]
 (let [properties (-> (profile-properties ctx user-id) :blocks)
       {:keys [enabled country public]} (as-> (first (plugin-fun (get-plugins ctx) "db" "user-location")) f (f ctx user-id))
       tmp (if (seq properties) properties default-profile-blocks)
       basic-blocks (cond
                     (empty? enabled) (remove #(= "location" (:type %)) tmp)
                     (and enabled (nil? (some #(= "location" (:type %)) tmp))) (conj tmp {:hidden false :block_order (count tmp) :type "location"})
                     :else tmp)
       showcase-blocks (showcase-blocks ctx user-id)
       blocks (vec (concat (remove #(nil? %) basic-blocks) showcase-blocks))]
  (sort-by :block_order blocks)))

(defn user-information-and-profile
  "Get user informatin, profile, public badges and pages"
  [ctx user-id current-user-id]
  (let [user          (user-information ctx user-id)
        user-profile  (user-profile ctx user-id)
        visibility    (if current-user-id "internal" "public")
        blocks (profile-blocks ctx user-id)
        profile-properties (profile-properties ctx user-id)
        tabs (some->> (mapv #(select-page {:id %} (into {:result-set-fn first} (get-db ctx))) (:tabs profile-properties))
                      (filter (fn [t] (seq t))))]

    {:user    user
     :profile user-profile
     :visibility visibility
     :blocks blocks
     :owner?  (= user-id current-user-id)
     :theme (or (-> profile-properties :theme) 0)
     :tabs tabs}))

(defn user-profile-for-edit
  "Get user profile visibility, profile picture, about text and profile fields for editing"
  [ctx user-id]
  (let [user (user-information ctx user-id)
        user-profile (user-profile ctx user-id)
        picture-files (as-> (first (plugin-fun (get-plugins ctx) "db" "user-image-files")) f (if f (f ctx user-id) []))]
    {:user (select-keys user [:about :profile_picture :profile_visibility])
     :profile user-profile
     :user_id user-id
     :picture_files picture-files}))

(defn is-profile-tab?
 "Check if page is a profile tab"
 [ctx user-id page-id]
 (if-let [check (some #(= % page-id) (some-> (profile-properties ctx user-id) :tabs))] true false))


(defn save-showcase-badges [ctx block]
  (let [badges (:badges block)
        user-id (:user_id block)]
    (delete-showcase-badges! {:block_id (:id block)} (get-db ctx))
    (doseq [b badges
            :let [index (.indexOf badges b)
                  {:keys [id visibility]} b]]
     (when-not (= "public" visibility) (as-> (first (plugin-fun (get-plugins ctx) "main" "set-visibility!")) f (f ctx id "public" user-id)))
     (insert-showcase-badges! {:block_id (:id block) :badge_id id :badge_order index} (get-db ctx)))))

(defn update-showcase-block! [ctx block]
  (update-badge-showcase-block! block (get-db ctx))
  (save-showcase-badges ctx block))

(defn create-showcase-block! [ctx block]
  (let [block-id (:generated_key (insert-showcase-block<! block (get-db ctx)))]
    (save-showcase-badges ctx (assoc block :id block-id))))

(defn delete-block! [ctx block]
  (case (:type block)
    "showcase" (do
                 (delete-showcase-block! {:id (:id block)} (get-db ctx))
                 (delete-showcase-badges! {:block_id (:id block)} (get-db ctx)))))

(defn delete-block-multi! [ctx block-ids user-id]
   (delete-showcase-block-multi! {:block_ids block-ids :user_id user-id} (get-db ctx))
   #_(delete-showcase-badges-multi! {:block_ids block-ids :user_id user-id} (get-db ctx)))

(defn publish-profile-tabs
 "Set profile tab page's visibility to public"
 [ctx user-id tabs]
 (doseq [page tabs
         :let [{:keys [id visibility]} page]]
  (when-not (= "public" visibility)
   (as-> (first (plugin-fun (get-plugins ctx) "main" "toggle-visibility!")) f (f ctx id "public" user-id)))))

(defn save-profile-blocks [ctx blocks theme tabs user-id]
  (let [profile-blocks (profile-blocks ctx user-id)
        badge-ids (map :id (user-badges ctx user-id))
        properties (atom [])]
   (doseq [block-index (range (count blocks))]
      (let [block (-> (nth blocks block-index)
                      (assoc :block_order block-index))
            id (and (:id block)
                    (some #(and (= (:type %) (:type block)) (= (:id %) (:id block))) profile-blocks))]

       (case (:type block)
        ("showcase") (when (= (->> (:badges block)
                                   (filter (fn [x] (some #(= (:id x) %) badge-ids)))
                                   count)
                              (count (:badges block)))
                           (if id
                             (update-showcase-block! ctx (assoc block :user_id user-id))
                             (create-showcase-block! ctx (assoc block :user_id user-id))))
        ("badges" "pages" "location")  (swap! properties conj block)
        nil)))
   (publish-profile-tabs ctx user-id tabs)
   (insert-user-profile-properties! {:value (json/write-str (hash-map :blocks @properties
                                                             :theme theme
                                                             :tabs (mapv :id tabs)))
                                     :user_id user-id} (get-db ctx))
   (doseq [old-block profile-blocks]
     (if-not (some #(and (= (:type old-block) (:type %)) (= (:id old-block) (:id %))) blocks)
       (delete-block! ctx old-block)))))

(defn save-user-profile
  "Save user's profile"
  [ctx profile user-id]
 (let [{:keys [profile_visibility profile_picture about fields blocks theme tabs]} profile]
   (try+
     (if (and (private? ctx) (= "public" profile_visibility))
       (throw+ {:status "error" :user-id user-id :message "trying save page visibilty as public in private mode"}))
     (delete-user-profile-fields! {:user_id user-id} (get-db ctx))
     (doseq [index (range 0 (count fields))
             :let [{:keys [field value]} (get fields index)]]
       (insert-user-profile-field! {:user_id user-id :field field :value value :field_order index} (get-db ctx)))
     (update-user-visibility-picture-about! {:profile_visibility profile_visibility :profile_picture profile_picture :about about :id user-id} (get-db ctx))
     (save-profile-blocks ctx blocks theme tabs user-id)
     {:status "success" :message "profile/Profilesuccesfullyupdated"}
     (catch Object _
       (log/error _)
       {:status "error" :message ""}))))

(defn save-user-profile-p [ctx profile user-id]
 (let [{:keys [profile_visibility profile_picture about theme]} profile
       existing-user-info (user-information ctx user-id)
       about (if about about (-> existing-user-info :about))
       profile_visibility (if (clojure.string/blank? profile_visibility) (-> existing-user-info :profile_visibility) profile_visibility)
       profile-picture (cond
                         (url? profile_picture) (file-from-url-fix ctx profile_picture)
                         (re-find #"^data" profile_picture) (file-from-url-fix ctx profile_picture)
                         :else (-> existing-user-info :profile_picture))
       profile-properties (profile-properties ctx user-id)]

   (try+
     (if (and (private? ctx) (= "public" profile_visibility))
       (throw+ {:status "error" :user-id user-id :message "trying save page visibilty as public in private mode"}))
     (update-user-visibility-picture-about! {:profile_visibility profile_visibility :profile_picture profile-picture :about about :id user-id} (get-db ctx))
     (when theme (insert-user-profile-properties! {:value (json/write-str (assoc profile-properties :theme theme))
                                                   :user_id user-id} (get-db ctx)))
     {:status "success" :message "profile/Profilesuccesfullyupdated"}
     (catch Object _
       (log/error _)
       {:status "error" :message ""}))))

(defn add-profile-block! [ctx new-block user-id]
  (try+
    (let [profile-properties (profile-properties ctx user-id)
          existing-blocks (profile-blocks ctx user-id)
          badge-ids (map :id (user-badges ctx user-id))
          blocks (conj (vec existing-blocks) new-block)]
     (doseq [block-index (range (count blocks))]
      (let [block (-> (nth blocks block-index)
                      (assoc :block_order block-index))
            id (and (:id block)
                    (some #(and (= (:type %) (:type block)) (= (:id %) (:id block))) blocks))]
         (case (:type block)
          ("showcase") (when (= (->> (:badges block)
                                     (filter (fn [x] (some #(= x %) badge-ids)))
                                     count)
                                (count (:badges block)))
                         (let [badges (if (map? (last (:badges block)))
                                          (:badges block)
                                          (when (seq (:badges block)) (->> (select-badge-multi {:ids (:badges block)} (get-db ctx)))))]
                           (if (empty? badges)
                             (throw+ "Trying to create showcase without badges or with badges user does not own")
                             (if id
                               (update-showcase-block! ctx (assoc block :badges badges :user_id user-id :format "short"))
                               (create-showcase-block! ctx (assoc block :badges badges :user_id user-id :format "short"))))))

            nil)))
     {:status "success"})
    (catch Object _
      (log/error _)
      {:status "error" :message _})))

(defn add-profile-fields! "add profile fields to user profile" [ctx fields user-id]
 (let [existing-fields (vec (user-profile ctx user-id))]
   (try+
    (doseq [index (range (count existing-fields) (+ (count existing-fields)(count fields)))
              :let [{:keys [field value]} (get (into existing-fields fields) index)]]
        (insert-user-profile-field! {:user_id user-id :field field :value value :field_order index} (get-db ctx)))
    {:status "success"}
    (catch Object _
     (log/error _)
     {:status "error" :message _}))))

(defn add-profile-tabs! "add pages as tabs to profile" [ctx input user-id]
 (let [profile-properties (profile-properties ctx user-id)
       existing-tabs (:tabs profile-properties)
       tabs (if (seq input)(select-page-multi {:tabs input :user_id user-id} (get-db ctx)) [])
       tabs-to-add (->> tabs (remove #(is-profile-tab? ctx user-id (:id %))))]
   (try+
    (when-not (every? true? (map #(= user-id (:user_id %)) tabs-to-add))
      (throw+ {:status "error" :message "user trying to add page they do not own"}))
    (when (seq tabs-to-add)
     (publish-profile-tabs ctx user-id tabs-to-add)
     (insert-user-profile-properties! {:value (json/write-str (assoc profile-properties :tabs (into existing-tabs (mapv :id tabs-to-add))))
                                       :user_id user-id} (get-db ctx)))
    {:status "success"}
    (catch Object _
      (log/error _)
      {:status "error" :message _}))))

(defn delete-profile-blocks! [ctx block-ids user-id]
 (try+
  (delete-block-multi! ctx block-ids user-id)
  {:status "success"}
  (catch Object _
    (log/error _)
    {:status "error" :message _})))

(defn delete-profile-fields! [ctx field_ids user-id]
 (try+
  (when (seq field_ids)
    (delete-user-profile-fields-multi! {:user_id user-id :field_ids field_ids} (get-db ctx)))
  {:status "success"}
  (catch Object _
    (log/error _)
    {:status "error" :message _})))

(defn delete-profile-tabs! [ctx tabs user-id]
 (try+
  (let [profile-properties (profile-properties ctx user-id)
        existing-tabs (-> profile-properties :tabs)
        tabs (if (seq tabs)(select-page-multi {:tabs tabs :user_id user-id} (get-db ctx)) [])
        filtered-tabs (remove (fn [t] (some #(= t  (:id %)) tabs)) existing-tabs)]
    (insert-user-profile-properties! {:value (json/write-str (assoc profile-properties :tabs filtered-tabs))
                                      :user_id user-id} (get-db ctx))
    {:status "success"})
  (catch Object _
    (log/error _)
    {:status "error" :message _})))

(defn profile-metrics [ctx user-id]
 (let [user-profile (user-information-and-profile ctx user-id nil)
       {:keys [user profile tabs blocks]} user-profile
       {:keys [about profile_picture ]} user
       {:keys [enabled country public]} (as-> (first (plugin-fun (get-plugins ctx) "db" "user-location")) f (f ctx user-id))
       complete-profile (and (not (blank? profile_picture)) (not (blank? about)))
       weights {:about 30 :profile-picture 30 :location 10 :default 30}
       enabled-location (not (empty? enabled))
       has-showcase (some #(= "showcase" (:type %)) blocks)]

  {:tips {:profile-picture-tip (blank? profile_picture)
          :aboutme-tip (blank? about)
          :location-tip (not enabled-location)
          :tabs-tip (empty? tabs)
          :showcase-tip (nil? has-showcase)}
   :completion_percentage (cond
                           (and complete-profile enabled-location) (reduce + (-> weights vals))
                           complete-profile (reduce + (-> (select-keys weights [:about :profile-picture :default]) vals))
                           (and enabled-location (every? blank? (vector profile_picture about))) (reduce + (-> (dissoc weights :about :profile-picture) vals))
                           (and enabled-location (or (not (blank? about))  (not (blank? profile_picture)) )) (reduce + 30 (-> (dissoc weights :about :profile-picture) vals))
                           (and (not enabled-location) (or (not (blank? about)) (not (blank? profile_picture)) )) (reduce + 30 (-> (dissoc weights :about :profile-picture :location) vals))
                           :else (:default weights))}))

(defn delete-profile-block-and-properties! [db user-id]
 (let [showcase-blocks (select-badge-showcase-blocks {:user_id user-id} db)]
  (doseq [sb showcase-blocks]
        (delete-showcase-badges! {:block_id (:id sb)} db))
  (delete-showcase-blocks! {:user_id user-id} db)
  (delete-user-profile-properties! {:user_id user-id} db)))
