(ns salava.extra.spaces.cron
  (:require
   [clojure.tools.logging :as log]
   [salava.extra.spaces.db :as db]
   [salava.extra.spaces.space :as space]
   [salava.core.time :as time]
   [slingshot.slingshot :refer :all]))

(defn check-expired-spaces [ctx]
 (let [expired-spaces (db/expired-spaces ctx)]
   (when (seq expired-spaces)
     (log/info (str (count expired-spaces) " expired spaces found"))
     (try+
      (doseq [space expired-spaces]
       (log/info "updating space " (:name space) " status to expired")
       (space/update-status! ctx (:id space) "expired" 0))
      (catch Object _
        (log/error _))))))

(defn check-spaces [ctx]
 (let [deleted-spaces (db/deleted-spaces ctx)]
  (when (seq deleted-spaces)
   (log/info (str (count deleted-spaces) " deleted spaces found"))
   (try+
    (doseq [space deleted-spaces
            :let [days-since-delete (time/no-of-days-passed (long (:mtime space)))]]
      (when (= days-since-delete 1)
        (log/info "Performing hard delete on space id: " (:id space))
        (db/clear-space-data! ctx (:id space))
        (log/info "Finished deleting space info")))
    (catch Object _
     (log/error _))))))

(defn every-hour [ctx]
  (check-spaces ctx)
  (check-expired-spaces ctx))

(defn every-day [ctx]
  (check-expired-spaces ctx))
