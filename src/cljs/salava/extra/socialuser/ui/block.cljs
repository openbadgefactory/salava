(ns salava.extra.socialuser.ui.block
  (:require [reagent.core :refer [atom cursor]]
            [ajax.core :as ajax]
            [salava.core.helper :refer [dump]]
            [salava.core.ui.helper :refer [path-for]]
            [salava.core.i18n :refer [t]]
            [salava.extra.socialuser.ui.connect :as connect]
            [salava.extra.socialuser.ui.connections :as connections]
            [salava.extra.socialuser.ui.pending-connections :as pending-connections]
            [salava.extra.socialuser.ui.connection-configs :as connection-configs])
  )


(defn ^:export connectuser [user-id]
  (connect/handler user-id))


#_(defn ^:export connections []
  (connections/handler))

(defn ^:export userconnectionstats []
  (connections/stats))

(defn ^:export pendingconnections [parent-data]
  (pending-connections/handler parent-data))

(defn ^:export userconnectionconfig []
  (connection-configs/handler))
