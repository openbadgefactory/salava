(ns salava.core.layout
  (:require [compojure.api.sweet :refer :all]
            [ring.util.http-response :refer [ok content-type]]
            [schema.core :as s]
            [clojure.java.io :as io]
            [clojure.data.json :as json]
            [salava.core.helper :refer [dump plugin-str private?]]
            [salava.core.util :refer [get-site-url plugin-fun get-plugins]]
            [clojure.string :refer [join split capitalize]]
            [salava.user.db :as u]
            [salava.badge.main :as b]
            [salava.page.main :as p]
            [salava.gallery.db :as g]
            [hiccup.page :refer [html5 include-css include-js]]
            salava.core.restructure))

(def asset-css
  ["/assets/bootstrap/css/bootstrap.min.css"
   "/assets/bootstrap/css/bootstrap-theme.min.css"
   "/assets/font-awesome/css/font-awesome.min.css"
   "/assets/leaflet/leaflet.css"
   "/css/rateit/rateit.css"
   "/css/simplemde.min.css"])


(def asset-js
  ["/assets/jquery/jquery.min.js"
   "/assets/bootstrap/js/bootstrap.min.js"
   "/assets/leaflet/leaflet.js"
   "/js/ckeditor/ckeditor.js"])


(defn with-version [ctx resource-name]
  (let [version (get-in ctx [:config :core :asset-version])]
    (str resource-name "?_=" (or version (System/currentTimeMillis)))))


