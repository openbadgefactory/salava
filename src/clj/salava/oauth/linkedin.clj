(ns salava.oauth.linkedin
  (:require [slingshot.slingshot :refer :all]
            [clojure.set :refer [rename-keys]]
            [clojure.string :refer [upper-case]]
            [salava.oauth.db :as d]
            [salava.user.db :as u]
            [salava.core.countries :refer [all-countries]]
            [salava.core.util :refer [get-site-url get-base-path]]))

(def linkedin-login-redirect-path "/oauth/linkedin")

(defn linkedin-access-token [ctx code redirect-path]
  (let [app-id (get-in ctx [:config :oauth :linkedin :app-id])
        app-secret (get-in ctx [:config :oauth :linkedin :app-secret])
        redirect-url (str (get-site-url ctx) (get-base-path ctx) redirect-path)
        opts {:content-type :x-www-form-urlencoded
              :form-params {:code code
                            :client_id app-id
                            :client_secret app-secret
                            :redirect_uri redirect-url
                            :grant_type "authorization_code"}}]
    (d/access-token :post "https://www.linkedin.com/uas/oauth2/accessToken" opts)))

(defn parse-linkedin-user [ctx linkedin-user]
  (let [location (or (get-in linkedin-user [:location :country :code]) (get-in ctx [:config :user :default-country]))
        language (get-in ctx [:config :user :default-language])]
    (-> linkedin-user
        (rename-keys {:emailAddress :email
                      :id :oauth_user_id
                      :firstName :first_name
                      :lastName :last_name
                      :pictureUrl :picture_url})
        (assoc :country (upper-case location) :language language))))

(defn linkedin-login [ctx code state current-user-id error]
  (try+
    (if (or error (not (and code state)))
      (throw+ "oauth/Unexpectederroroccured"))
    (let [access-token (linkedin-access-token ctx code linkedin-login-redirect-path)
          linkedin-user (d/oauth-user ctx "https://api.linkedin.com/v1/people/~:(id,first-name,last-name,picture-url,email-address,location)?format=json" {:oauth-token access-token} parse-linkedin-user)
          user-id (d/get-or-create-user ctx "linkedin" linkedin-user current-user-id)
          {:keys [role]} (u/user-information ctx user-id)]
      (do
        (d/update-user-last_login ctx user-id)
        {:status "success" :user-id user-id :role role}))
    (catch Object _
      {:status "error" :message _})))

(defn linkedin-deauthorize [ctx current-user-id]
  (try+
    (d/remove-oauth-user ctx current-user-id "linkedin")
    {:status "success"}
    (catch Object _
      {:status "error" :message _})))
