(ns salava.core.handler
  (:require [compojure.api.sweet :refer :all]
            [com.stuartsierra.component :as component]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [ring.middleware.session :refer [wrap-session]]
            [ring.middleware.session.cookie :refer [cookie-store]]
            [ring.middleware.webjars :refer [wrap-webjars]]))


(defn wrap-middlewares [context routes]
  (let [config (get-in context [:config :core])]
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


(defn handler [context route-coll]
  (let [route-def (apply routes route-coll)]
    (api {:components {:context context}}
         (swagger-docs {:info  {:title "OBP API"
                                :description "Open Badge Passport API"}})
         (swagger-ui "/swagger-ui" :validator-url nil)
         (wrap-middlewares context route-def))))