(defn css-list [ctx]
  (let [plugins (get-in ctx [:config :core :plugins])
        coll (mapcat #(get-in ctx [:config % :css] []) plugins)]
    (if-not (empty? coll)
      ; If any plugin defines a list of css files, use those as-is
      (map #(with-version ctx %) (concat asset-css coll))
      ; Otherwise, create the list using plugin names
      (->> plugins
           (cons :core)
           (map plugin-str)
           (map    #(str "/css/" % ".css"))
           (filter #(io/resource (str "public" %)))
           (concat asset-css)
           (map #(with-version ctx %))))))


(defn js-list [ctx]
    (map #(with-version ctx %) (conj asset-js "/js/salava.js")))


(defn context-js [ctx]
  (let [site-name (get-in ctx [:config :core :site-name])
        share     {:site-name (get-in ctx [:config :core :share :site-name] site-name)
                   :hashtag   (get-in ctx [:config :core :share :hashtag] (->> (split site-name #" ")
                                                                               (map capitalize)
                                                                               join)) }
        ctx-out   {:plugins         {:all (map plugin-str (get-in ctx [:config :core :plugins]))}
                   :user            (:user ctx)
                   :flash-message   (:flash-message ctx)
                   :site-url        (get-in ctx [:config :core :site-url])
                   :site-name       site-name
                   :share           share
                   :base-path       (get-in ctx [:config :core :base-path])
                   :facebook-app-id (get-in ctx [:config :oauth :facebook :app-id])
                   :linkedin-app-id (get-in ctx [:config :oauth :linkedin :app-id])
                   :languages       (map name (get-in ctx [:config :core :languages]))
                   :private         (private? ctx)
                   :footer          (get-in ctx [:config :extra/theme :footer] nil)
                   :factory-url     (get-in ctx [:config :factory :url])
                   :show-terms?     (get-in ctx [:config :core :show-terms?] false)
                   :filter-options  (first (mapcat #(get-in ctx [:config % :filter-options] []) (get-plugins ctx)))
                   }]
    (str "function salavaCoreCtx() { return " (json/write-str ctx-out) "; }")))


(defn include-meta-tags [ctx tags]
  (if tags
    (let [{:keys [title description image]} tags]
      [[:meta {:property "og:title" :content title}]
       [:meta {:property "og:description" :content description}]
       [:meta {:name "description" :content description}]
       [:meta {:property "og:image" :content (str (get-site-url ctx) "/" image)}]])))

(defn favicon [ctx]
  (let [favicon-url (first (plugin-fun (get-plugins ctx) "block" "favicon"))]
    (if favicon-url
      (favicon-url ctx)
      {:icon "/img/favicon.icon"
       :png  "/img/favicon.png"})))

(defn html-attributes [ctx]
  (let [attrib (-> {:dir "ltr"}
                   (cons (map (fn [f] (f ctx)) (plugin-fun (get-plugins ctx) "layout" "html-attributes"))))]
    (apply merge attrib)))


(defn main-view
  ([ctx] (main-view ctx nil))
  ([ctx meta-tags]
   (let [favicons (favicon ctx)
         attrib (html-attributes ctx)]
     (html5 {:dir (:dir attrib)}
      [:head
       [:title (get-in ctx [:config :core :site-name])]
       [:meta {:charset "utf-8"}]
       [:meta {:http-equiv "X-UA-Compatible" :content "IE=edge"}]
       [:meta {:name "viewport" :content "width=device-width, initial-scale=1.0"}]
       [:meta {:property "og:sitename" :content (get-in ctx [:config :core :site-name])}]
       (seq (include-meta-tags ctx meta-tags))
       (when (:json-oembed meta-tags)
         (:json-oembed meta-tags))
       (apply include-css (css-list ctx))
       [:link {:type "text/css" :href "/css/custom.css" :rel "stylesheet" :media "screen"}]
       [:link {:type "text/css" :href "/css/print.css" :rel "stylesheet" :media "print"}]
       [:link {:type "text/css", :href "https://fonts.googleapis.com/css?family=Halant:300,400,600,700|Dosis:300,400,600,700,800|Gochi+Hand|Coming+Soon|Oswald:400,300,700|Dancing+Script:400,700|Archivo+Black|Archivo+Narrow|Open+Sans:700,300,600,800,400|Open+Sans+Condensed:300,700|Cinzel:400,700&subset=latin,latin-ext", :rel "stylesheet"}]
       [:link {:rel "shortcut icon" :href (:icon favicons) }]
       [:link {:rel "icon" :type "image/png" :href  (:png favicons)}]

       [:script {:type "text/javascript"} (context-js ctx)]]
      [:body {:class (if (nil? (get-in ctx [:user])) "anon")}
       [:div#app]
       "<!--[if lt IE 10]>"
       (include-js "/assets/es5-shim/es5-shim.min.js" "/assets/es5-shim/es5-sham.min.js")
       "<![endif]-->"
       (include-js "/assets/es6-shim/es6-shim.min.js" "/assets/es6-shim/es6-sham.min.js")
       (apply include-js (js-list ctx))
       #_(include-js "https://backpack.openbadges.org/issuer.js")]))))


(defn main-response [ctx current-user flash-message meta-tags]
  (let [user (if current-user (-> (u/user-information ctx (:id current-user))
                                  (assoc :terms (:status (u/get-accepted-terms-by-id ctx (:id current-user))))
                                  (assoc  :real-id (:real-id current-user) ;;real-id is for admin login as user
                                          :last-visited (u/last-visited ctx (:id current-user)))))] ;;user's previous visit
    (-> (main-view (assoc ctx :user user :flash-message flash-message) meta-tags)
        (ok)
        (content-type "text/html; charset=\"UTF-8\""))))

(defn main [ctx path]
  (GET path []
    :no-doc true
    :summary "Main HTML layout"
    :current-user current-user
    :flash-message flash-message
    (main-response ctx current-user flash-message nil)))

(defn main-meta [ctx path plugin]
  (GET path []
    :no-doc true
    :path-params [id :- s/Any]
    :summary "Main with meta tags"
    :current-user current-user
    :flash-message flash-message
    (let [meta-tags (case plugin
                      :badge (b/meta-tags ctx id)
                      :page (p/meta-tags ctx id)
                      :user (u/meta-tags ctx id)
                      :gallery (g/meta-tags ctx id))]
      (main-response ctx current-user flash-message meta-tags))))
