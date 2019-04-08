(ns salava.location.db
  (:require [clojure.tools.logging :as log]
            [clojure.string :as string]
            [clojure.pprint :refer [pprint]]
            [clojure.java.jdbc :as jdbc]
            [yesql.core :refer [defqueries]]
            [salava.core.util :as u]
            [salava.location.country :as c]
            ))

(defqueries "sql/location/queries.sql")

(defn- fake-rand [seed]
  (let [x (* (Math/sin seed) 10000)]
    (- x (Math/floor x))))

(defn- noise
  ([seed v] (noise (inc seed) v 1))
  ([seed v multip]
   (let [op (if (even? seed) - +)]
     (op v (* (fake-rand seed) 0.0019 multip)))))

(defn set-location-reset [ctx user-id]
  (jdbc/with-db-transaction  [tx (:connection (u/get-db ctx))]
    {:success (and
                (boolean (update-user-location-public! {:user user-id :pub 0} {:connection tx}))
                (boolean (update-user-location! {:user user-id :lat nil :lng nil} {:connection tx}))
                (boolean (reset-user-badge-location! {:user user-id} {:connection tx})))
     }))

(defn set-location-public [ctx user-id public?]
  {:success (boolean (update-user-location-public! {:user user-id :pub (if public? 1 0)} (u/get-db ctx)))})

(defn set-user-badge-location [ctx user-id user-badge-id lat lng]
  {:success (boolean (update-user-badge-location! {:user user-id :badge user-badge-id :lat lat :lng lng} (u/get-db ctx)))})


(defn user-badge-location [ctx user-id user-badge-id]
  (if-let [loc (select-user-badge-location {:user user-id :badge user-badge-id} (u/get-db-1 ctx))]
    loc
    {:lat nil :lng nil}))

(defn set-user-location [ctx user-id lat lng]
  {:success (boolean (update-user-location! {:user user-id :lat lat :lng lng} (u/get-db ctx)))})


(defn user-enabled-location [ctx user-id logged-in?]
  (if logged-in?
    (or (select-user-location        {:user user-id} (u/get-db-1 ctx)) {:lat nil :lng nil})
    (or (select-user-location-public {:user user-id} (u/get-db-1 ctx)) {:lat nil :lng nil})))


(defn user-location [ctx user-id]
  (if-let [user (select-user {:user user-id} (u/get-db-1 ctx))]
    {:enabled (select-user-location {:user user-id} (u/get-db-1 ctx))
     :country (get c/lat-lng (keyword (:country user)) {:lat nil :lng nil})
     :public (pos? (:location_public user))}
    {:enabled nil
     :country {:lat nil :lng nil}
     :public false}))


(defn explore-badge [ctx badge-id]
  {:badges (->> (select-explore-badge {:badge badge-id} (u/get-db ctx))
                (map (fn [b]
                       (-> b
                           (assoc :lat (or (:badge_lat b) (noise (:id b) (:user_lat b)))
                                  :lng (or (:badge_lng b) (noise (:id b) (:user_lng b) 3)))
                           (dissoc :user_lat :badge_lat :user_lng :badge_lng)))))})


(defn explore-filters [ctx logged-in?]
  (let [opt (if logged-in?
              {:min_pub 0 :visibility ["public" "internal"]}
              {:min_pub 1 :visibility ["public"]})]
    {:tag_name    (sort (select-explore-taglist    opt (u/get-db-col ctx :tag)))
     :badge_name  (sort (select-explore-badgelist  opt (u/get-db-col ctx :name)))
     :issuer_name (sort (select-explore-issuerlist opt (u/get-db-col ctx :name)))}))


(defn explore-list-users [ctx logged-in? opt]
  (let [;; Get all user ids in provided map box
        user-ids
        (select-explore-user-ids-latlng (select-keys opt [:max_lat :max_lng :min_lat :min_lng]) (u/get-db-col ctx :id))
        ;; Build a list of filter functions from query parameters
        filters
        (cond-> []
          (not logged-in?)
          (conj (fn [ids] (select-explore-user-ids-public {:user ids} (u/get-db-col ctx :id))))

          (not (string/blank? (:user_name opt)))
          (conj (fn [ids] (select-explore-user-ids-name {:user ids :name (str "%" (:user_name opt) "%")} (u/get-db-col ctx :id))))
          )
        ;; Get final filtered user id list
        filtered-user-ids
        (reduce (fn [coll f] (if (seq coll) (f coll) [])) user-ids filters)
        ]

    (if (seq filtered-user-ids)
      {:users (map (fn [u] (assoc u :user_url (str (u/get-full-path ctx) "/user/profile/" (:id u))))
                   (select-explore-users {:user filtered-user-ids} (u/get-db ctx)))}
      {:users []})))


(defn explore-list-badges [ctx logged-in? opt]
  (let [;; Get all user_badge ids in provided map box
        badge-ids
        (select-explore-badge-ids-latlng (select-keys opt [:max_lat :max_lng :min_lat :min_lng]) (u/get-db-col ctx :id))
        ;; Build a list of filter functions from query parameters
        filters
        (cond-> []
          (not logged-in?)
          (conj (fn [ids] (select-explore-badge-ids-public {:badge ids} (u/get-db-col ctx :id))))

          (not (string/blank? (:tag_name opt)))
          (conj (fn [ids] (select-explore-badge-ids-tag {:badge ids :tag (:tag_name opt)} (u/get-db-col ctx :id))))

          (not (string/blank? (:badge_name opt)))
          (conj (fn [ids] (select-explore-badge-ids-name {:badge ids :name (str "%" (:badge_name opt) "%")} (u/get-db-col ctx :id))))

          (not (string/blank? (:issuer_name opt)))
          (conj (fn [ids] (select-explore-badge-ids-issuer {:badge ids :issuer (str "%" (:issuer_name opt) "%")} (u/get-db-col ctx :id))))
          )
        ;; Get final filtered user_badge id list
        filtered-badge-ids
        (reduce (fn [coll f] (if (seq coll) (f coll) [])) badge-ids filters)]

    (if (seq filtered-badge-ids)
      {:badges (->> (select-explore-badges {:badge filtered-badge-ids} (u/get-db ctx))
                    (map (fn [b]
                           (-> b
                               (assoc :badge_url   (str (u/get-full-path ctx) "/badge/info/" (:id b))
                                      :badge_image (str (u/get-site-url ctx) "/" (:badge_image b))
                                      :lat (or (:badge_lat b) (noise (:id b) (:user_lat b)))
                                      :lng (or (:badge_lng b) (noise (:id b) (:user_lng b) 3)))
                               (dissoc :user_lat :badge_lat :user_lng :badge_lng))

                           )))}
      {:badges []})))

