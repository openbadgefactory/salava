(ns salava.admin.db
 (:require [yesql.core :refer [defqueries]]
            [clojure.set :refer [rename-keys]]
            [clojure.java.jdbc :as jdbc]
            [salava.core.helper :refer [dump]]
            [slingshot.slingshot :refer :all]
            [salava.core.util :refer [get-db]]
            [salava.core.time :refer [unix-time get-date-from-today]]
            [salava.core.mail :as m]))

(defqueries "sql/admin/queries.sql")

(defn user-admin?
  "Check if user is admin"
  [ctx user-id]
  (let [admin (select-user-admin {:id user-id} (into {:result-set-fn first :row-fn :role} (get-db ctx)))]
    (= admin "admin")))

(defn register-users-count
  "Get count from all active and registered users"
  [ctx]
  (total-user-count {} (into {:result-set-fn first :row-fn :count} (get-db ctx))))

(defn last-month-users-login-count
  "Get count from all last month logged in users"
  [ctx]
  (let [date (get-date-from-today -1 0 0)]
    (count-logged-users-after-date {:time date} (into {:result-set-fn first :row-fn :count} (get-db ctx)))))

(defn last-month-users-registered-count
  "Get count form all last month registered users"
  [ctx]
  (let [date (get-date-from-today -1 0 0)]
    (count-registered-users-after-date {:time date} (into {:result-set-fn first :row-fn :count} (get-db ctx)))))

(defn badges-count
  "Get count from all badges"
  [ctx]
  (count-all-badges {} (into {:result-set-fn first :row-fn :count} (get-db ctx))))

(defn last-month-added-badges-count
  "Get count from all last month added badges"
  [ctx]
  (let [date (get-date-from-today -1 0 0)]
    (count-all-badges-after-date {:time date} (into {:result-set-fn first :row-fn :count} (get-db ctx)))))

(defn pages-count
  "Get count from all pages have been created"
  [ctx]
  (count-all-pages {} (into {:result-set-fn first :row-fn :count} (get-db ctx))))

(defn get-stats [ctx]
  (try+
   {:register-users (register-users-count ctx)
   :last-month-active-users (last-month-users-login-count ctx)
   :last-month-registered-users (last-month-users-registered-count ctx)
   :all-badges (badges-count ctx)
   :last-month-added-badges (last-month-added-badges-count ctx)
   :pages (pages-count ctx)}
   (catch Object _
     "error")))

(defn private-badge! [ctx id]
  (try+
   (update-badge-visibility! {:id id} (get-db ctx))
   "success"
   (catch Object _
     "error")))


(defn private-badges! [ctx badge_content_id]
  (try+
   (update-badges-visibility!{:badge_content_id badge_content_id} (get-db ctx))
   "success"
   (catch Object _
     "error")))



(defn private-page! [ctx id]
  (try+
   (update-page-visibility! {:id id} (get-db ctx))
   "success"
   (catch Object _
     "error")))

(defn private-user! [ctx id]
  (try+
   (update-user-visibility! {:id id} (get-db ctx))
   "success"
   (catch Object _
     "error"
     )))

(defn ticket [ctx description report_type item_id item_url item_name item_type reporter_id item_content_id]
  (try+
   (insert-report-ticket<! {:description description :report_type report_type :item_id item_id :item_url item_url :item_name item_name :item_type item_type :reporter_id reporter_id :item_content_id item_content_id} (get-db ctx))
   "success"
   (catch Object _
     "error"
     )))

(defn get-tickets [ctx]
  (let [tickets (select-tickets {} (get-db ctx))]
    tickets))

(defn close-ticket! [ctx id]
  (try+
   (update-ticket-status! {:id id} (get-db ctx))
   "success"
   (catch Object _
     "error")))



(defn delete-badge! [ctx id  user-id subject message]
  (try+
   (let [user-id (select-user-id-by-badge-id {:id id}(into {:result-set-fn first :row-fn :user_id} (get-db ctx)))
         user (select-user-and-email {:id user-id} (into {:result-set-fn first} (get-db ctx)))]
     (m/send-mail ctx subject message [(:email user)])
     (update-badge-deleted! {:id id} (get-db ctx))
     )
   "success"
   (catch Object _
     "error")))

(defn delete-badges! [ctx badge-content-id subject message]
  (try+
   (let [user-ids (select-users-id-by-badge-content-id {:badge_content_id badge-content-id}(into {:row-fn :user_id} (get-db ctx)))
         users-email (select-users-email {:user_id user-ids} (into {:result-set-fn vec :row-fn :email} (get-db ctx)))]     
     (m/send-mail ctx subject message users-email)
     (update-badge-deleted! {:badge_content_id badge-content-id} (get-db ctx))
     
     )
   "success"
   (catch Object _
     "error")))

(defn delete-page! [ctx id user-id subject message]
  (try+
   (let [user (select-user-and-email {:id user-id} (into {:result-set-fn first} (get-db ctx)))]
     (dump (:email user))
     (m/send-mail ctx subject message [(:email user)])
     (update-page-deleted! {:id id} (get-db ctx))
     )
   "success"
   (catch Object _
     "error")))

(defn delete-user! [ctx user-id subject message]
  (try+
   (let [user (select-user-and-email {:id user-id} (into {:result-set-fn first} (get-db ctx)))]
     (dump (:email user))
     (m/send-mail ctx subject message [(:email user)])
     (update-user-pages-set-private! {:user_id user-id}(get-db ctx))
     (update-user-badges-set-private! {:user_id user-id}(get-db ctx))
     (delete-user-badge-congratulations! {:user_id user-id}(get-db ctx))
     (delete-user-badge-views! {:user_id user-id}(get-db ctx))
     (update-user-deleted! {:id user-id} (get-db ctx))
     )
   "success"
   (catch Object _
     "error")))

(defn send-message [ctx user_id subject message]
  (try+
   (let [user (select-user-and-email {:id user_id} (into {:result-set-fn first} (get-db ctx)))]
     (dump (:email user))
     (m/send-mail ctx subject message [(:email user)])
     )
   "success"
   (catch Object _
     "error")))



(defn get-user-name-and-primary-email [ctx user_id]
  (let [user (select-user-and-email {:id user_id} (into {:result-set-fn first} (get-db ctx)))]
    (hash-map :name (str (:first_name user) " " (:last_name user))
              :email (:email user))))


