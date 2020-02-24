(ns salava.badgeIssuer.async
  (:require
   [salava.badgeIssuer.db :as db]
   [salava.core.util :refer [publish]]))

(defn subscribe [ctx]
  {:create (fn [data]
             (let [event-id (-> (db/insert-create-event! ctx data) :generated_key)]))
                ;(db/insert-selfie-event-owner! ctx (assoc data :event_id event-id))))
   :issue  (fn [data]
             (let [event-id (-> (db/insert-issue-event! ctx data) :generated_key)]
                 (db/insert-issue-event-owner! ctx (assoc data :event_id event-id))))})
