(ns salava.profile.db
  (:require [yesql.core :refer [defqueries]]
            [salava.core.util :refer [get-db get-plugins plugin-fun md->html]]
            [salava.core.helper :refer [dump private?]]
            [slingshot.slingshot :refer :all]
            [clojure.tools.logging :as log]
            [clojure.data.json :as json]
            [clojure.string :refer [blank?]]))

(defqueries "sql/profile/main.sql")


(defn user-badges [ctx user-id]
 (as-> (first (plugin-fun (get-plugins ctx) "main" "user-badges-all")) f (if f (f ctx user-id) [])))

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
  (let [profile-properties (-> (profile-properties ctx user-id) :blocks)
        basic-blocks (if (seq profile-properties) profile-properties default-profile-blocks)
        showcase-blocks (showcase-blocks ctx user-id)
        blocks (vec (concat basic-blocks showcase-blocks))]
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
     :theme (-> profile-properties :theme)
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

(defn profile-metrics [ctx user-id]
 (let [user-profile (user-information-and-profile ctx user-id nil)
       {:keys [user profile tabs]} user-profile
       {:keys [about profile_picture ]} user
       {:keys [enabled country public]} (as-> (first (plugin-fun (get-plugins ctx) "db" "user-location")) f (f ctx user-id))
       complete-profile (and (not (blank? profile_picture)) (not (blank? about)))
       weights {:about 50 :profile-picture 50 :location 25}]
  {:tips {:profile-picture-tip (blank? profile_picture)
          :aboutme-tip (blank? about)
          :location-tip (empty?  enabled #_(seq enabled))
          :tabs-tip (empty? tabs)}
   :completion_percentage (cond
                           (and complete-profile (true? enabled)) 100
                           complete-profile (reduce + (vals (select-keys weights [:about :profile-picture])))
                           ;(and (not (blank? about)) (blank? profile_picture) #_(true? enabled)) (+ (:about weights) (:location weights))
                           (and (not (blank? about)) (blank? profile_picture) #_(false? enabled)) (:about weights)
                           ;(and (not (blank? profile_picture)) (blank? about) #_(true? enabled)) (+ (:profile-picture weights) (:location weights))
                           (and (not (blank? profile_picture)) (blank? about) #_(false? enabled)) (:profile-picture weights)
                           :else 5)}))
