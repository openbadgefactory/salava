(ns salava.badgeIssuer.util
  (:require
   [salava.core.util :refer [bytes->base64 get-db-1]]
   [yesql.core :refer [defqueries]])
  (:import
    [java.io ByteArrayOutputStream]
    [javax.imageio ImageIO]))

(defqueries "sql/badgeIssuer/main.sql")

(defn is-badge-creator? [ctx id user-id]
  (let [creator-id (-> (get-selfie-badge-creator {:id id} (get-db-1 ctx)) :creator_id)]
    (= user-id creator-id)))

(defn selfie-id []
  (str (java.util.UUID/randomUUID)))

(defn image->base64str [canvas]
  (let [out (ByteArrayOutputStream.)]
    (ImageIO/write canvas "png" out)
    (str "data:image/png;base64," (bytes->base64 (.toByteArray out)))))
