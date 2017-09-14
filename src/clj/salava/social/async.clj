(ns salava.social.async
  (:require [salava.social.db :as db]
            [salava.core.util :refer [publish]]))



(defn subscribe [ctx]
  {:event (fn [data] (let [event-id (:generated_key (db/insert-social-event! ctx data))]
                       (db/insert-event-owners! ctx (assoc data :event-id event-id))))})
