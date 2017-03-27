(ns salava.badge.db
  (:require [clojure.tools.logging :as log]
            [yesql.core :refer [defqueries]]
            [clojure.java.jdbc :as jdbc]
            [clojure.string :as string]
            [clojure.data.json :as json]
            [schema.core :as s]
            [salava.badge.schemas :as schemas] ;cljc
            [salava.core.util :as u]))

(defqueries "sql/badge/main.sql")

(defn- content-id [data]
  (u/map-sha256 (assoc data :id "")))


(defn- save-image [ctx item]
  (if (string/blank? (:image_file item))
    item
    (assoc item :image_file (u/file-from-url ctx (:image_file item)))))


(defn save-criteria-content! [ctx input]
  (s/validate schemas/CriteriaContent input)
  (let [id (content-id input)]
    (insert-criteria-content! (assoc input :id id) (u/get-db ctx))
    id))

(defn save-issuer-content! [ctx input]
  (s/validate schemas/IssuerContent input)
  (let [id (content-id input)]
    (insert-issuer-content! (assoc input :id id) (u/get-db ctx))
    id))

(defn save-creator-content! [ctx input]
  (when input
    (s/validate schemas/CreatorContent input)
    (let [id (content-id input)]
      (insert-creator-content! (assoc input :id id) (u/get-db ctx))
      id)))

(defn save-badge-content! [ctx input]
  (s/validate schemas/BadgeContent input)
  (let [id (content-id input)]
    (jdbc/with-db-transaction  [t-con (:connection (u/get-db ctx))]
      (insert-badge-content! (assoc input :id id) {:connection t-con})
      (doseq [tag (:tags input)]
        (insert-badge-content-tag! {:badge_content_id id :tag tag} {:connection t-con}))
      (doseq [a (:alignment input)]
        (insert-badge-content-alignment! (assoc a :badge_content_id id) {:connection t-con})))
    id))

;;

(defn save-badge! [ctx badge]
  (try
    (let [badge_content_id    (->> (:badge_content badge) (save-image ctx) (save-badge-content! ctx))
          issuer_content_id   (->> (:issuer_content badge) (save-image ctx) (save-issuer-content! ctx))
          criteria_content_id (save-criteria-content! ctx (:criteria_content badge))
          creator_content_id  (->> (:creator_content badge) (save-image ctx) (save-creator-content! ctx))]
      (-> badge
          (dissoc :badge_content :issuer_content :criteria_content :creator_content)
          (assoc    :badge_content_id badge_content_id
                   :issuer_content_id issuer_content_id
                 :criteria_content_id criteria_content_id
                  :creator_content_id creator_content_id)
          (insert-badge<! (u/get-db ctx))))
    (catch Exception ex
      (log/error "save-badge!: failed to save badge data")
      (log/error (.toString ex)))))
