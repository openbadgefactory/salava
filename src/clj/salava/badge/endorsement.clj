(ns salava.badge.endorsement
  (:require [yesql.core :refer [defqueries]]
            [salava.core.util :refer [get-db md->html get-full-path plugin-fun get-plugins event publish]]
            [slingshot.slingshot :refer :all]
            [clojure.tools.logging :as log]
            [salava.badge.main :refer [send-badge-info-to-obf badge-exists?]]
            [salava.core.time :as time]))

(defqueries "sql/badge/main.sql")
(defqueries "sql/badge/endorsement.sql")

(defn generate-external-id []
  (str "urn:uuid:" (java.util.UUID/randomUUID)))

(defn badge-owner? [ctx badge-id user-id]
  (let [owner (select-badge-owner {:id badge-id} (into {:result-set-fn first :row-fn :user_id} (get-db ctx)))]
    (= owner user-id)))

(defn endorsement-owner? [ctx endorsement-id user-id]
  (let [owner (select-endorsement-owner {:id endorsement-id} (into {:result-set-fn first :row-fn :issuer_id} (get-db ctx)))]
    (= owner user-id)))

(defn already-endorsed? [ctx user-badge-id user-id]
  (pos? (count (select-endorsement-by-issuerid-and-badgeid {:user_id user-id :id user-badge-id} (get-db ctx)))))

(defn insert-endorse-event! [ctx data]
 (insert-endorsement-event<! data (get-db ctx)))

(defn insert-endorsement-owner! [ctx data]
 (let [owner-id (select-endorsement-receiver-by-badge-id {:id (:object data)} (into {:result-set-fn first :row-fn :id} (get-db ctx)))]
   (insert-event-owner! (assoc data :object owner-id) (get-db ctx))))

(defn endorse! [ctx user-badge-id user-id content]
;;TODO check endorsment request first
  (try+
    (if-not (badge-exists? ctx user-badge-id)
     (throw+ {:status "error" :message (str "badge with id " user-badge-id " does not exist")})
     (if (badge-owner? ctx user-badge-id user-id)
       (throw+ {:status "error" :message "User cannot endorse himself"})
       (let [endorser-info-fn (first (plugin-fun (get-plugins ctx) "db" "user-information"))
              endorser-info (endorser-info-fn ctx user-id)]
         (when-let [id (->> (insert-user-badge-endorsement<! {:user_badge_id user-badge-id
                                                              :external_id (generate-external-id)
                                                              :issuer_id user-id
                                                              :issuer_name (str (:first_name endorser-info) " " (:last_name endorser-info))
                                                              :issuer_url (str (get-full-path ctx) "/user/profile/" user-id)
                                                              :content content} (get-db ctx))
                            :generated_key)]
           (publish ctx :endorse_badge {:subject user-id :verb "endorse_badge" :object id :type "badge"})
           {:id id :status "success"}))))
    (catch Object _
      (log/error _)
      _)))

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

(defn endorsement-requests [ctx user-id]
  (map (fn [r] (-> r (update :content md->html))) (select-endorsement-requests {:user_id user-id} (get-db ctx))))

