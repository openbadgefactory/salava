(ns salava.extra.spaces.stats
 (:require
   [yesql.core :refer [defqueries]]
   [salava.core.util :as util :refer [get-db get-plugins]]
   [salava.core.time :refer [get-date-from-today]]
   [slingshot.slingshot :refer :all]
   [clojure.tools.logging :as log]))

(defqueries "sql/extra/spaces/stats.sql")

(defn user-stats
  "Get user statistics"
  [ctx space-id last-login]
  {:Totalusersno (total-user-count {:id space-id} (into {:result-set-fn first :row-fn :count} (get-db ctx)))
   :activatedusers (activated-user-count {:id space-id} (into {:result-set-fn first :row-fn :count} (get-db ctx)))
   :notactivatedusers (not-activated-user-count {:id space-id} (into {:result-set-fn first :row-fn :count} (get-db ctx)))
   :userssincelastlogin (count-registered-users-after-date {:id space-id :time last-login} (into {:result-set-fn first :row-fn :count} (get-db ctx)))
   :userssincelastmonth (count-registered-users-after-date {:id space-id :time (get-date-from-today -1 0 0)} (into {:result-set-fn first :row-fn :count} (get-db ctx)))
   :userssince3month (count-registered-users-after-date {:id space-id :time (get-date-from-today -3 0 0)} (into {:result-set-fn first :row-fn :count} (get-db ctx)))
   :userssince6month (count-registered-users-after-date {:id space-id :time (get-date-from-today -6 0 0)} (into {:result-set-fn first :row-fn :count} (get-db ctx)))
   :userssince1year (count-registered-users-after-date {:id space-id :time (get-date-from-today -12 0 0)} (into {:result-set-fn first :row-fn :count} (get-db ctx)))
   :internalusers (internal-user-count {:id space-id} (into {:result-set-fn first :row-fn :count} (get-db ctx)))
   :publicusers (public-user-count {:id space-id} (into {:result-set-fn first :row-fn :count} (get-db ctx)))})

(defn badge-stats
    "Get badge statistics"
    [ctx id last-login]
    (let [url-pattern (str (util/get-factory-url ctx) "%")]
     {:Totalbadgesno (count-all-badges {:id id} (into {:result-set-fn first :row-fn :count} (get-db ctx)))
       :pendingbadgescount (count-pending-badges {:id id} (into {:result-set-fn first :row-fn :count} (get-db ctx)))
       :acceptedbadgescount (count-accepted-badges {:id id} (into {:result-set-fn first :row-fn :count} (get-db ctx)))
       :declinedbadgescount (count-declined-badges {:id id} (into {:result-set-fn first :row-fn :count} (get-db ctx)))
       :badgessincelastlogin (count-all-badges-after-date {:id id :time last-login} (into {:result-set-fn first :row-fn :count} (get-db ctx)))
       :badgessincelastmonth (count-all-badges-after-date {:id id :time (get-date-from-today -1 0 0)} (into {:result-set-fn first :row-fn :count} (get-db ctx)))
       :badgessince3month (count-all-badges-after-date {:id id :time (get-date-from-today -3 0 0)} (into {:result-set-fn first :row-fn :count} (get-db ctx)))
       :badgessince6month (count-all-badges-after-date {:id id :time (get-date-from-today -6 0 0)} (into {:result-set-fn first :row-fn :count} (get-db ctx)))
       :badgessince1year (count-all-badges-after-date {:id id :time (get-date-from-today -12 0 0)} (into {:result-set-fn first :row-fn :count} (get-db ctx)))
       :privatebadgescount (count-private-badges {:id id} (into {:result-set-fn first :row-fn :count} (get-db ctx)))
       :publicbadgescount (count-public-badges {:id id} (into {:result-set-fn first :row-fn :count} (get-db ctx)))
       :internalbadgescount (count-internal-badges {:id id} (into {:result-set-fn first :row-fn :count} (get-db ctx)))
       :factorybadges (count-badges-issued-from-url {:id id :url url-pattern} (into {:result-set-fn first :row-fn :count} (get-db ctx)))}))

