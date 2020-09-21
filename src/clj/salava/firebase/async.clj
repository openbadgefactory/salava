(ns salava.firebase.async
  (:require [clojure.tools.logging :as log]
            [clojure.string :as string]
            [clojure.pprint :refer [pprint]]
            [clojure.java.io :as io]
            [clojure.data.json :as json]
            [yesql.core :refer [defqueries]]
            [salava.core.util :as u]
            [salava.core.i18n :refer [t]]
            [salava.core.http :as http])
  (:import [com.google.auth.oauth2 GoogleCredentials ServiceAccountCredentials AccessToken]
           [java.util Vector]))

(defqueries "sql/firebase/queries.sql")

(defonce account-credentials (atom nil))

;; com.google.auth.oauth2 parts from:
;;   https://github.com/alekcz/google-credentials
;;   https://github.com/alekcz/fire

(defn- build-service-account-credentials [creds]
  (let [credentials (ServiceAccountCredentials/fromPkcs8
                      ^String (-> creds :client_id str)
                      ^String (-> creds :client_email str)
                      ^String (-> creds :private_key str)
                      ^String (-> creds :private_key_id str)
                      [])]
    (-> (^ServiceAccountCredentials$Builder .toBuilder credentials)
        (.setProjectId (-> creds :project_id str))
        (^ServiceAccountCredentials .build))))


(defn- now []
  (inst-ms (java.util.Date.)))

(defn- auth-token [conf]
  (if-let [cred @account-credentials]
    (do
      (.refreshIfExpired cred)
      (-> cred .getAccessToken .getTokenValue))
    (let [^ServiceAccountCredentials cred (build-service-account-credentials conf)
          ^ServiceAccountCredentials scoped (.createScoped cred ^Vector (into [] ["https://www.googleapis.com/auth/firebase.messaging"]))]
      (reset! account-credentials scoped)
      (-> @account-credentials ^AccessToken .refreshAccessToken .getTokenValue))))


(defn- http-batch-message [auth url payload]
  (str
    "--subrequest_boundary" "\r\n"
    "Content-Type: application/http" "\r\n"
    "Content-Transfer-Encoding: binary" "\r\n"
    "Authorization: Bearer " auth "\r\n" "\r\n"

    "POST " url "\r\n"
    "Content-Type: application/json" "\r\n"
    "accept: application/json" "\r\n" "\r\n"

    (json/write-str payload) "\r\n"))

(defn- png-convert-url [ctx image]
  (if image
    (if (re-find #"\w+\.svg$" image)
      (str (u/get-full-path ctx) "/obpv1/file/as-png?image=" image)
      (str (u/get-site-url ctx) "/" image))
    "https://openbadgepassport.com/theme/img/logo.png"))

(defn- notification-content [ctx input]
  (let [badge  (some-> input vals first first http/json-get :badge http/json-get)
        issuer (some-> badge :issuer http/json-get)
        image  (some->> badge :image (u/file-from-url ctx))]
    (fn [lang]
      (if (and badge issuer image)
        {:title (t :badge/yougotnewbadge lang)
         :image (png-convert-url ctx image)
         :body (str (:name badge) " " (t :badge/fromissuer lang) " " (:name issuer))}

        {:title "You got a new open badge!"
         :body "Click here to see it."}))))

(defn new-badge-notification [ctx input]
  (when (get-in ctx [:config :firebase :project_id])
    (let [conf (get-in ctx [:config :firebase])
          auth (auth-token conf)
          url (str "/v1/projects/" (:project_id conf) "/messages:send")
          emails (map name (keys input))
          content (notification-content ctx input)]
      (doseq [chunk (partition-all 100 emails)]
        (let [body (->> (select-firebase-tokens-by-emails {:emails chunk} (u/get-db ctx))
                        (map (fn [user]
                               (http-batch-message
                                 auth url
                                 {:message
                                  {:token (:firebase_token user)
                                   :notification (content (:language user))
                                   :android {:notification (merge {:default_sound true
                                                                   :default_vibrate_timings true
                                                                   :default_light_settings true}
                                                                  (content (:language user)))}}})))
                        (apply str))]

          (when-not (string/blank? body)
            (http/http-post
              "https://fcm.googleapis.com/batch"
              {:headers {"Content-Type" "multipart/mixed; boundary=\"subrequest_boundary\""}
               :content-type "multipart/mixed; boundary=\"subrequest_boundary\""
               :body (str body "--subrequest_boundary--" "\r\n")
               })))))))


(defn subscribe [ctx]
  {:new-factory-badge (fn [data] (new-badge-notification ctx data))})
