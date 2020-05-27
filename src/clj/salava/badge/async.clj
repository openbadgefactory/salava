(ns salava.badge.async
  (:require [salava.badge.endorsement :as e]
            [salava.badge.ext-endorsement :as ext]
            [salava.core.util :refer [publish]]))

(defn subscribe [ctx]
  {:request_endorsement (fn [data] (let [event-id (:generated_key (e/insert-request-event! ctx data))]
                                     (e/insert-request-owner! ctx (assoc data :event_id event-id))))
   :endorse_badge (fn [data] (let [event-id (:generated_key (e/insert-endorse-event! ctx data))]
                                (e/insert-endorsement-owner! ctx (assoc data :event_id event-id))))
   :endorse_badge_ext (fn [data] (let [event-id (:generated_key (ext/insert-ext-endorse-event! ctx data))]
                                    (ext/insert-ext-endorsement-owner! ctx (assoc data :event_id event-id))))})
