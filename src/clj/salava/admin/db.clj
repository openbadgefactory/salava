(ns salava.admin.db
 (:require [yesql.core :refer [defqueries]]
            [clojure.set :refer [rename-keys]]
            [clojure.java.jdbc :as jdbc]
            [salava.core.helper :refer [dump]]
            [slingshot.slingshot :refer :all]
            [salava.core.util :refer [get-db]]
            [salava.core.time :refer [unix-time get-date-from-today]]
            [salava.core.countries :refer [all-countries sort-countries]]
            [salava.user.db :as u]
            [salava.badge.main :as b]
            [salava.page.main :as p]
            [salava.core.mail :as m]
            [salava.gallery.db :as g]))

(defqueries "sql/admin/queries.sql")



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
     (if (and (< 1 (count subject)) (< 1 (count message)))
       (m/send-mail ctx subject message users-email))
     
     (update-badge-deleted-by-badge-content-id! {:badge_content_id badge-content-id} (get-db ctx))
     
     )
   "success"
   (catch Object _
     "error")))

(defn delete-page! [ctx id user-id subject message]
  (try+
   (let [user (select-user-and-email {:id user-id} (into {:result-set-fn first} (get-db ctx)))]
     (if (and (< 1 (count subject)) (< 1 (count message)))
       (m/send-mail ctx subject message [(:email user)]))
     (update-page-deleted! {:id id} (get-db ctx))
     )
   "success"
   (catch Object _
     "error")))

(defn delete-user! [ctx user-id subject message email]
  (try+
   (if (and (< 1 (count subject)) (< 1 (count message)))
     (m/send-mail ctx subject message [email]))
   (update-user-pages-set-private! {:user_id user-id}(get-db ctx))
   (update-user-badges-set-private! {:user_id user-id}(get-db ctx))
   (delete-user-badge-congratulations! {:user_id user-id}(get-db ctx))
   (delete-user-badge-views! {:user_id user-id}(get-db ctx))
   (update-user-deleted! {:id user-id} (get-db ctx))
   "success"
   (catch Object _
     "error")))

(defn undelete-user! [ctx user-id]
  (try+
   (update-user-undeleted! {:id user-id} (get-db ctx))
   "success"
   (catch Object _
     "error")))

(defn send-message [ctx user_id subject message email]
  (try+
   (if (and (< 1 (count subject)) (< 1 (count message)))
       (m/send-mail ctx subject message [email]))
   "success"
   (catch Object _
     "error")))



(defn get-user-name-and-primary-email [ctx user_id]
  (let [user (select-user-and-email {:id user_id} (into {:result-set-fn first} (get-db ctx)))]
    (hash-map :name (str (:first_name user) " " (:last_name user))
              :email (:email user))))



(defn get-user [ctx user_id]
  (let [user (u/user-information-with-registered-and-last-login ctx user_id)
        emails (vec (u/email-addresses ctx user_id))]
    (hash-map :name (str (:first_name user) " " (:last_name user))
              :image_file (:profile_picture user)
              :item_owner_id (:id user)
              :item_owner (str (:first_name user) " " (:last_name user))
              :info {:emails emails
                     :last_login (:last_login user)
                     :ctime (:ctime user)
                     :deleted (:deleted user)})))



(defn get-badge-modal [ctx badgeid]
  (let [badge  (b/get-badge ctx badgeid nil)
        emails (vec (u/email-addresses ctx (:owner badge)))]
    (hash-map :name (:name badge)
              :image_file (:image_file badge)
              :item_owner_id (:owner badge) 
              :item_owner (str (:first_name badge) " " (:last_name badge))
              :info {:issuer_content_name (:issuer_content_name badge)
                     :issuer_content_url (:issuer_content_url badge)
                     :issuer_contact (:issuer_contact badge)
                     :issuer_image (:issuer_image badge)
                     :creator_name (:creator_name badge)
                     :creator_url (:creator_url badge)
                     :creator_email (:creator_email badge)
                     :creator_image (:creator_image badge)
                     :emails emails})))

(defn get-public-badge-content-modal [ctx badge-content-id]
  (let [badge (g/select-common-badge-content {:id badge-content-id} (into {:result-set-fn first} (get-db ctx)))
        badge-content (g/select-badge-criteria-issuer-by-date {:badge_content_id badge-content-id} (into {:result-set-fn first} (get-db ctx)))
        recipients (g/select-badge-recipients {:badge_content_id badge-content-id} (get-db ctx))]
    (hash-map :name (:name badge)
              :image_file (:image_file badge)
              :item_owner_id  (vec (map :id recipients))
              :item_owner (vec (map (fn [x] (str (:first_name x) " " (:last_name x))) recipients))
              :info {:issuer_content_name (:issuer_content_name badge-content)
                     :issuer_content_url (:issuer_content_url badge-content)
                     :issuer_contact (:issuer_contact badge-content)
                     :issuer_image (:issuer_image badge-content)
                     :creator_name (:creator_name badge-content)
                     :creator_url (:creator_url badge-content)
                     :creator_email (:creator_email badge-content)
                     :creator_image (:creator_image badge-content)
                     })))


(defn get-page-modal [ctx pageid]
  (let [page  (p/page-with-blocks ctx pageid)
        user (u/user-information ctx (:user_id page))
        emails (vec (u/email-addresses ctx (:user_id page)))]
    (hash-map :name (:name page)
              :image_file (:profile_picture user)
              :item_owner_id (:user_id page)
              :item_owner (str (:first_name page) " " (:last_name page))
              :info {:emails emails})))

(defn profile-countries [ctx user-id]
  (let [current-country (g/user-country ctx user-id)
        countries (g/select-profile-countries {} (into {:row-fn :country} (get-db ctx)))]
    (-> all-countries
        (select-keys (conj countries current-country))
        (sort-countries)
        (seq))))


(defn all-profiles
  "Search all user profiles by user's name, email and country"
  [ctx search-params user-id]
  (let [{:keys [name country order_by email filter]} search-params
        where ""
        order (case order_by
                "ctime" " ORDER BY ctime DESC"
                "name" " ORDER BY last_name, first_name"
                "")
        params []
        [where params] (if-not (or (empty? country) (= country "all"))
                         [(str where " AND country = ?") (conj params country)]
                         [where params])
        [where params] (if-not (empty? name)
                         [(str where " AND CONCAT(first_name,' ',last_name) LIKE ?") (conj params (str "%" name "%"))]
                         [where params])
        [where params] (if-not (empty? email)
                         [(str where " AND u.id in (SELECT user_id from user_email WHERE email LIKE ?)") (conj params (str "%" email "%"))]
                         [where params]
                         )
        [where params] (if (pos? filter)
                         [(str where " AND u.deleted = ?") (conj params filter)]
                         [where params])
        query (str "SELECT u.id, u.first_name, u.last_name, u.country, u.ctime, u.deleted, GROUP_CONCAT(ue.email,' ', ue.primary_address) AS email FROM user AS u
JOIN user_email AS ue ON ue.user_id = u.id
WHERE (profile_visibility = 'public' OR profile_visibility = 'internal') "
                   where
                   " GROUP BY u.id, u.first_name, u.last_name, u.country, u.ctime, u.deleted "
                   order
                   " LIMIT 50"
                   )
        profiles (jdbc/with-db-connection
                   [conn (:connection (get-db ctx))]
                   (jdbc/query conn (into [query] params)))]
    (->> profiles
         (take 50))))



