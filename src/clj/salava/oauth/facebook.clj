(ns salava.oauth.facebook
  (:require [slingshot.slingshot :refer :all]
            [salava.oauth.db :as d]
            [salava.user.db :as u]
            [salava.core.countries :refer [all-countries]]
            [salava.core.util :refer [get-site-url get-base-path]]))

(def facebook-base-url "https://graph.facebook.com/v2.6")

(defn facebook-access-token [ctx code redirect-path]
  (let [app-id (get-in ctx [:config :oauth :facebook :app-id])
        app-secret (get-in ctx [:config :oauth :facebook :app-secret])
        redirect-url (str (get-site-url ctx) (get-base-path ctx) redirect-path)
        opts {:query-params {:code code
                             :client_id app-id
                             :client_secret app-secret
                             :redirect_uri redirect-url}}]
    (d/access-token :get (str facebook-base-url "/oauth/access_token") opts)))

(defn parse-facebook-user [ctx facebook-user]
  (let [{:keys [id first_name last_name email locale]} facebook-user
        email (or email (str id "@facebook.com"))
        [_ language country] (if (and (string? locale) (= (count locale) 5))
                               (re-find #"([a-z]{2})_([A-Z]{2})" locale))
        country (if (get all-countries country)
                  country
                  (get-in ctx [:config :user :default-country]))
        language (if (some #(= (keyword language) %) (get-in ctx [:config :core :languages]))
                   language
                   (get-in ctx [:config :user :default-language]))]
    {:oauth_user_id id
     :email email
     :first_name first_name
     :last_name last_name
     :country country
     :language language
     :picture_url (get-in facebook-user [:picture :data :url])}))

(defn facebook-login [ctx code current-user-id error]
  (try+
    (if (or error (not code))
      (throw+ "oauth/Unexpectederroroccured"))
    (let [access-token (facebook-access-token ctx code "/oauth/facebook")
          facebook-user (d/oauth-user ctx (str facebook-base-url "/me") {:query-params {:access_token access-token :fields "id,email,first_name,last_name,picture,locale"}} parse-facebook-user)
          user-id (d/get-or-create-user ctx "facebook" facebook-user current-user-id)
          {:keys [role]} (u/user-information ctx user-id)
          private (get-in ctx [:config :core :private] false)]
      (do
        (d/update-user-last_login ctx user-id)
        {:status "success" :user-id user-id :role role :private private}))
    (catch Object _
      {:status "error" :message _})))

(defn facebook-deauthorize [ctx code current-user-id]
  (try+
    (let [access-token (facebook-access-token ctx code "/oauth/facebook/deauthorize")]
      (d/oauth-request :delete (str facebook-base-url "/me/permissions") {:query-params {:access_token access-token}})
      (d/remove-oauth-user ctx current-user-id "facebook")
      {:status "success"})
    (catch Object _
      {:status "error" :message _})))
