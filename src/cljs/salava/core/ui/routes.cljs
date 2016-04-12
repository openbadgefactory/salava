(ns salava.core.ui.routes
  (:require [salava.core.ui.layout :as layout]
            [salava.badge.ui.my :as my]))

(defn placeholder [content]
  (fn [site-navi params]
    #(layout/default site-navi content)))

(defn ^:export routes [context]
  {"/" [["" my/handler]]})


(defn ^:export navi [context] {})