(defn pages-stats
  "get page statistics"
   [ctx id last-login]
   {:Totalpagesno (count-all-pages {:id id} (into {:result-set-fn first :row-fn :count} (get-db ctx)))
    :pagessincelastlogin (count-all-pages-after-date {:id id :time last-login} (into {:result-set-fn first :row-fn :count} (get-db ctx)))
    :pagessincelastmonth (count-all-pages-after-date {:id id :time (get-date-from-today -1 0 0)} (into {:result-set-fn first :row-fn :count} (get-db ctx)))
    :pagessince3month (count-all-pages-after-date {:id id :time (get-date-from-today -3 0 0)} (into {:result-set-fn first :row-fn :count} (get-db ctx)))
    :pagessince6month (count-all-pages-after-date {:id id :time (get-date-from-today -6 0 0)} (into {:result-set-fn first :row-fn :count} (get-db ctx)))
    :pagessince1year (count-all-pages-after-date {:id id :time (get-date-from-today -12 0 0)} (into {:result-set-fn first :row-fn :count} (get-db ctx)))
    :privatepagescount (count-private-pages {:id id} (into {:result-set-fn first :row-fn :count} (get-db ctx)))
    :publicpagescount (count-public-pages {:id id} (into {:result-set-fn first :row-fn :count} (get-db ctx)))
    :internalpagescount (count-internal-pages {:id id} (into {:result-set-fn first :row-fn :count} (get-db ctx)))})

(defn selfie-stats [ctx id last-login]
  {:created {:Totalcreatedno (created-badges-count {:id id} (into {:result-set-fn first :row-fn :count} (get-db ctx)))
             :createdsincelastlogin (created-badges-count-after-date {:id id :time last-login} (into {:result-set-fn first :row-fn :count} (get-db ctx)))
             :createdsincelastmonth (created-badges-count-after-date {:id id :time (get-date-from-today -1 0 0)} (into {:result-set-fn first :row-fn :count} (get-db ctx)))}
   :issued {:Totalissuedno (issued-badges-count {:id id} (into {:result-set-fn first :row-fn :count} (get-db ctx)))
            :issuedsincelastlogin (issued-badges-count-after-date {:id id :time last-login} (into {:result-set-fn first :row-fn :count} (get-db ctx)))
            :issuedsincelastmonth (issued-badges-count-after-date {:id id :time (get-date-from-today -1 0 0)} (into {:result-set-fn first :row-fn :count} (get-db ctx)))}})

(defn issuer-stats [ctx id last-login]
 {:Totalissuersno (count-badge-issuers {:id id} (into {:result-set-fn first :row-fn :count} (get-db ctx)))
  :issuerssincelastlogin (count-badge-issuers-after-date {:id id :time last-login} (into {:result-set-fn first :row-fn :count} (get-db ctx)))
  :issuerssincelastmonth (count-badge-issuers-after-date {:id id :time (get-date-from-today -1 0 0)} (into {:result-set-fn first :row-fn :count} (get-db ctx)))
  :issuerssince3month (count-badge-issuers-after-date {:id id :time (get-date-from-today -3 0 0)} (into {:result-set-fn first :row-fn :count} (get-db ctx)))
  :issuerssince6month (count-badge-issuers-after-date {:id id :time (get-date-from-today -6 0 0)} (into {:result-set-fn first :row-fn :count} (get-db ctx)))
  :issuerssince1year (count-badge-issuers-after-date {:id id :time (get-date-from-today -12 0 0)} (into {:result-set-fn first :row-fn :count} (get-db ctx)))})

(defn space-stats [ctx space-id last-login]
  (try+
   (as-> {:users (user-stats ctx space-id last-login)
          :userbadges (badge-stats ctx space-id last-login)
          :pages (pages-stats ctx space-id last-login)
          :issuers (issuer-stats ctx space-id last-login)} $
        ;:user-badge-correlation (user-badge-correlation ctx)}
         (if (some #(= % :badgeIssuer) (get-plugins ctx)) (merge $ (selfie-stats ctx space-id last-login)) (merge $ {})))
   (catch Object _
     (log/error (.getMessage _))
     "error")))
 
