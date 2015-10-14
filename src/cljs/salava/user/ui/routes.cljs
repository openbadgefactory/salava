(ns salava.user.ui.routes
  (:require [salava.core.ui.layout :as layout]
            [salava.core.i18n :as i18n :refer [t]]))


(defn placeholder [content]
  (fn [site-navi params]
    #(layout/default site-navi content)))

(defn ^:export routes [context]
  {"/user" [["/login"   (placeholder [:p "Login page"])]
            ["/account" (placeholder [:p "My account"])]]})


(defn ^:export navi [context] {})

