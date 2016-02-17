(ns salava.user.db
  (:require [yesql.core :refer [defqueries]]
            [clojure.java.jdbc :as jdbc]
            [clojure.string :refer [trim split]]
            [slingshot.slingshot :refer :all]
            [buddy.hashers :as hashers]
            [salava.gallery.db :as g]
            [salava.core.util :refer [get-db get-datasource]]
            [salava.core.countries :refer [all-countries]]
            [salava.core.i18n :refer [t]]
            [salava.core.time :refer [unix-time]]))

(defqueries "sql/user/main.sql")

(defn hash-password [password]
  (hashers/encrypt password {:alg :pbkdf2+sha256}))

(defn verified-email-addresses
  "Get list of user email addresses by user id"
  [ctx user-id]
  (let [addresses (filter :verified (select-user-email-addresses {:user_id user-id} (get-db ctx)))]
    (map :email addresses)))

(defn primary-email
  "Get user's primary email address"
  [ctx user-id]
  (select-user-primary-email-addresses {:userid user-id} (into {:result-set-fn first :row-fn :email} (get-db ctx))))

(defn email-exists?
  "Check if provided email address exsits"
  [ctx email]
  (select-email-address {:email email} (into {:result-set-fn first :row-fn :user_id} (get-db ctx))))

(defn generate-activation-id []
  (str (java.util.UUID/randomUUID)))

(defn register-user
  "Create new user"
  [ctx email first-name last-name country]
  (if (email-exists? ctx email)
    {:status "error" :message (t :user/Enteredaddressisalready)}
    (let [activation_code (generate-activation-id)
          new-user (insert-user<! {:first_name first-name :last_name last-name :email email :country country :language "fi"} (get-db ctx))
          user-id (:generated_key new-user)]
      (insert-user-email! {:user_id user-id :email email :primary_address 1 :verification_key activation_code} (get-db ctx))
      ;(m/send-verification email)
      {:status "success" :message ""})))

(defn set-password-and-activate
  "Activate user account and set password"
  [ctx user-id code password verify-password]
  (if-not (= password verify-password)
    {:status "error" :message (t :user/Passwordmissmatch)}
    (let [{:keys [verification_key activated]} (select-activation-code-and-status {:id user-id} (into {:result-set-fn first} (get-db ctx)))]
      (if activated
        {:status "error" :message (t :user/Accountalreadyactivated)}
        (if-not (= verification_key code)
          {:status "error" :message (t :user/Invalidactivationcode)}
          (do
            (update-user-password-and-activate! {:pass (hash-password password) :id user-id} (get-db ctx))
            (update-verify-primary-email-address! {:user_id user-id} (get-db ctx))
            {:status "success" :message ""}))))))

(defn login-user
  "Check if user exists and password matches"
  [ctx email plain-password]
  (let [{:keys [id first_name last_name pass activated verified primary_address]} (select-user-by-email-address {:email email} (into {:result-set-fn first} (get-db ctx)))]
    (if (and (hashers/check plain-password pass) id activated verified primary_address)
      {:status "success" :id id :fullname (str first_name " " last_name)}
      {:status "error" :message (t :user/Loginfailed)})))

(defn user-information
  "Get user data by user-id"
  [ctx user-id]
  (select-user {:id user-id} (into {:result-set-fn first} (get-db ctx))))

(defn edit-user
  "Edit user information"
  [ctx user-information user-id]
  (try+
    (let [{:keys [first_name last_name country language current_password new_password new_password_verify]} user-information]
      (when new_password
        (if-not (= new_password new_password_verify)
          (throw+ (t :user/Passwordmissmatch)))
        (let [pass (select-password-by-user-id {:id user-id} (into {:result-set-fn first :row-fn :pass} (get-db ctx)))]
          (if-not (hashers/check current_password pass)
            (throw+ (t :user/Wrongpassword)))))
      (update-user! {:id user-id :first_name (trim first_name) :last_name (trim last_name) :language language :country country} (get-db ctx))
      (if new_password
        (update-password! {:id user-id :pass (hash-password new_password)} (get-db ctx)))
      {:status "success" :message nil})
    (catch Object _
      {:status "error" :message _})))

(defn email-addresses
  "Get all user email addresses"
  [ctx user-id]
  (select-user-email-addresses {:user_id user-id} (get-db ctx)))

(defn add-email-address
  "Add new email address to user accont"
  [ctx email user-id]
  (try+
    (if (email-exists? ctx email)
      {:status "error" :message (t :user/Enteredaddressisalready)}
      (let [verification-key (generate-activation-id)]
        (insert-user-email! {:user_id user-id :email email :primary_address 0 :verification_key verification-key} (get-db ctx))
        ;(m/send-verification email)
        {:status "success" :message (str (t :user/Emailaddress) " " email " " (t :user/added)) :new-email {:email email :verified false :primary_address false :backpack_id nil :ctime (unix-time) :mtime (unix-time)}}))
    (catch Object _
      {:status "error" :message (t :user/Errorwhileaddingemail)})))

(defn delete-email-address
  "Remove email address attached to user account"
  [ctx email user-id]
  (let [{:keys [id primary_address]} (select-user-by-email-address {:email email} (into {:result-set-fn first} (get-db ctx)))]
    (if (and (= id user-id) (not primary_address))
      (do
        (delete-email-address! {:email email :user_id user-id} (get-db ctx))
        {:status "success"})
      {:status "error"})))

(defn set-primary-email-address
  "Set user's primary email address"
  [ctx email user-id]
  (let [{:keys [id primary_address verified]} (select-user-by-email-address {:email email} (into {:result-set-fn first} (get-db ctx)))]
    (if (and (= id user-id) (not primary_address) verified)
      (do
        (jdbc/with-db-transaction
          [tr-cn (get-datasource ctx)]
          (update-primary-email-address! {:email email :user_id user-id} {:connection tr-cn})
          (update-other-addresses! {:email email :user_id user-id} {:connection tr-cn}))
        {:status "success"})
      {:status "error"})))

(defn verify-email-address
  "Verify email address"
  [ctx email verification-code user-id]
  (let [{:keys [id verified verification_key]} (select-user-by-email-address {:email email} (into {:result-set-fn first} (get-db ctx)))]
    (if (and (= id user-id) (not verified) (= verification-code verification_key))
      (do
        (update-verify-email-address! {:email email :user_id user-id} (get-db ctx))
        {:status "success"})
      {:status "error"})))

(defn user-profile
  "Get user profile information"
  [ctx user-id]
  (let [user (select-user {:id user-id} (into {:result-set-fn first} (get-db ctx)))
        user-profile (select-user-profile-fields {:user_id user-id} (get-db ctx))
        recent-badges (g/public-badges-by-user ctx user-id)
        recent-pages (g/public-pages-by-user ctx user-id)]
    {:user user
     :profile user-profile
     :badges recent-badges
     :pages recent-pages}))

(defn set-profile-visibility
  "Set user profile visibility."
  [ctx visibility user-id]
  (update-user-visibility! {:profile_visibility visibility :id user-id} (get-db ctx))
  visibility)