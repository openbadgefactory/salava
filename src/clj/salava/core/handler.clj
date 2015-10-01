(ns salava.core.handler
  (:require [ring.middleware.resource :refer [wrap-resource]]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [ring.middleware.session :refer [wrap-session]]
            [ring.middleware.session.cookie :refer [cookie-store]]
            [ring.middleware.webjars :refer [wrap-webjars]]
            [bidi.bidi]
            [bidi.ring]))

(defn wrap-middlewares [routes config]
  (-> routes
      (wrap-defaults (-> site-defaults
                         (assoc-in [:security :anti-forgery] false)
                         (assoc-in [:session] false)))
      (wrap-session {:store (cookie-store {:key (get-in config [:session :secret])})
                     :root  (get-in config [:session :root])
                     :cookie-name  (get-in config [:session :name])
                     :cookie-attrs {:http-only true
                                    :secure    (get-in config [:session :secure])
                                    :max-age   (get-in config [:session :max-age])}})
      (wrap-webjars)))

(defn handler [config routes] (-> routes
                           bidi.bidi/compile-route
                           bidi.ring/make-handler
                           (wrap-middlewares config)))
