(ns salava.core.ui.routes
  (:require [salava.core.ui.layout :as layout]))

(defn placeholder [content]
  (fn [site-navi params]
    #(layout/default site-navi content)))

(defn ^:export routes [context]
  {"/" [[""   (placeholder (constantly [:p "Home page"]))]
        [true (placeholder (constantly [:p "404 Not Found"]))]]})


(defn ^:export navi [context] {})

