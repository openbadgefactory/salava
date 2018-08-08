(ns salava.user.db
  (:require [yesql.core :refer [defqueries]]
            [clojure.java.jdbc :as jdbc]
            [clojure.string :refer [trim split]]
            [slingshot.slingshot :refer :all]
            [salava.core.helper :refer [dump private?]]
            [buddy.hashers :as hashers]
            [salava.gallery.db :as g]
            [salava.file.db :as f]
            [salava.page.main :as p]
            [salava.badge.main :as b]
            [salava.oauth.db :as o]
            [salava.core.util :as u :refer [get-db get-datasource get-site-url get-base-path get-site-name get-plugins plugin-fun get-email-notifications]]
            [salava.core.countries :refer [all-countries]]
            [salava.core.i18n :refer [t]]
            [salava.core.time :refer [unix-time]]
            [salava.mail.mail :as m]))

(defqueries "sql/user/main.sql")

(defn generate-activation-id []
  (str (java.util.UUID/randomUUID)))

(defn activation-link
  ([site-url base-path user-id code]
   (str site-url base-path "/user/activate/" user-id "/" (unix-time) "/" code))
  ([site-url base-path user-id code lng]
   (str site-url base-path "/user/activate/" user-id "/" (unix-time) "/" code "/" lng)))

(defn login-link [site-url base-path]
  (str site-url base-path "/user/login"))

(defn email-verification-link [site-url base-path verification-key]
  (str site-url base-path "/user/verify_email/" verification-key))

(defn hash-password [password]
  (hashers/encrypt password {:alg :pbkdf2+sha256}))

(defn check-password [ctx password hash]
  (let [funs (plugin-fun (get-plugins ctx) "password" "check-password")]
    (some (fn [f] (try (f password hash) (catch Throwable _ false))) funs)))

