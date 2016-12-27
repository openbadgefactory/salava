(ns salava.oauth.google
  (:require [slingshot.slingshot :refer :all]
            [salava.oauth.db :as d]
            [salava.user.db :as u]
            [salava.core.countries :refer [all-countries]]
            [salava.core.util :refer [get-site-url get-base-path]]))

(def google-base-url "https://accounts.google.com/o")

(defn google-access-token [ctx code redirect-path]
  (let [app-id (get-in ctx [:config :oauth :google :app-id])
        app-secret (get-in ctx [:config :oauth :google :app-secret])
        redirect-url (str (get-site-url ctx) (get-base-path ctx) redirect-path)
        opts {:query-params {:code code
                             :client_id app-id
                             :client_secret app-secret
                             :redirect_uri redirect-url}}]
    (d/access-token :get (str google-base-url "/oauth2/token") opts)))






