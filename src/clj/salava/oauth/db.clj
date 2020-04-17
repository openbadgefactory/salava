(ns salava.oauth.db
  (:require [clojure.tools.logging :as log]
            [yesql.core :refer [defqueries]]
            [clojure.java.jdbc :as jdbc]
            [clojure.set :refer [rename-keys]]
            [clj-http.client :as http]
            [buddy.hashers :as hashers]
            [slingshot.slingshot :refer :all]
            [salava.core.helper :refer [dump]]
            [ring.middleware.session.cookie :refer [cookie-store]]
            [ring.middleware.session :refer [session-response]]
            [salava.core.util :as u :refer [get-db get-datasource get-site-url get-base-path save-file-from-http-url get-plugins]]))


(defqueries "sql/oauth/queries.sql")

(defn oauth-request [method url opts]
  (try
    (-> {:method       method
         :url          url
         :socket-timeout 30000
         :conn-timeout   30000
         :content-type :json
         :as           :json}
        (merge opts)
        (http/request)
        (get :body))
    (catch Exception ex
      (log/error "OAuth request failed")
      (log/error (.toString ex))
      (throw+ "oauth/Cannotconnecttoservice"))))

(defn access-token [method url opts]
  (let [response (oauth-request method url opts)
        access-token (:access_token response)]
    (when-not access-token
      ;TODO: (log "OAuth request failed" _)
      (throw+ "oauth/Cannotconnecttoservice"))
    access-token))

(defn oauth-user [ctx url opts parse-fn]
  (let [user (oauth-request :get url opts)]
    (if-not user
      (throw+ "oauth/Cannotconnecttoservice"))
    (parse-fn ctx user)))

(defn remove-oauth-user [ctx user-id service]
  (delete-oauth-user! {:user_id user-id :service service} (get-db ctx)))

(defn remove-oauth-user-all-services [ctx user-id]
  (delete-oauth-user-all-services! {:user_id user-id} (get-db ctx)))

(defn add-oauth-user [ctx user-id oauth-user-id service]
  (remove-oauth-user ctx user-id service)
  (insert-oauth-user! {:user_id user-id :oauth_user_id oauth-user-id :service service} (get-db ctx))
  user-id)

(defn insert-user-terms [ctx user-id status]
  (let [show-terms? (get-in ctx [:config :core :show-terms?] false)]
    (when show-terms?
      (try+
        (do
          (insert-user-terms<! {:user_id user-id :status status} (get-db ctx))
          {:status "success" :input status})
        (catch Object _
          {:status "error" :input status})
        ))))

(defn create-local-user [ctx oauth-user service]
  (try+
    (let [picture-url (:picture_url oauth-user)
          profile-picture (if picture-url (save-file-from-http-url ctx (str picture-url)))
          email_notifications (get-in ctx [:config :user :email-notifications] true)
          new-user (-> oauth-user
                       (dissoc :oauth_user_id :email :picture_url)
                       (assoc :profile_picture profile-picture :email_notifications email_notifications))
          new-user-id (:generated_key (insert-user<! new-user (get-db ctx)))
          _ (insert-user-email! {:email (:email oauth-user) :user_id new-user-id} (get-db ctx))
          _ (add-oauth-user ctx new-user-id (:oauth_user_id oauth-user) service)
          ]
      #_new-user-id
      {:user-id new-user-id :new-user true})
    (catch Object _
      ;TODO: (log "Could not create user via OAuth" _)
      (throw+ "oauth/Unexpectederroroccured"))))

(defn get-or-create-user [ctx service oauth-user current-user-id]
  (let [oauth-user-id (:oauth_user_id oauth-user)
        user-id (select-user-id-by-oauth-user-id-and-service {:oauth_user_id oauth-user-id :service service} (into {:result-set-fn first :row-fn :user_id} (get-db ctx)))
        {:keys [user_id verified]} (if-not user-id (select-user-id-by-email {:email (:email oauth-user)} (into {:result-set-fn first} (get-db ctx))))
        user-id-by-email user_id]
    (if current-user-id
      (cond user-id (throw+ "oauth/Accountalreadyregistered")
            (and user-id-by-email (not= current-user-id user-id-by-email)) (throw+ "oauth/Emailalreadyregistered")
            (and user-id-by-email (= current-user-id user-id-by-email) (not verified)) (throw+ "oauth/Emailnotverified")
            :else (add-oauth-user ctx current-user-id oauth-user-id service))
      (if user-id
        user-id
        (if user-id-by-email
          (if verified
            (add-oauth-user ctx user-id-by-email oauth-user-id service)
            (throw+ "oauth/Facebookemailnotverified"))
          (create-local-user ctx oauth-user service))))))