(defn verified-email-addresses
  "Get list of user's verified email addresses by user id"
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





(defn register-user
  "Create new user"
  [ctx email first-name last-name country language password password-verify]
  (if (email-exists? ctx email)
    {:status "error" :message "user/Enteredaddressisalready"}
    (if-not (= password password-verify)
      {:status "error" :message "user/Passwordmissmatch"}
      (let [site-url            (get-site-url ctx)
            base-path           (get-base-path ctx)
            activation_code     (generate-activation-id)
            email_notifications (get-email-notifications ctx)
            new-user            (insert-user<! {:first_name first-name :last_name last-name :email email :country country :language language :email_notifications email_notifications :pass (hash-password password)} (get-db ctx))
            user-id             (:generated_key new-user)]
        (insert-user-email! {:user_id user-id :email email :primary_address 1 :verification_key activation_code} (get-db ctx))
        (u/publish ctx :new-user {:user-id user-id})
        {:status "success" :message ""}))))

(defn set-password-and-activate
  "Activate user account and set password. Do not change password if user account is already activated and verification key is older than a day. If account is not yet activated, verify primary email address."
  [ctx user-id code password verify-password]
  (if-not (= password verify-password)
    {:status "error" :message "user/Passwordmissmatch"}
    (let [{:keys [verification_key activated mtime, email, primary_address]} (select-activation-code-and-status {:id user-id} (into {:result-set-fn first} (get-db ctx)))]
      (if-not (= verification_key code)
        {:status "error" :message "user/Invalidactivationcode"}
        (if (and activated (< mtime (- (unix-time) 86400)))
          {:status "error" :message "user/Passwordresetlinkexpired"}
          (do
            (if (or (true? primary_address) (= primary_address 1))
              (do
                (update-user-password-and-activate! {:pass (hash-password password) :id user-id} (get-db ctx))
                (update-set-primary-email-address-verification-key-null! {:user_id user-id} (get-db ctx))
                (update-verify-primary-email-address! {:user_id user-id} (get-db ctx)))
              (do
                (update-password! {:pass (hash-password password) :id user-id} (get-db ctx))
                (update-verify-email-address! {:user_id user-id :email email} (get-db ctx)))
              )
            {:status "success" :message (if activated "user/Accountpasswordchangedsuccessfully" "user/Accountactivatedsuccessfully")}))))))

(defn get-user-by-email [ctx email]
  (select-user-by-email-address {:email email} (into {:result-set-fn first} (get-db ctx))))

(defn accepted-terms? [ctx email]
  (select-user-terms {:email email} (into {:result-set-fn first} (get-db ctx))))

(defn get-accepted-terms-by-id [ctx user-id]
  (select-user-terms-with-userid {:user_id user-id} (into {:result-set-fn first} (get-db ctx))))

(defn insert-user-terms [ctx user-id status]
  (try+
    (do
      (insert-user-terms<! {:user_id user-id :status status} (get-db ctx))
      {:status "success" :input status})
    (catch Object _
      {:status "error" :input status})
    ))

(defn login-user
  "Check if user exists and password matches. User account must be activated."
  [ctx email plain-password]
  (try+
    (let [{:keys [id pass activated verified primary_address role deleted]} (select-user-by-email-address {:email email} (into {:result-set-fn first} (get-db ctx)))]
      (if (and id pass (not deleted) (check-password ctx plain-password pass)) ;activated verified
        (do
          (update-user-last_login! {:id id} (get-db ctx))
          {:status "success" :id id})
        (if (and id pass deleted (check-password ctx plain-password pass))
          {:status "error" :message "user/Accountdeleted"}
          {:status "error" :message "user/Loginfailed"})))
    (catch Object _
      {:status "error" :message "user/Loginfailed"})))


(defn edit-user-password
  "Edit user information."
  [ctx user-information user-id]
  (try+
    (let [{:keys [current_password new_password new_password_verify]} user-information]
      (when new_password
        (if (not (= new_password new_password_verify))
          (throw+ "user/Passwordmissmatch"))
        (let [pass (select-password-by-user-id {:id user-id} (into {:result-set-fn first :row-fn :pass} (get-db ctx)))]
          (if (and pass (not (check-password ctx current_password pass)))
            (throw+ "user/Wrongpassword"))))
      (update-password! {:id user-id :pass (hash-password new_password)} (get-db ctx))

      {:status "success" :message "core/Thechangeshavebeensaved" })
    (catch Object _
      {:status "error" :message _})))

(defn edit-user
  "Edit user information."
  [ctx user-information user-id]
  (try+
    (let [{:keys [first_name last_name country language email_notifications]} user-information]
      (update-user! {:id user-id :first_name (trim first_name) :last_name (trim last_name) :language language :country country :email_notifications email_notifications} (get-db ctx))
      {:status "success" :message "core/Thechangeshavebeensaved" })
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
      {:status "error" :message "user/Enteredaddressisalready"}
      (let [site-url (get-site-url ctx)
            base-path (get-base-path ctx)
            verification-key (generate-activation-id)
            {:keys [first_name last_name language]} (select-user {:id user-id} (into {:result-set-fn first} (get-db ctx)))]
        (insert-user-email! {:user_id user-id :email email :primary_address 0 :verification_key verification-key} (get-db ctx))
        (m/send-verification ctx site-url (email-verification-link site-url base-path verification-key) (str first_name " " last_name) email language)
        {:status "success" :message (str (t :user/Emailaddress language) " " email " " (t :user/added language)) :new-email {:email email :verified false :primary_address false :backpack_id nil :ctime (unix-time) :mtime (unix-time)}}))
    (catch Object _
      {:status "error" :message "user/Errorwhileaddingemail"})))

(defn send-email-verified-link
  "Send verififed link to address"
  [ctx email user-id]
  (try+
    (if (:verified (email-exists? ctx email))
      (throw+ {:status "error" :message "user/Emailisalreadyverified"}))
    (let [site-url (get-site-url ctx)
          base-path (get-base-path ctx)
          verification-key (generate-activation-id)
          {:keys [first_name last_name language]} (select-user {:id user-id} (into {:result-set-fn first} (get-db ctx)))]
      (update-email-address-verification-key! { :email email :verification_key verification-key} (get-db ctx))
      (m/send-verification ctx site-url (email-verification-link site-url base-path verification-key) (str first_name " " last_name) email language)
      {:status "success" :message (str (t :user/Emailaddress language) " " email " " (t :user/added language)) :new-email {:email email :verified false :primary_address false :backpack_id nil :ctime (unix-time) :mtime (unix-time)}})
    (catch Object _
      {:status "error" :message "user/Errorwhileaddingemail"})))

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
  [ctx key user-id activated]
  (let [{:keys [user_id email verified verification_key mtime]} (select-email-by-verification-key {:verification_key key :user_id user-id} (into {:result-set-fn first} (get-db ctx)))
        verified-emails (verified-email-addresses ctx user-id)]
    (try+
      (if-not (and email (= user_id user-id) (not verified) (= key verification_key) (>= mtime (- (unix-time) (* 5 24 60 60))))
        (throw+ {:status "error"}))
      (if-not activated
        (update-user-activate! {:id user_id} (get-db ctx)))
      (update-verify-email-address! {:email email :user_id user_id} (get-db ctx))
      (if (= 0 (count verified-emails))
        (set-primary-email-address ctx email user-id))
      "success"
      (catch Object _
        "error"))))

