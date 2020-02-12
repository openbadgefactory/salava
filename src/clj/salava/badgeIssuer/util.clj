(ns salava.badgeIssuer.util
  (:require
   [salava.core.util :refer [bytes->base64 get-db-1 get-db]]
   [yesql.core :refer [defqueries]])
  (:import
    [java.io ByteArrayOutputStream]
    [javax.imageio ImageIO]))

(defqueries "sql/badgeIssuer/main.sql")

(defn is-badge-creator? [ctx id user-id]
  (let [creator-id (-> (get-selfie-badge-creator {:id id} (get-db-1 ctx)) :creator_id)]
    (= user-id creator-id)))

(defn is-badge-issuer? [ctx user-badge-id issuer-id]
  (= issuer-id (select-selfie-issuer-by-badge-id {:id user-badge-id} (into {:result-set-fn first :row-fn :issuer_id} (get-db ctx)))))

(defn issuable-from-gallery? [ctx badge_id]
  (check-badge-issuable {:id badge_id} (into {:result-set-fn first :row-fn :issuable_from_gallery} (get-db ctx))))

(defn badge-valid?
  "Check is badge exists, has been deleted by owner or is revoked"
  [ctx user-badge-id]
  (some-> (select-issued-badge-validity-status {:id user-badge-id} (into {:result-set-fn first} (get-db ctx)))))

(defn selfie-id []
  (str (java.util.UUID/randomUUID)))

(defn image->base64str [canvas]
  (let [out (ByteArrayOutputStream.)]
    (ImageIO/write canvas "png" out)
    (str "data:image/png;base64," (bytes->base64 (.toByteArray out)))))
