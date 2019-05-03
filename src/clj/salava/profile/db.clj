(ns salava.profile.db
  (:require [yesql.core :refer [defqueries]]
            [salava.core.util :refer [get-db get-plugins plugin-fun]]
            [salava.core.helper :refer [dump private?]]
            [slingshot.slingshot :refer :all]
            [clojure.tools.logging :as log]
            [clojure.data.json :as json]))

(defqueries "sql/profile/main.sql")

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

(defn profile-blocks [ctx user-id]
  (let [profile-properties (-> (select-user-profile-properties {:user_id user-id} (into {:result-set-fn first :row-fn :value} (get-db ctx)))
                               (json/read-str :key-fn keyword)
                               :blocks)]
    (dump profile-properties)
    profile-properties
    )
  )

(defn user-information-and-profile
  "Get user informatin, profile, public badges and pages"
  [ctx user-id current-user-id]
  (let [user          (user-information ctx user-id)
        user-profile  (user-profile ctx user-id)
        visibility    (if current-user-id "internal" "public")
        blocks (or (profile-blocks ctx user-id) [ {:id 0 :hidden false :block_order 0 :type "badges"}  {:id 1 :hidden false :block_order 1 :type "pages"}])
        ]
    {:user    user
     :profile user-profile
     :visibility visibility
     :blocks blocks
     :owner?  (= user-id current-user-id)}))

(defn user-profile-for-edit
  "Get user profile visibility, profile picture, about text and profile fields for editing"
  [ctx user-id]
  (let [user (user-information ctx user-id)
        user-profile (user-profile ctx user-id)
        picture-files (as-> (first (plugin-fun (get-plugins ctx) "db" "user-image-files")) f (if f (f ctx user-id) []))]
    {:user (select-keys user [:about :profile_picture :profile_visibility])
     :profile user-profile
     :user_id user-id
     :picture_files picture-files
     }))

(defn save-showcase-badges [])

(defn create-showcase-block [])
(defn update-showcase-block [])

(defn process-showcase-blocks [])

(defn save-profile-properties [ctx blocks theme user-id]
  (let [profile-blocks (profile-blocks ctx user-id)
         json-map (json/write-str (hash-map :blocks blocks :theme theme))]

    #_(doseq [block-index (range (count blocks))]
      (let [block (-> (nth blocks block-index)
                      (assoc :block_order block-index))
            id (and (:id block)
                      (some #(and (= (:type %) (:type block)) (= (:id %) (:id block))) profile-blocks))]

        )
      )
    (insert-user-profile-properties! {:value json-map :user_id user-id} (get-db ctx))
    ))

(defn save-user-profile
  "Save user's profile"
  [ctx visibility picture about fields blocks theme user-id]
  (try+
    (if (and (private? ctx) (= "public" visibility))
      (throw+ {:status "error" :user-id user-id :message "trying save page visibilty as public in private mode"}) )
    (delete-user-profile-fields! {:user_id user-id} (get-db ctx))
    (doseq [index (range 0 (count fields))
            :let [{:keys [field value]} (get fields index)]]
      (insert-user-profile-field! {:user_id user-id :field field :value value :field_order index} (get-db ctx)))
    (update-user-visibility-picture-about! {:profile_visibility visibility :profile_picture picture :about about :id user-id} (get-db ctx))
    (save-profile-properties ctx blocks theme user-id)
    {:status "success" :message "profile/Profilesuccesfullyupdated"}
    (catch Object _
      (log/error _)
      {:status "error" :message ""})))
