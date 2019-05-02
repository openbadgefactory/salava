(ns salava.profile.main
  (:require [yesql.core :refer [defqueries]]
            [salava.core.util :refer [get-db get-plugins plugin-fun]]))

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
  )

(defn user-information-and-profile
  "Get user informatin, profile, public badges and pages"
  [ctx user-id current-user-id]
  (let [user          (user-information ctx user-id)
        user-profile  (user-profile ctx user-id)
        visibility    (if current-user-id "internal" "public")
        blocks (or (profile-blocks ctx user-id) [ {:hidden "false" :block_order 0 :type "badges"}  {:hidden "false" :block_order 1 :type "pages"}])
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
