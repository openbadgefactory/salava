(ns salava.extra.application.async
  (:require [salava.extra.application.db :as db]
            [salava.core.util :refer [publish]]))

(defn subscribe [ctx]
  {:advert (fn [data] (let [event-id (:generated_key (db/insert-advert-event! ctx data))]
                        (db/insert-advert-owners! ctx (assoc data :event-id event-id))))})
