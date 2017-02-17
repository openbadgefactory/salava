(ns salava.core.components.pubsub
  (:require [clojure.tools.logging :as log]
            [com.stuartsierra.component :as component]
            [clojure.core.async :refer [chan pub sub go-loop <! close! unsub-all buffer]]
            [salava.core.util :as u]))

(defn- sub-loop [our-pub]
  (fn [[key fun]]
    (let [c (chan)]
      (sub our-pub key c)
      (go-loop []
               (try
                 (fun (<! c))
                 (catch Throwable ex
                   (log/error "pubsub/sub-loop: failed to execute handler" key)
                   (log/error (.toString ex))))
               (recur)))))

(defn- subscriptions [ctx our-pub]
  (let [funs (u/plugin-fun (u/get-plugins ctx) "async" "subscribe")
        register (fn [f] (doall (map (sub-loop our-pub) (f ctx))))]
    (mapcat register funs)))


(defrecord PubSub [config db channel]
  component/Lifecycle

  (start [this]
    (let [input-chan (chan)
          our-pub (pub input-chan :topic (fn [_] (buffer 100)))
          context {:config     (:config config)
                   :db         (:datasource db)
                   :input-chan input-chan}]
      (assoc this :channel {:input-chan input-chan
                            :sub (subscriptions context our-pub)
                            :pub our-pub})))

  (stop [this]
    (when channel
      (log/info "Closing pub/sub channels...")
      (unsub-all (:pub channel))
      (close! (:input-chan channel))
      (log/info "Closed"))
    (assoc this :channel nil)))


(defn create []
  (map->PubSub {}))
