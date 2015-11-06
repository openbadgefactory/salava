(ns salava.core.time
  #?(:clj (:require [clj-time.coerce :as c])))

(defn unix-time []
  (quot #?(:clj (System/currentTimeMillis)
           :cljs (.now js/Date))
        1000))

(defn iso8601-to-unix-time [str]
  (quot #?(:clj (c/to-long str)
           :cljs (.parse js/Date str))
        1000))

(defn date-from-unix-time
  ([time] (date-from-unix-time time "date"))
  ([time format]
    #?(:cljs
       (let [date (js/Date. time)]
         (case format
           "date" (str (.getDate date) "." (inc (.getMonth date)) "." (.getFullYear date))
           "minutes" (str (.getDate date) "." (inc (.getMonth date)) "." (.getFullYear date) " - " (.getHours date) ":"(.getMinutes date))
           (str (.getDate date) "." (inc (.getMonth date)) "." (.getFullYear date)))))))

