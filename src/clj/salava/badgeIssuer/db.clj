(ns salava.badgeIssuer.db
 (:require
  [clojure.data.json :as json]
  [clojure.string :refer [blank?]]
  [clojure.tools.logging :as log]
  [salava.badgeIssuer.util :refer [selfie-id]]
  [salava.core.util :refer [get-db file-from-url-fix]]
  [slingshot.slingshot :refer :all]
  [yesql.core :refer [defqueries]]))

(defqueries "sql/badgeIssuer/main.sql")

(defn save-selfie-badge [ctx data user-id]
  (try+
    (let [id (if-not (blank? (:id data))
               (:id data)
               (selfie-id))
          image (if (re-find #"^data:image" (:image data))
                    (file-from-url-fix ctx (:image data))
                    (:image data))
          tags (if (seq (:tags data)) (json/write-str (:tags data)) "")]

      (insert-selfie-badge<! (assoc data
                                    :id id
                                    :creator_id user-id
                                    :image image
                                    :tags tags)
                             (get-db ctx))
      {:status "success" :id id})
    (catch Object _
      (log/error (.getMessage _))
      {:status "error" :id "-1"})))

(defn user-selfie-badges [ctx user-id]
  (mapv
   (fn [s]
     (-> s (assoc :tags (if (blank? (:tags s)) nil (json/read-str (:tags s))))))
   (get-user-selfie-badges {:creator_id user-id} (get-db ctx))))

(defn user-selfie-badge [ctx user-id id]
  (get-selfie-badge {:id id} (get-db ctx)))

(defn delete-selfie-badge-soft [ctx user-id id]
  (try+
    (hard-delete-selfie-badge! {:id id :creator_id user-id} (get-db ctx))
    {:status "success"}
    (catch Object _
      {:status "error"})))

(defn update-assertions-info! [ctx data]
  (update-user-badge-assertions! data (get-db ctx)))

(defn badge-assertion [ctx id]
  (some-> (get-assertion-json {:id id} (into {:result-set-fn first :row-fn :assertion_json} (get-db ctx)))
      (json/read-str :key-fn keyword)))

(defn badge-criteria [ctx id])
