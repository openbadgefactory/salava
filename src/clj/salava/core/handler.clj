(ns salava.core.handler
  (:require [clojure.tools.logging :as log]
            [compojure.api.sweet :refer :all]
            [compojure.route :as route]
            [salava.core.session :refer [wrap-app-session]]
            [salava.core.util :refer [get-base-path get-data-dir]]
            [salava.core.routes :refer [legacy-routes]]
            [salava.core.helper :refer [dump plugin-str]]
            [slingshot.slingshot :refer :all]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [ring.middleware.session :refer [wrap-session]]
            [ring.middleware.flash :refer [wrap-flash]]
            [ring.middleware.session.cookie :refer [cookie-store]]
            [ring.middleware.webjars :refer [wrap-webjars]]))


(defn get-route-def [ctx plugin]
  (try+
    (let [sym (symbol (str "salava." (clojure.string/replace (plugin-str plugin) #"/" ".") ".routes/route-def"))]
      (require (symbol (namespace sym)) :reload)
      ((resolve sym) ctx))
    (catch Object _
      (log/info (str "no routes in plugin " plugin)))))


(defn resolve-routes [ctx]
  (apply routes (map (fn [p] (get-route-def ctx p)) (conj (get-in ctx [:config :core :plugins]) :core))))


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
        (wrap-webjars)
        (wrap-defaults (-> site-defaults
                           (assoc-in [:security :anti-forgery] false)
                           (assoc-in [:session] false)
                           (assoc-in [:static :files] (get-data-dir ctx))
                           ))
        (wrap-flash)
        (wrap-app-session config))))


(defn handler [ctx]
  (let [main-routes (resolve-routes ctx)]
    (wrap-middlewares
      ctx
      (api
        (swagger-routes {:ui "/swagger-ui"
                         :info  {:version "0.1.0"
                               :title "Salava REST API"
                               :description ""
                               :contact  {:name "Discendum Oy"
                                          :email "contact@openbadgepassport.com"
                                          :url "http://salava.org"}
                               :license  {:name "Apache 2.0"
                                          :url "http://www.apache.org/licenses/LICENSE-2.0"}}
                       :tags  [{:name "badge", :description "plugin"}
                               {:name "file", :description "plugin"}
                               {:name "gallery", :description "plugin"}
                               {:name "page", :description "plugin"}
                               {:name "translator", :description "plugin"}
                               {:name "user", :description "plugin"}]})

        (context (get-base-path ctx) [] main-routes)

        (legacy-routes ctx)

        (route/not-found "404 Not found")))))

