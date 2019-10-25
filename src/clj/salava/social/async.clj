(ns salava.social.async
  (:require [salava.social.db :as db]
            [salava.core.util :refer [publish]]))

(defn subscribe [ctx]
  {:event (fn [data] (db/insert-social-event! ctx data))})