(defn endorsement-requests-pending [ctx user-id]
  (->> (endorsement-requests ctx user-id) (filter #(= "pending" (:status %)))))

(defn all-user-endorsements [ctx user-id & md?]
  (let [received (if md? (endorsements-received ctx user-id true)(endorsements-received ctx user-id))
        given (endorsements-given ctx user-id)
        requests (->> (endorsement-requests ctx user-id) (filterv #(= (:status %) "pending")) (mapv #(assoc % :type "request")))
        sent-requests (some->> (select-sent-endorsement-requests {:id user-id} (get-db ctx)) (mapv #(assoc % :type "sent_request")))
        all (->> (list* given received requests sent-requests) flatten (sort-by :mtime >))]
    {:given given
     :received received
     :requests requests
     :sent-requests sent-requests
     :all-endorsements all}))

(defn insert-request-event! [ctx data]
 (insert-endorsement-request-event<! data (get-db ctx)))

(defn insert-request-owner! [ctx data]
 (let [owner-id (select-endorsement-request-owner-by-badge-id {:id (:object data)} (into {:result-set-fn first :row-fn :id} (get-db ctx)))]
  (insert-event-owner! (assoc data :object owner-id) (get-db ctx))))

(defn- request-sent?
  "Avoid request spamming; Check if request has previously been sent to user, returns map of request-id and status"
 [ctx user-badge-id user-id]
 (select-user-badge-endorsement-request-by-issuer-id {:user_badge_id user-badge-id :issuer_id user-id } (into {:result-set-fn first} (get-db ctx))))

(defn request-endorsement! [ctx user-badge-id owner-id user-ids content]
 (try+
  (if-not (badge-owner? ctx user-badge-id owner-id)
   (throw+ {:status "error" :message "User cannot request endorsement for a badge they do not own"})
   (doseq [id user-ids]
    (if-let [check (-> (request-sent? ctx user-badge-id id) :id)]
     (throw+ {:status "error" :message "Request already sent to user"})
     (let [endorser-info (as-> (first (plugin-fun (get-plugins ctx) "db" "user-information")) $
                              (if $ ($ ctx id) {}))
           {:keys [first_name last_name]} endorser-info
           user-connection (as-> (first (plugin-fun (get-plugins ctx) "db" "get-connections-user")) $
                                 (if $ (some-> ($ ctx owner-id id) :status) nil))
           request-id (-> (request-endorsement<! {:id user-badge-id
                                                  :content content
                                                  :issuer_name (str first_name " " last_name)
                                                  :issuer_id id
                                                  :issuer_url (str (get-full-path ctx) "/profile/" id)}  (get-db ctx))
                          :generated_key)]
       (publish ctx :request_endorsement {:subject owner-id :object request-id})
       (when-not user-connection (as-> (first (plugin-fun (get-plugins ctx) "db" "create-connections-user!")) $   ;;create user connection if not existing
                                       (when $ ($ ctx owner-id id))))))))
  {:status "success"}
  (catch Object ex
    (log/error ex)
    {:status "error"})))

(defn- request-owner? [ctx request-id user-id]
 (let [{:keys [id issuer_id]} (->> (select-endorsement-request-owner {:id request-id} (into {:result-set-fn first} (get-db ctx))))]
  (or (= id user-id) (= issuer_id user-id))))

(defn- delete-request! [ctx request-id user-id]
 (delete-endorsement-request! {:id request-id} (get-db ctx)))

(defn update-request-status!
 "Update endorsement request status, delete when request is declined"
 [ctx request-id status user-id]
 (try+
  (if (request-owner? ctx request-id user-id)
    (do
     (update-endorsement-request-status! {:id request-id :status status} (get-db ctx))
     (case status
      "declined" (delete-request! ctx request-id user-id)
       nil))
    (throw+ {:status "error" :message "User does not own request"}))
  {:status "success"}
  (catch Object ex
    (log/error ex)
    {:status "error"})))

(defn user-endorsements-status
 "Return endorsement interaction statuses based on user-badge-id"
 [ctx user-badge-id current-user-id user-id]
 {:received (select-user-received-endorsement-status {:id user-badge-id :issuer_id user-id} (into {:result-set-fn first :row-fn :status} (get-db ctx)))
  :request (select-user-endorsement-request-status {:id user-badge-id :issuer_id user-id} (into {:result-set-fn first :row-fn :status} (get-db ctx)))})

(defn pending-endorsement-count [ctx user-badge-id user-id]
 (pending-user-badge-endorsement-count {:id user-badge-id} (into {:result-set-fn first :row-fn :count} (get-db ctx))))

(defn accepted-endorsement-count [ctx user-badge-id user-id]
 {:user_endorsement_count (->> (select-accepted-badge-endorsements {:id user-badge-id}  (get-db ctx)) count)})

(defn endorsements-count [ctx user-badge-id user-id]
 {:pending_endorsements_count (pending-endorsement-count ctx user-badge-id user-id)
  :user_endorsement_count ""
  :endorsement_count ""})

(defn user-badge-pending-requests [ctx user-badge-id user-id]
 (sent-pending-requests-by-badge-id {:id user-badge-id} (get-db ctx)))

(defn delete-pending-request! [ctx user-badge-id]
 (let [requests (user-badge-pending-requests ctx user-badge-id nil)]
  (try+
   (doseq [r requests
           :let [days-pending (time/no-of-days-passed (long (:mtime r)))]]
    (when (>= days-pending 30)
      (log/info "Expired pending request id" (:id r))
      (log/info "Deleting pending request id " (:id r))
      (delete-request! ctx (:id r) nil)
      (log/info "Pending request deleted!")))
   (catch Object _
     (log/error _)))))
