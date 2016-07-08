(ns salava.extra.passport.ui.routes
  (:require [salava.extra.passport.ui.footer :refer [footer]]))

(defn ^:export routes [context] {})

(defn ^:export navi [context]
  {"passport/footer" {:footer footer}})
