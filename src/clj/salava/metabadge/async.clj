(ns salava.metabadge.async
  (:require [clojure.tools.logging :as log]
            [clojure.string :as string]
            [clojure.pprint :refer [pprint]]
            [salava.metabadge.metabadge :as m]))

(defn subscribe [ctx]
  {:new-pending-badge (fn [data] (m/pending-metabadge? ctx data (:id data)))})
