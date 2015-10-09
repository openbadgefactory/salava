(ns salava.core.handler
  (:require [compojure.api.sweet :refer :all]
            [salava.registry]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [ring.middleware.session :refer [wrap-session]]
            [ring.middleware.session.cookie :refer [cookie-store]]
            [ring.middleware.webjars :refer [wrap-webjars]]))


(defn wrap-middlewares [ctx routes]
  (let [config (get-in ctx [:config :core])]
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
        (wrap-webjars))))


(defn handler [ctx]
  (let [route-fn (:routes salava.registry/enabled)]
    (wrap-middlewares ctx (route-fn ctx))))
