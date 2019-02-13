(ns salava.badge.endorsement
  (:require [yesql.core :refer [defqueries]]
            [salava.core.util :refer [get-db md->html]]
            [slingshot.slingshot :refer :all]
            [clojure.tools.logging :as log]
            [salava.badge.main :refer [send-badge-info-to-obf]]))

(defqueries "sql/badge/main.sql")

(defn external-id []
  (str "urn:uuid:" (java.util.UUID/randomUUID)))

(defn badge-owner? [ctx badge-id user-id]
  (let [owner (select-badge-owner {:id badge-id} (into {:result-set-fn first :row-fn :user_id} (get-db ctx)))]
    (= owner user-id)))

(defn endorsement-owner? [ctx endorsement-id user-id]
  (let [owner (select-endorsement-owner {:id endorsement-id} (into {:result-set-fn first :row-fn :endorser_id} (get-db ctx)))]
    (= owner user-id)))

(defn request-endorsement [])

(defn endorse! [ctx user-badge-id user-id content]
  (try+
    (if (badge-owner? ctx user-badge-id user-id)
      (throw+ {:status "error" :message "User cannot endorse himself"})
      (when-let [id (->> (insert-user-badge-endorsement<! {:user_badge_id user-badge-id
                                                           :external_id (external-id)
                                                           :endorser_id user-id
                                                           :content content} (get-db ctx))
                         :generated_key)]
        {:id id :status "success"}))
    (catch Object _
      (log/error _)
      {:status "error"})))

(defn delete! [ctx user-badge-id endorsement-id user-id]
  (try+
    (if (or (endorsement-owner? ctx endorsement-id user-id) (badge-owner? ctx user-badge-id user-id ))
      (do (delete-user-badge-endorsement! {:id endorsement-id} (get-db ctx))
        {:status "success"})
      {:status "error"}
      )
    (catch Object _
      (log/error _)
      {:status "error"})))


(defn update-status! [ctx user-id user-badge-id endorsement-id status]
  (try+
    (when (badge-owner? ctx user-badge-id user-id)
      (update-endorsement-status! {:id endorsement-id :status status} (get-db ctx))
      (case status
        "accepted" (send-badge-info-to-obf ctx user-badge-id user-id)
        "declined" (do
                     (delete! ctx user-badge-id endorsement-id user-id)
                     (send-badge-info-to-obf ctx user-badge-id user-id)))
      {:status "success"})
    (catch Object _
      (log/error _)
      {:status "error"})))

(defn user-badge-endorsements
  [ctx user-badge-id]
  (reduce (fn [r e]
            (conj r (-> e (update :content md->html)))
            ) [] (select-user-badge-endorsements {:user_badge_id user-badge-id} (get-db ctx))))

(defn received-pending-endorsements [ctx user-id]
  (map (fn [e] (-> e (update :content md->html))) (select-received-pending-endorsements {:user_id user-id} (get-db ctx))))

(defn all-recieved-endorsements [ctx user-id])
(defn all-given-endorsements [ctx endorser-id])



;Send information to obf
;Endorsements show in user profile
;Delete endorsement?
;GDPR
;badge-pdf
;see declined-endorsements???
;request-endorsement
;;edit-endorsement
;;should the endorsement be editable, if yes does the user have to accept or decline after every edit
;;update endorsement
