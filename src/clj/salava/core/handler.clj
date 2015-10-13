(ns salava.core.handler
  (:require [compojure.api.sweet :refer :all]
            [salava.registry]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [ring.middleware.session :refer [wrap-session]]
            [ring.middleware.session.cookie :refer [cookie-store]]
            [ring.middleware.webjars :refer [wrap-webjars]]))

(defn ignore-trailing-slash
  "Modifies the request uri before calling the handler.
  Removes a single trailing slash from the end of the uri if present.

  Useful for handling optional trailing slashes until Compojure's route matching syntax supports regex.
  Adapted from http://stackoverflow.com/questions/8380468/compojure-regex-for-matching-a-trailing-slash"
  [handler]
  (fn  [request]
    (let [uri (:uri request)]
      (handler (assoc request :uri (if (and (not (= "/" uri))
                                            (.endsWith uri "/"))
                                     (subs uri 0 (dec (count uri)))
                                     uri))))))

(defn wrap-middlewares [ctx routes]
  (let [config (get-in ctx [:config :core])]
    (-> routes
        (ignore-trailing-slash)
        (wrap-defaults (-> site-defaults
                           (assoc-in [:security :anti-forgery] false)
                           (assoc-in [:session] false)))
        (wrap-session {:store (cookie-store {:key (get-in config [:session :secret])})
                       :root  (get-in config [:session :root])
                       :cookie-name  (get-in config [:session :name])
                       :cookie-attrs {:http-only true
                                      :secure    (get-in config [:session :secure])
                                      :max-age   (get-in config [:session :max-age])}})
        (wrap-webjars))))


(defn handler [ctx]
  (let [route-fn (:routes salava.registry/enabled)]
    (wrap-middlewares ctx (route-fn ctx))))
