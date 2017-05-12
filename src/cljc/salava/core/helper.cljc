(ns salava.core.helper
  #?(:clj (:require
            [clojure.pprint :refer [pprint]])))

(defn dump [data]
  #?(:clj (pprint data)
     :cljs (.log js/console (pr-str data))))

(defn private? [data]
  #?(:clj (get-in data [:config :core :private] false)
     :cljs false))

(defn plugin-str [plugin]
  (if (keyword? plugin)
    (subs (str plugin) 1)
    (str plugin)))

(defn string->number [str]
  #?(:clj (let [n (read-string str)]
            (if (number? n) n nil))))

