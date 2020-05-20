(ns salava.core.components.cron
  (:require [com.stuartsierra.component :as component]
            [clj-time.core :as t]
            [salava.core.util :as u]))

(def running (atom false))

(defn- periodically [ctx fun]
  (doseq [task (u/plugin-fun (u/get-plugins ctx) "cron" fun)]
    (try
      (task ctx)
      (catch Throwable _))))

(defn- run-every-x [ctx x cond-fun]
  (fn []
    (while @running
      (let [now (t/now)
            h (t/hour now)
            m (t/minute now)]
        (when (cond-fun now)
          (periodically (assoc ctx :-cron {:hour h :minute m}) x)))
      (try
        (Thread/sleep 60000)
        (catch InterruptedException _)))))

(defn- start-loop [ctx]
  (let [d-th (Thread. (run-every-x ctx "every-day"    (fn [now] (and (= (t/hour now) 0) (= (t/minute now) 0)))))
        h-th (Thread. (run-every-x ctx "every-hour"   (fn [now] (= (t/minute now) 0))))
        m-th (Thread. (run-every-x ctx "every-minute" (fn [_]   true)))]
    (reset! running true)
    (.start d-th)
    (.start h-th)
    (.start m-th)
    (fn []
      (reset! running false)
      (.interrupt d-th)
      (.interrupt h-th)
      (.interrupt m-th)
      (.join d-th)
      (.join h-th)
      (.join m-th))))


(defrecord Cron [config db cron]
  component/Lifecycle

  (start [this]
    (let [context {:config (:config config)
                   :db     (:datasource db)}]
      (assoc this :cron (start-loop context))))

  (stop [this]
    (if cron
      (cron))
    (assoc this :cron nil)))


(defn create []
  (map->Cron {}))
