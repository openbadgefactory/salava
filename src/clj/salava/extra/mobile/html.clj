(ns salava.extra.mobile.html
  (:require [clojure.pprint :refer [pprint]]
            [salava.core.util :as u]
            [salava.core.layout :as layout]
            [clojure.string :as string]
            [salava.core.i18n :refer [t]]
            [hiccup.page :refer [html5 include-css include-js]]))

(defn wrapper
  [ctx content]
  (let [favicons (layout/favicon ctx)]
    (html5 [:head
            [:title (get-in ctx [:config :core :site-name])]
            [:meta {:charset "utf-8"}]
            [:meta {:http-equiv "X-UA-Compatible" :content "IE=edge"}]
            [:meta {:name "viewport" :content "width=device-width, initial-scale=1.0"}]
            [:meta {:property "og:sitename" :content (get-in ctx [:config :core :site-name])}]
            (apply include-css (layout/css-list ctx))
            [:link {:type "text/css" :href "/css/custom.css" :rel "stylesheet" :media "screen"}]
            [:link {:type "text/css" :href "/css/print.css" :rel "stylesheet" :media "print"}]
            [:link {:type "text/css", :href "https://fonts.googleapis.com/css?family=Halant:300,400,600,700|Dosis:300,400,600,700,800|Roboto|Gochi+Hand|Coming+Soon|Oswald:400,300,700|Dancing+Script:400,700|Archivo+Black|Archivo+Narrow|Open+Sans:700,300,600,800,400|Open+Sans+Condensed:300,700|Cinzel:400,700&subset=latin,latin-ext", :rel "stylesheet"}]
            [:link {:rel "shortcut icon" :href (:icon favicons) }]
            [:link {:rel "icon" :type "image/png" :href  (:png favicons)}]]

           [:body {:class "anon"}
            [:div#app content]])))

(defn site-picker-page
  [ctx sites query lang]
  (->> [:div.container.main-container
        [:h1 (t :extra-mobile/LoginToService lang)]
        [:p  (t :extra-mobile/LoginInfo lang)]
        (map (fn [s]
               [:div.list-group
                [:a.list-group-item {:href (str (:url s) "/app/user/oauth2/authorize?" query)}
                 [:h4.list-group-item-heading (:name s) " (" (:country s) ")"]
                 [:p.list-group-item-text {:style "margin-bottom: 8px"} (:url s)]
                 [:p.list-group-item-text (:description s)]]])
             sites)]
       (wrapper ctx)))