(defn user-information
  "Get user data by user-id"
  [ctx user-id]
  (let [select-user (select-user {:id user-id} (into {:result-set-fn first} (get-db ctx)))
        private (get-in ctx [:config :core :private] false)
        user (assoc select-user :private private)]
    user))

(defn user-information-with-registered-and-last-login
  "Get user data by user-id "
  [ctx user-id]
  (select-user-with-register-last-login {:id user-id} (into {:result-set-fn first} (get-db ctx))))

(defn user-profile
  "Get user profile fields"
  [ctx user-id]
  (select-user-profile-fields {:user_id user-id} (get-db ctx)))

(defn has-password? [ctx user-id]
  (let [pass (select-user-password {:id user-id} (into {:result-set-fn first :row-fn :pass} (get-db ctx)))]
    (not (empty? pass))))

(defn user-information-and-profile
  "Get user informatin, profile, public badges and pages"
  [ctx user-id current-user-id]
  (let [user          (user-information ctx user-id)
        user-profile  (user-profile ctx user-id)
        visibility    (if current-user-id "internal" "public")
        recent-badges (g/public-badges-by-user ctx user-id visibility)
        recent-pages  (g/public-pages-by-user ctx user-id visibility)]
    {:user    user
     :profile user-profile
     :badges  recent-badges
     :pages   recent-pages
     :owner?  (= user-id current-user-id)}))

(defn user-profile-for-edit
  "Get user profile visibility, profile picture, about text and profile fields for editing"
  [ctx user-id]
  (let [user (user-information ctx user-id)
        user-profile (user-profile ctx user-id)]
    {:user (select-keys user [:about :profile_picture :profile_visibility])
     :profile user-profile
     :user_id user-id
     :picture_files (f/user-image-files ctx user-id)}))


(defn set-profile-visibility
  "Set user profile visibility."
  [ctx visibility user-id]
  (try+
    (if (and (private? ctx) (= "public" visibility))
      (throw+ {:status "error" :user-id user-id :message "trying save page visibilty as public in private mode"}) )
    (update-user-visibility! {:profile_visibility visibility :id user-id} (get-db ctx))
    visibility
    (catch Object _
      "error")))

(defn save-user-profile
  "Save user's profile"
  [ctx visibility picture about fields user-id]
  (try+
    (if (and (private? ctx) (= "public" visibility))
      (throw+ {:status "error" :user-id user-id :message "trying save page visibilty as public in private mode"}) )
    (delete-user-profile-fields! {:user_id user-id} (get-db ctx))
    (doseq [index (range 0 (count fields))
            :let [{:keys [field value]} (get fields index)]]
      (insert-user-profile-field! {:user_id user-id :field field :value value :field_order index} (get-db ctx)))
    (update-user-visibility-picture-about! {:profile_visibility visibility :profile_picture picture :about about :id user-id} (get-db ctx))
    (catch Object _
      "error")))


(defn send-password-reset-link
  ""
  [ctx email]
  (let [site-url (get-site-url ctx)
        base-path (get-base-path ctx)
        {:keys [id first_name last_name verified primary_address language]} (select-user-by-email-address {:email email} (into {:result-set-fn first} (get-db ctx)))
        verification-key (generate-activation-id)]
    (if id ;(and id verified)
      (do
        (update-verified-email-address-verification-key! {:verification_key verification-key :email email} (get-db ctx))
        (m/send-password-reset-message ctx site-url (activation-link site-url base-path id verification-key language) (str first_name " " last_name) email language)
        {:status "success"})
      {:status "error"})))

(defn set-email-backpack-id
  "Associate Mozilla backpack-id to email address"
  [ctx user-id email backpack-id]
  (update-email-backpack-id! {:user_id user-id :email email :backpack_id backpack-id} (get-db ctx)))

(defn remove-user-files [ctx db user-id]
  (let [user-files (select-user-files-id-path {:user_id user-id} db)]
    (doseq [file user-files
            :let [{:keys [id path]} file]]
      (f/remove-file-with-db! db id)
      (f/remove-file! ctx path))))

(defn remove-user-badges [db user-id]
  (let [user-badge-ids (select-user-badge-ids {:user_id user-id} (into {:row-fn :id} db))]
    (doseq [user-badge-id user-badge-ids]
      (b/delete-badge-with-db! db user-badge-id))))



