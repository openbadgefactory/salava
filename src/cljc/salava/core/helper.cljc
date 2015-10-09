(ns salava.core.helper
  #?(:clj (:require
            [clojure.pprint :refer [pprint]])))

(defn dump [data]
  #?(:clj (pprint data)
     :cljs (.log js/console (pr-str data))))
