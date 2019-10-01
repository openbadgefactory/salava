(ns salava.badge.endorsement
  (:require [yesql.core :refer [defqueries]]
            [salava.core.util :refer [get-db md->html get-full-path plugin-fun get-plugins]]
            [slingshot.slingshot :refer :all]
            [clojure.tools.logging :as log]
            [salava.badge.main :refer [send-badge-info-to-obf]]
            #_[salava.user.db :as user]))

(defqueries "sql/badge/main.sql")

(defn generate-external-id []
  (str "urn:uuid:" (java.util.UUID/randomUUID)))

(defn badge-owner? [ctx badge-id user-id]
  (let [owner (select-badge-owner {:id badge-id} (into {:result-set-fn first :row-fn :user_id} (get-db ctx)))]
    (= owner user-id)))

(defn endorsement-owner? [ctx endorsement-id user-id]
  (let [owner (select-endorsement-owner {:id endorsement-id} (into {:result-set-fn first :row-fn :issuer_id} (get-db ctx)))]
    (= owner user-id)))

(defn endorse! [ctx user-badge-id user-id content]
  (try+
    (if (badge-owner? ctx user-badge-id user-id)
      (throw+ {:status "error" :message "User cannot endorse himself"})
      (let [endorser-info-fn (first (plugin-fun (get-plugins ctx) "db" "user-information"))
             endorser-info (endorser-info-fn ctx user-id)#_(user/user-information ctx user-id)]
        (when-let [id (->> (insert-user-badge-endorsement<! {:user_badge_id user-badge-id
                                                             :external_id (generate-external-id)
                                                             :issuer_id user-id
                                                             :issuer_name (str (:first_name endorser-info) " " (:last_name endorser-info))
                                                             :issuer_url (str (get-full-path ctx) "/user/profile/" user-id)
                                                             :content content} (get-db ctx))
                           :generated_key)]
          {:id id :status "success"})))
    (catch Object _
      (log/error _)
      {:status "error"})))

(defn edit! [ctx user-badge-id endorsement-id content user-id]
  (try+
    (if (endorsement-owner? ctx endorsement-id user-id)
      (do
        (update-user-badge-endorsement! {:content content :id endorsement-id} (get-db ctx))
        {:status "success"})
      (throw+ {:status "error" :message "User cannot delete endorsement they do not own"}))
    (catch Object _
      {:status "error" :message _})))

(defn delete! [ctx user-badge-id endorsement-id user-id]
  (try+
    (if (or (endorsement-owner? ctx endorsement-id user-id) (badge-owner? ctx user-badge-id user-id))
      (do
        (delete-user-badge-endorsement! {:id endorsement-id} (get-db ctx))
        (send-badge-info-to-obf ctx user-badge-id user-id)
        {:status "success"})
      {:status "error"})

    (catch Object _
      (log/error _)
      {:status "error"})))


(defn update-status! [ctx user-id user-badge-id endorsement-id status]
  (try+
    (when (badge-owner? ctx user-badge-id user-id)
      (update-endorsement-status! {:id endorsement-id :status status} (get-db ctx))
      (case status
        "accepted" (send-badge-info-to-obf ctx user-badge-id user-id)
        "declined" (delete! ctx user-badge-id endorsement-id user-id)
        nil)
      {:status "success"})
    (catch Object _
      (log/error _)
      {:status "error"})))

(defn user-badge-endorsements
  ([ctx user-badge-id]
   (select-user-badge-endorsements {:user_badge_id user-badge-id} (get-db ctx)))
  ([ctx user-badge-id html?]
   (reduce (fn [r e]
             (conj r (-> e (update :content md->html)))) [] (user-badge-endorsements ctx user-badge-id))))

(defn received-pending-endorsements [ctx user-id]
  (map (fn [e] (-> e (update :content md->html))) (select-pending-endorsements {:user_id user-id} (get-db ctx))))

(defn endorsements-received
 ([ctx user-id]
  (map (fn [e] (-> e (update :content md->html)))
       (select-received-endorsements {:user_id user-id} (get-db ctx))))
 ([ctx user-id md?]
  (select-received-endorsements {:user_id user-id} (get-db ctx))))

(defn endorsements-given [ctx user-id]
  (select-given-endorsements {:user_id user-id} (get-db ctx)))

(defn all-user-endorsements [ctx user-id & md?]
  (let [received (if md? (endorsements-received ctx user-id true)(endorsements-received ctx user-id))
        given (endorsements-given ctx user-id)
        all (->> (list* given received) flatten (sort-by :mtime >))]
    {:given given
     :received received
     :all-endorsements all}))


#_(defn request-endorsement [ctx user-badge-id owner-id user-id request]
   (let [{:keys [name content email]} request]
    (try+
     (if-not (badge-owner? ctx user-badge-id owner-id)
       (throw+ {:status "error" :message "User cannot request endorsement for badge they do not own"})
       (let [endorser-info (as-> (first (plugin-fun (get-plugins ctx) "db" "user-information")) $
                                 (if $ ($ ctx user-id) {}))
             {:keys [first_name last_name]} endorser-info
             request-id (-> (request-endorsement<! {:id user-badge-id :content (:content request)} :issuer))]))
     (catch Object _
       (log/error _)
       {:status "error"}))))

;Endorsements show in user profile
;request-endorsement
