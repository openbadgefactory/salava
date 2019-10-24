(ns salava.core.time
  #?(:clj
     (:require
      [clj-time.core :as t]
      [clj-time.coerce :as c]
      [clj-time.format :as f])))



(defn unix-time []
  (quot #?(:clj (System/currentTimeMillis)
           :cljs (.now js/Date))
        1000))

#_(defn- no-of-days-passed [event-time]
 (let [current-time (t/now)]
  (t/in-days (t/interval current-time event-time))))

(defn no-of-days-passed [event-time]
 #?(:clj (t/in-days (t/interval (c/from-long (* event-time 1000)) (t/now)))))

(defn get-day-of-week []
  #?(:clj (t/day-of-week (t/now))
     :cljs (.getDay (new js/Date))))

(defn get-date-from-today
  "today - months - weeks - days"
  [months weeks days]
  (quot #?(:clj (c/to-long (t/plus (t/today) (t/months months) (t/weeks weeks) (t/days days)))
           :cljs (.now js/Date)) ;TODO frontend get-date-from-today
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
           "months" (str (inc (.getMonth date)) " / " (.getFullYear date))
           "date" (str (.getDate date) "." (inc (.getMonth date)) "." (.getFullYear date))
           "minutes" (str (.getDate date) "." (inc (.getMonth date)) "." (.getFullYear date) " - " (.getHours date) ":" (if (< (.getMinutes date) 10) (str "0" (.getMinutes date)) (.getMinutes date)))
           (str (.getDate date) "." (inc (.getMonth date)) "." (.getFullYear date))))
       :clj
       (let [date (c/from-long time)
             formatter (case format
                         "date" "dd.MM.yyyy"
                         "minutes" "dd.MM.yyyy - HH:mm"
                         "months" "MM / yyyy"
                         "dd.MM.yyyy")]
         (f/unparse (f/formatter formatter) (c/from-long date))))))
