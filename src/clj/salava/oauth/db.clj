(ns salava.oauth.db
  (:require [yesql.core :refer [defqueries]]
            [clojure.java.jdbc :as jdbc]
            [clojure.set :refer [rename-keys]]
            [clj-http.client :as http]
            [slingshot.slingshot :refer :all]
            [salava.core.util :refer [get-db get-datasource get-site-url get-base-path save-file-from-http-url]]))

(defqueries "sql/oauth/queries.sql")

(defn oauth-request [method url opts]
  (try+
    (-> {:method       method
         :url          url
         :content-type :json
         :as           :json}
        (merge opts)
        (http/request)
        (get :body))
    (catch Object _
      ;TODO: (log "OAuth request failed" _)
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

(defn create-local-user [ctx oauth-user service]
  (try+
    (let [picture-url (:picture_url oauth-user)
          profile-picture (if picture-url (save-file-from-http-url ctx (str picture-url)))
          new-user (-> oauth-user
                       (dissoc :oauth_user_id :email :picture_url)
                       (assoc :profile_picture profile-picture))
          new-user-id (:generated_key (insert-user<! new-user (get-db ctx)))
          _ (insert-user-email! {:email (:email oauth-user) :user_id new-user-id} (get-db ctx))
          _ (add-oauth-user ctx new-user-id (:oauth_user_id oauth-user) service)]
      new-user-id)
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