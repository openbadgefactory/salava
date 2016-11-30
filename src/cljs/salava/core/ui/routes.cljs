(ns salava.core.ui.routes
  (:require [salava.core.ui.layout :as layout]
            [salava.core.ui.helper :refer [base-path]]
            [salava.social.ui.stream :as s]
            [salava.core.ui.error :as err]))

(defn placeholder [content]
  (fn [site-navi params]
    #(layout/default site-navi content)))

(defn ^:export routes [context]
  {(str (base-path context)) [["" s/handler]
                              [["/error/" :status] err/handler]]})


(defn ^:export navi [context] {})

