(ns salava.user.ui.routes
  (:require [salava.core.ui.layout :as layout]
            [salava.core.i18n :as i18n :refer [t]]
            [salava.user.ui.login :as login]
            [salava.user.ui.activate :as password]
            [salava.user.ui.register :as register]))

(defn placeholder [content]
  (fn [site-navi params]
    #(layout/default site-navi content)))

(defn ^:export routes [context]
  {"/user" [["/login" login/handler]
            [["/login/" :next-url] login/handler]
            [["/activate/" :user-id "/" :timestamp "/" :code] password/handler]
            ["/register" register/handler]
            ["/account" (placeholder [:p "My account"])]]})

(defn ^:export navi [context] {})

