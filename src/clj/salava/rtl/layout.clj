(ns salava.rtl.layout
(:require [salava.core.helper :refer [dump]]))

(defn html-attributes [ctx]
  (let [user-lang (keyword (get-in ctx [:user :language] "en"))
        dir (if (some #(= % user-lang) (get-in ctx [:config :rtl :rtl-languages])) "rtl" "ltr")]
    {:dir dir}))
