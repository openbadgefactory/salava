(ns salava.extra.application.event
  (:require [salava.extra.application.db :as db])
  )

(defn events [ctx user_id]
  (db/get-advert-events ctx user_id))
