(ns salava.rtl.layout
(:require [salava.core.helper :refer [dump]]))

(defn set-page-direction [ctx]
  (let [user-lang (get-in ctx [:user :language])
        rtl?  (contains? (set (get-in ctx [:config :core :plugins])) :rtl)]
    (if (and (= user-lang "ar") rtl?) {:dir "rtl"} {})))