(defn login-status [ctx user-id service]
  (let [oauth-user-id (select-oauth-user-id {:user_id user-id :service service} (into {:result-set-fn first :row-fn :oauth_user_id} (get-db ctx)))
        password (select-user-password {:id user-id} (into {:result-set-fn first :row-fn :pass} (get-db ctx)))]
    {:active (boolean oauth-user-id) :no-password? (empty? password)}))

(defn update-user-last_login [ctx user-id]
  (let [last_login  (select-oauth-user-last-login {:id user-id} (into {:result-set-fn first :row-fn :last_login} (get-db ctx)))]
    (when last_login
      (store-user-last-visited! {:user_id user-id :value last_login} (get-db ctx)))
    (update-user-last_login! {:id user-id} (get-db ctx))))

(defn get-user-information [ctx user-id]
  (select-oauth-user-service {:user_id user-id}  (into {:row-fn :service} (get-db ctx))))

(defn user-session [ctx user expires]
  (let [session (-> user
                    (select-keys [:id :role :activated])
                    (assoc :private (get-in ctx [:config :core :private] false))
                    (assoc :last-visited (:last_login user))
                    (assoc :expires (+ (long (/ (System/currentTimeMillis) 1000)) expires)))
        temp-res (-> {:session {:identity session}}
                     (session-response {}
                                       {:store (cookie-store {:key (get-in ctx [:config :core :session :secret])})
                                        :root  "/"
                                        :cookie-name  "oauth2"}))
        session-str (get-in temp-res [:headers "Set-Cookie"])]
    (some->> session-str first (re-find #"oauth2=(.+)") last)))

(defn authorization-code [ctx client_id user_id code_challenge]
  (let [auth_code (u/random-token user_id)]
    (jdbc/with-db-transaction  [tx (:connection (get-db ctx))]
      (delete-oauth2-auth-code! {:user_id user_id :client_id client_id} {:connection tx})
      (insert-oauth2-auth-code! {:user_id user_id :client_id client_id :auth_code auth_code :auth_code_challenge code_challenge} {:connection tx}))
    auth_code))

(defn- challenge-hash [code_verifier]
  (some->> code_verifier (u/digest "sha256") u/bytes->base64-url))

;; Expiry time for OAuth access token in seconds. Low value used for debugging.
(def token-expires 120)

(defn new-access-token [ctx client_id auth_code code_verifier]
  (when-let [user (select-oauth2-auth-code-user {:client_id client_id :auth_code auth_code :auth_code_challenge (challenge-hash code_verifier)} (u/get-db-1 ctx))]
    (let [rtoken (u/random-token auth_code)
          expires token-expires]
      (update-oauth2-auth-code! {:client_id client_id :auth_code auth_code :rtoken (hashers/derive rtoken {:alg :bcrypt+sha512})} (get-db ctx))
      {:access_token (user-session ctx user expires)
       :refresh_token (str (:id user) "-" rtoken)
       :token_type "bearer",
       :expires expires})))

(defn refresh-access-token [ctx client_id [user_id refresh_token]]
  (let [users (select-oauth2-refresh-token-user {:client_id client_id :user_id user_id} (u/get-db ctx))]
    (when-let [user (first (filter #(hashers/check refresh_token (:refresh_token %)) users))]
      (let [out-token (u/random-token refresh_token)
            enc-token  (hashers/derive out-token {:alg :bcrypt+sha512})
            expires token-expires]
        (update-oauth2-refresh-token! {:client_id client_id :id (:token_id user) :user_id (:id user) :rtoken enc-token} (get-db ctx))
        {:access_token (user-session ctx user expires)
         :refresh_token (str (:id user) "-" out-token)
         :token_type "bearer"
         :expires expires}))))

(defn unauthorize-client [ctx client_id user_id]
  (delete-oauth2-token! {:client_id client_id :user_id user_id} (get-db ctx))
  {:action "unauthorize" :success true})
