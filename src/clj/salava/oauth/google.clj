(ns salava.oauth.google
  (:require [slingshot.slingshot :refer :all]
            [salava.oauth.db :as d]
            [salava.user.db :as u]
            [salava.core.countries :refer [all-countries]]
            [salava.core.util :refer [get-site-url get-base-path]]))

(def api {:access-token-api "https://www.googleapis.com/oauth2/v4/token"
          :user-info-api "https://www.googleapis.com/oauth2/v2/userinfo"
          :revoke-token-api "https://accounts.google.com/o/oauth2/revoke"})

(defn google-access-token [ctx code redirect-path]
  (let [app-id (get-in ctx [:config :oauth :google :app-id])
        app-secret (get-in ctx [:config :oauth :google :app-secret])
        redirect-url (str (get-site-url ctx) (get-base-path ctx) redirect-path)
        opts {:query-params {:code code
                             :client_id app-id
                             :client_secret app-secret
                             :redirect_uri redirect-url
                             :grant_type "authorization_code"}}]
    (d/access-token :post (:access-token-api api) opts)))


(defn parse-google-user [ctx google-user]
  (let [{:keys [id given_name family_name picture email locale]} google-user
        [_ language country] (if (and (string? locale) (= (count locale) 5))
                               (re-find #"([a-z]{2})-([A-Z]{2})" locale))
        country (if (get all-countries country)
                  country
                  (get-in ctx [:config :user :default-country]))
        language (if (some #(= (keyword language) %) (get-in ctx [:config :core :languages]))
                   language
                   (get-in ctx [:config :user :default-language]))]

       {:oauth_user_id id
        :email email
        :first_name given_name
        :last_name family_name
        :country country
        :language language
        :picture_url picture}))

(defn google-login [ctx code current-user-id error]
  (try+
   (if (or error (clojure.string/blank? code))
    (throw+ "oauth/Unexpectederroroccured")
    (let [access-token (google-access-token ctx code "/oauth/google")
          google-user (d/oauth-user ctx (str (:user-info-api api)"?access_token=" access-token ) {} parse-google-user)
          user-id (d/get-or-create-user ctx "google" google-user current-user-id)]
      (do
       (d/update-user-last_login ctx user-id)
       {:status "success" :user-id user-id})))
   (catch Object _
    {:status "error" :message _})))


(defn google-deauthorize [ctx code current-user-id]
  (try+
    (let [access-token (google-access-token ctx code "/oauth/google/deauthorize")]
      (d/oauth-request :post (str (:revoke-token-api api)"?token=" access-token) {})
      (d/remove-oauth-user ctx current-user-id "google")
      {:status "success"})
    (catch Object _
      {:status "error" :message (str _)})))
