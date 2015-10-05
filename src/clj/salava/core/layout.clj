(ns salava.core.layout
  (:require [compojure.api.sweet :refer :all]
            [clojure.java.io :as io]
            [clojure.data.json :as json]
            [ring.util.response :as r]
            [hiccup.page :refer [html5 include-css include-js]]))

(def asset-css
  ["/assets/bootstrap/css/bootstrap.min.css"
   "/assets/bootstrap/css/bootstrap-theme.min.css"])

(def asset-js
  ["/assets/jquery/jquery.min.js"
   "/assets/bootstrap/js/bootstrap.min.js"])


(defn with-version [ctx resource-name]
  (let [version (get-in ctx [:config :core :asset-version])]
    (str resource-name "?_=" (or version (System/currentTimeMillis)))))


(defn css-list [ctx]
  (let [plugin-css (map #(str "/css/" (name %) ".css") (cons :core (get-in ctx [:config :core :plugins])))]
    (map #(with-version ctx %) (concat asset-css plugin-css))))


(defn js-list [ctx]
    (map #(with-version ctx %) (conj asset-js "/js/salava.js")))


(defn context-js [ctx]
  (let [ctx-out {:plugins {:all (get-in ctx [:config :core :plugins])}}]
    (str "function salavaCoreCtx() { return " (json/write-str ctx-out) "; }")))


(defn main-view [ctx]
  (html5
    [:head
     [:title (get-in ctx [:config :core :site-name])]
     [:meta {:charset "utf-8"}]
     [:meta {:http-equiv "X-UA-Compatible" :content "IE=edge"}]
     [:meta {:name "viewport" :content "width=device-width, initial-scale=1.0"}]
     (apply include-css (css-list ctx))
     [:script {:type "text/javascript"} (context-js ctx)]]
    [:body
     [:div#app]
     "<!--[if lt IE 10]>"
     (include-js "/assets/es5-shim/es5-shim.js" "/assets/es5-shim/es5-sham.js")
     "<![endif]-->"
     (apply include-js (js-list ctx))]))


(defn main-response [ctx]
  (-> (r/response (main-view ctx))
      (r/header "Content-type" "text/html; charset=\"UTF-8\"")))

(defn main [path]
  (GET* path []
        :components [context]
        (main-response context)))


