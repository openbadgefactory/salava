(ns salava.user.db
  (:require [yesql.core :refer [defqueries]]
            [buddy.hashers :as hashers]
            [salava.core.util :refer [get-db]]
            [salava.core.countries :refer [all-countries]]
            [salava.core.i18n :refer [t]]))

(defqueries "sql/user/main.sql")

(defn user-backpack-emails
  "Get list of user email addresses by user id"
  [ctx user-id]
  (map :email (select-user-email-addresses {:userid user-id} (get-db ctx))))

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
    {:status "error" :message (t :user/Emailaddressinuse)}
    (let [activation_code (generate-activation-id)
          new-user (insert-user<! {:first_name first-name :last_name last-name :email email :country country :language "fi" :activation_code activation_code} (get-db ctx))
          user-id (:generated_key new-user)]
      (insert-user-email! {:user_id user-id :email email :primary_address 1} (get-db ctx))
      ;(m/send-verification email)
      {:status "success" :message ""})))

(defn set-password-and-activate
  "Activate user account and set password"
  [ctx user-id code password verify-password]
  (if-not (= password verify-password)
    {:status "error" :message (t :user/Passwordmissmatch)}
    (let [{:keys [activation_code activated]} (select-activation-code-and-status {:id user-id} (into {:result-set-fn first} (get-db ctx)))]
      (if activated
        {:status "error" :message (t :user/Accountalreadyactivated)}
        (if-not (= activation_code code)
          {:status "error" :message (t :user/Activationcodemissmatch)}
          (do
            (update-user-password-and-activate! {:pass (hashers/encrypt password {:alg :pbkdf2+sha256})
                                                 :id user-id} (get-db ctx))
            {:status "success" :message ""}))))))

(defn login-user
  "Check if user exists and password matches"
  [ctx email plain-password]
  (let [{:keys [id first_name last_name pass activated]} (select-user-by-email-address {:email email} (into {:result-set-fn first} (get-db ctx)))]
    (if (and id
             activated
             (hashers/check plain-password pass))
      {:status "success" :id id :fullname (str first_name " " last_name)}
      {:status "error" :message (t :user/Loginfailed)})))