(defn delete-user
  "Delete user and all user data"
  ([ctx user-id plain-password]
   (delete-user ctx user-id plain-password false))
  ([ctx user-id plain-password admin?]
   (try+
     (let [{:keys [id pass activated]} (select-user-by-id {:id user-id} (into {:result-set-fn first} (get-db ctx)))
           emails                      (email-addresses ctx user-id)]
       (if-not (and (or admin? (check-password ctx plain-password pass)) id)
         (throw+ "Invalid password"))
       (jdbc/with-db-transaction
         [tr-cn (get-datasource ctx)]
         (remove-user-files ctx {:connection tr-cn} user-id)
         (remove-user-badges {:connection tr-cn} user-id)
         (delete-user-badge-views! {:user_id user-id} {:connection tr-cn})
         (delete-user-badge-congratulations! {:user_id user-id} {:connection tr-cn})
         (update-user-badge-messages-set-removed! {:user_id user-id} {:connection tr-cn}) ;set badge messages as removed
         (delete-user-badge-message-views! {:user_id user-id} {:connection tr-cn}) ;remove badge message views
         (delete-user-pending-badges! {:user_id user-id} {:connection tr-cn}) ;delete pending badges
         (delete-user-badge! {:user_id user-id} {:connection tr-cn}) ;remove user badge completely
         (delete-social-connections-user-following! {:owner_id user-id} {:connection tr-cn} );remove social-connections-user
         (delete-user-social-events! {:owner user-id} {:connection tr-cn} );remove users social events
         (delete-all-user-events! {:subject user-id} {:connection tr-cn}) ;remove all user events
         (delete-social-connections-badge! {:user_id user-id} {:connection tr-cn} );remove social-connections-badge
         (delete-user-pages-all! {:user_id user-id} {:connection tr-cn});remove user pages with blocks
         #_(update-user-pages-set-deleted! {:user_id user-id} {:connection tr-cn})
         (delete-user-profile! {:user_id user-id} {:connection tr-cn})

         #_(if activated
             (doall (map #(update-user-email-set-deleted! {:user_id user-id :email (:email %) :deletedemail (str "deleted-" (:email %) ".so.deleted")} {:connection tr-cn} ) emails))
             (delete-email-addresses! {:user_id user-id} {:connection tr-cn}))

         (delete-email-addresses! {:user_id user-id} {:connection tr-cn});delete user email-addresses anyway

         (if (some #(= % :oauth) (get-in ctx [:config :core :plugins]))
           (o/remove-oauth-user-all-services ctx user-id))

         #_(if activated
             (delete-user! {:id user-id} {:connection tr-cn})
             (update-user-set-deleted! {:first_name "deleted" :last_name "deleted" :id user-id } {:connection tr-cn}))

         (delete-user! {:id user-id} {:connection tr-cn});delete user anyway
         )


       {:status "success"})
     (catch Object _
       {:status "error"}))))

(defn meta-tags [ctx id]
  (let [user (select-user {:id id} (into {:result-set-fn first} (get-db ctx)))]
    (if (= "public" (:profile_visibility user))
      {:title       (str (:first_name user) " " (:last_name user) " - profile")
       :description (str (get-site-name ctx) " user profile")
       :image       (:profile_picture user)})))


(defn- send-email-verification-maybe
  "If user has one email address and it is unverified, send email link."
  [ctx user-id]
  (let [emails (select-user-email-addresses {:user_id user-id} (get-db ctx))]
    (when (and (= 1 (count emails)) (not (-> emails first :verified)))
      (send-email-verified-link ctx (-> emails first :email) user-id))))

(defn- save-pending-badge-and-email [ctx user-id pending-badge-id new-account]
  (when pending-badge-id
    (when-let [user-badge (select-pending-badge {:id pending-badge-id :user_id user-id} (u/get-db-1 ctx))]
      (update-pending-badge! {:user_id user-id :id pending-badge-id} (u/get-db ctx))
      (put-pending-badge-email! {:user_id user-id :email (:email user-badge) :primary (if new-account 1 0)} (u/get-db ctx))
      (update-user-activate! {:id user-id} (get-db ctx)))))

(defn set-session [ctx ok-status user-id]
  (let [{:keys [role id private activated]} (user-information ctx user-id)]
    (assoc-in ok-status [:session :identity] {:id id :role role :private private :activated activated})))

(defn finalize-login [ctx ok-res user-id pending-badge-id new-account]
  (save-pending-badge-and-email ctx user-id pending-badge-id new-account)
  (send-email-verification-maybe ctx user-id)
  (set-session ctx ok-res user-id))

;; --- Email sender --- ;;

(defn get-user-and-primary-email [ctx user-id]
  (select-user-and-primary-address {:id user-id} (into {:result-set-fn first} (get-db ctx))))

(defn get-user-ids-from-event-owners [ctx]
  (select-userid-from-event-owners {} (get-db ctx)) )




