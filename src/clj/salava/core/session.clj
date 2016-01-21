(ns salava.core.session
  (:require [ring.util.http-response :refer [unauthorized forbidden]]
            [ring.middleware.session :refer [wrap-session]]
            [ring.middleware.session.cookie :refer [cookie-store]]
            [buddy.auth.backends.session :refer [session-backend]]
            [buddy.auth.accessrules :refer [wrap-access-rules]]
            [buddy.auth.middleware :refer [wrap-authentication wrap-authorization]]))

(def auth-backend
  ; By default responds with 401 or 403 if unauthorized
  (session-backend))

(defn wrap-app-session [routes config]
  (-> routes
      (wrap-authorization auth-backend)
      (wrap-authentication auth-backend)
      (wrap-session {:store (cookie-store {:key (get-in config [:session :secret])})
                     :root  (get-in config [:session :root])
                     :cookie-name  (get-in config [:session :name])
                     :cookie-attrs {:http-only true
                                    :secure    (get-in config [:session :secure])
                                    :max-age   (get-in config [:session :max-age])}})))

(defn access-error [req val]
  (unauthorized))

(defn wrap-rule [handler rule]
  (-> handler
      (wrap-access-rules {:rules [{:pattern #".*"
                                   :handler rule}]
                          :on-error access-error})))