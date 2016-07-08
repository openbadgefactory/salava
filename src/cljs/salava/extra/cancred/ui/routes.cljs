(ns salava.extra.cancred.ui.routes
  (:require [salava.extra.cancred.ui.footer :refer [footer]]))

(defn ^:export routes [context] {})


(defn ^:export navi [context]
  {"cancred/footer" {:footer footer}})
