(ns salava.badgeIssuer.db
 (:require
  [clojure.data.json :as json]
  [clojure.string :refer [blank?]]
  [clojure.tools.logging :as log]
  ;[salava.badgeIssuer.util :refer [selfie-id]]
  [salava.core.util :refer [get-db file-from-url-fix get-full-path md->html]]
  [slingshot.slingshot :refer :all]
  [yesql.core :refer [defqueries]]))

(defqueries "sql/badgeIssuer/main.sql")

(defn user-selfie-badges [ctx user-id]
  (mapv
   (fn [s]
     (-> s (assoc :tags (if (blank? (:tags s)) nil (json/read-str (:tags s))))
           (update :criteria md->html)))
   (get-user-selfie-badges {:creator_id user-id} (get-db ctx))))

(defn user-selfie-badge [ctx user-id id]
  (get-selfie-badge {:id id} (get-db ctx)))

(defn selfie-badge [ctx id]
 (get-selfie-badge {:id id} (get-db ctx)))

(defn delete-selfie-badge-soft [ctx user-id id]
  (try+
    (hard-delete-selfie-badge! {:id id :creator_id user-id} (get-db ctx))
    {:status "success"}
    (catch Object _
      {:status "error"})))

(defn update-criteria-url! [ctx user-badge-id]
  (let [badge-id (select-badge-id-by-user-badge-id {:user_badge_id user-badge-id} (into {:result-set-fn first :row-fn :badge_id} (get-db ctx)))
        criteria_content_id (select-criteria-content-id-by-badge-id {:badge_id badge-id} (into {:result-set-fn first :row-fn :criteria_content_id} (get-db ctx)))
        url (str (get-full-path ctx) "/selfie/criteria/" criteria_content_id)]
   (update-badge-criteria-url! {:id criteria_content_id :url url} (get-db ctx))))

(defn finalise-user-badge! [ctx data]
  (finalise-issued-user-badge! data (get-db ctx)))

 
