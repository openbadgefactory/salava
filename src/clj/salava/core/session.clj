(ns salava.core.session
  (:require [clojure.pprint :refer [pprint]]
            [ring.util.http-response :refer [unauthorized forbidden]]
            [ring.middleware.cookies :refer [wrap-cookies]]
            [ring.middleware.session :refer [wrap-session]]
            [ring.middleware.session.cookie :refer [cookie-store]]
            [buddy.auth.backends :as backends]
            [buddy.auth.accessrules :refer [wrap-access-rules]]
            [buddy.auth.middleware :refer [wrap-authentication wrap-authorization]]))

(def auth-backend
  ; By default responds with 401 or 403 if unauthorized
  (backends/session))

(defn wrap-session-expired [handler]
  (fn [request]
    (if (some-> request (get-in [:session :identity :expires]) (> (long (/ (System/currentTimeMillis) 1000))))
        (handler request)
        (handler (assoc request :session {})))))

;;TODO allow other cookies in bearer token request
(defn wrap-bearer-token [handler session-name]
  (fn  [request]
    (if (or (get-in request [:headers "cookie"])
            (nil? (get-in request [:headers "authorization"])))
      (handler request)
      (let [token (last (re-find #"(?i)^Bearer (.+)" (get-in request [:headers "authorization"])))]
        (handler (assoc-in request [:headers "cookie"] (str session-name "=" token)))))))

(defn wrap-app-session [routes config]
  (-> routes
      (wrap-authorization auth-backend)
      (wrap-authentication auth-backend)
      (wrap-session-expired)
      (wrap-session {:store (cookie-store {:key (get-in config [:session :secret])})
                     :root  (get-in config [:session :root])
                     :cookie-name  (get-in config [:session :name])
                     :cookie-attrs {:http-only true
                                    :secure    (get-in config [:session :secure])
                                    :max-age   (get-in config [:session :max-age])}})
      (wrap-bearer-token (get-in config [:session :name]))))

(defn access-error [req val]
  (unauthorized))

(defn wrap-rule [handler rule]
  (-> handler
      (wrap-access-rules {:rules [{:pattern #".*"
                                   :handler rule}]
                          :on-error access-error})))
