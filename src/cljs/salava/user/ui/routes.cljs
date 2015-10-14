(ns salava.user.ui.routes
  (:require [salava.core.ui.layout :as layout]))


(defn placeholder [content]
  (fn [site-navi params]
    #(layout/default site-navi content)))

(defn ^:export routes [context]
  {"/user" [["/login"   (placeholder [:p "Login page"])]
            ["/account" (placeholder [:p "IMy account"])]]})


(defn ^:export navi [context] {})

(defn ^:export heading [context] {})
