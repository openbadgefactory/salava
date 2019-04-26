(ns salava.oauth.linkedin
  (:require [slingshot.slingshot :refer :all]
            [clojure.set :refer [rename-keys]]
            [clojure.string :refer [upper-case]]
            [salava.oauth.db :as d]
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
    (d/access-token :post "https://www.linkedin.com/oauth/v2/accessToken" opts)
    #_(d/access-token :post "https://www.linkedin.com/uas/oauth2/accessToken" opts)))



(defn parse-linkedin-user [ctx linkedin-user]
  (let [;location (or (get-in linkedin-user [:location :country :code]) (get-in ctx [:config :user :default-country]))
         ;language (get-in ctx [:config :user :default-language])
         user-locale (when (= (get-in linkedin-user [:firstName :preferredLocale]) (get-in linkedin-user [:lastName :preferredLocale]))
                       (get-in linkedin-user [:firstName :preferredLocale] nil))
         location (get-in ctx [:config :user :default-country])
         language (get user-locale :language (get-in ctx [:config :user :default-language]))
         firstName (-> linkedin-user :firstName :localized vals first)
         lastName (-> linkedin-user :lastName :localized vals first)
         picture-url (some-> (get-in linkedin-user [:profilePicture (keyword "displayImage~") :elements]) first :identifiers first :identifier)]

    {:first_name firstName
     :oauth_user_id (:id linkedin-user)
     :last_name lastName
     :picture_url picture-url
     :country location
     :language language
     :email (:email linkedin-user)}))

(defn linkedin-login [ctx code state current-user-id error]
  (try+
    (if (or error (not (and code state)))
      (throw+ "oauth/Unexpectederroroccured"))
    (let [access-token (linkedin-access-token ctx code linkedin-login-redirect-path)
          linkedin-user (d/oauth-user ctx "https://api.linkedin.com/v2/me?projection=(id,firstName,lastName,profilePicture(displayImage~:playableStreams))" {:oauth-token access-token} parse-linkedin-user) #_(d/oauth-user ctx "https://api.linkedin.com/v1/people/~:(id,first-name,last-name,picture-url,email-address,location)?format=json" {:oauth-token access-token} parse-linkedin-user)
          email-handle (keyword "handle~")
          email (some-> (d/oauth-request :get "https://api.linkedin.com/v2/emailAddress?q=members&projection=(elements*(handle~))" {:oauth-token access-token}) :elements first email-handle :emailAddress)
          user-id (d/get-or-create-user ctx "linkedin" (assoc linkedin-user :email email) current-user-id)]

      (do
        (d/update-user-last_login ctx user-id)
        {:status "success" :user-id user-id}))
    (catch Object _
      {:status "error" :message _})))

(defn linkedin-deauthorize [ctx current-user-id]
  (try+
    (d/remove-oauth-user ctx current-user-id "linkedin")
    {:status "success"}
    (catch Object _
      {:status "error" :message _})))
