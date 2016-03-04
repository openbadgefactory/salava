(ns salava.core.time
  #?(:clj
     (:require [clj-time.coerce :as c]
               [clj-time.format :as f])))

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
           "minutes" (str (.getDate date) "." (inc (.getMonth date)) "." (.getFullYear date) " - " (.getHours date) ":" (if (< (.getMinutes date) 10) (str "0" (.getMinutes date)) (.getMinutes date)))
           (str (.getDate date) "." (inc (.getMonth date)) "." (.getFullYear date))))
       :clj
       (let [date (c/from-long time)
             formatter (case format
                         "date" "dd.MM.yyyy"
                         "minutes" "dd.MM.yyyy - HH:mm"
                         "dd.MM.yyyy")]
         (f/unparse (f/formatter formatter) (c/from-long date))))))