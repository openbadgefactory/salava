(ns salava.mobile.db
  (:require [clojure.tools.logging :as log]
            [clojure.string :as string]
            [clojure.pprint :refer [pprint]]
            [clojure.data.json :as json]
            [yesql.core :refer [defqueries]]
            [salava.factory.db :as factory]
            [salava.core.util :as u]))


(defqueries "sql/mobile/main.sql")

(defn- png-convert-url [ctx image]
  (if (string/blank? image)
    ""
    (if (re-find #"\w+\.svg$" image)
      (str (u/get-full-path ctx) "/obpv1/file/as-png?image=" image)
      (str (u/get-site-url ctx) "/" image))))



(defn user-badges-all [ctx user_id]
  {:badges (->> (select-user-badges-all {:user_id user_id} (u/get-db ctx))
                (map  (fn [b]
                        (-> b
                            (update :tags #(if (string/blank? %) [] (string/split % #",")))
                            (update :revoked pos?)
                            (update :image_file #(png-convert-url ctx %))
                            (update :issuer_image_file #(png-convert-url ctx %))
                            (update :creator_image_file #(png-convert-url ctx %))
                            ))))})

(defn user-badge
  "Get badge by id"
  [ctx user-badge-id user_id]
  (let [badge (select-user-badge {:id user-badge-id :user_id user_id} (u/get-db-1 ctx))]
    (some-> badge
            (merge (select-badge-detail-count {:id user-badge-id} (u/get-db-1 ctx)))
            (assoc :content (->> (select-badge-content {:badge_id (:badge_id badge)} (u/get-db ctx))
                                 (map (fn [c]
                                        (assoc c :alignment (select-badge-content-alignments
                                                              {:badge_id (:badge_id badge)
                                                               :language (:language_code c)}
                                                              (u/get-db ctx)))))))
            (update :tags #(if (string/blank? %) [] (string/split % #",")))
            (update :revoked pos?)
            (update :show_recipient_name pos?)
            (update :image_file #(png-convert-url ctx %))
            (update :issuer_image_file #(png-convert-url ctx %))
            (update :creator_image_file #(png-convert-url ctx %))

            (assoc :share_url (str (u/get-full-path ctx) "/badge/info/" user-badge-id))
            )))


(defn pending-badges-first [ctx user_id]
  (factory/save-pending-assertion-first ctx user_id)
  {:id (some->> (select-user-badges-all {:user_id user_id} (u/get-db ctx))
                (filter #(= "pending" (:status %)))
                first :id)})


(defn user-badge-endorsements [ctx user-badge-id user_id]
  {:badge  (->> (select-user-badge-endorsements {:id user-badge-id :user_id user_id} (u/get-db ctx))
                (map  (fn [b]
                        (-> b
                            (update :issuer_image_file #(png-convert-url ctx %))))))

   :issuer (->> (select-user-issuer-endorsements {:id user-badge-id :user_id user_id} (u/get-db ctx))
                (map  (fn [b]
                        (-> b
                            (update :issuer_image_file #(png-convert-url ctx %))))))

   :user   (->> (select-user-endorsements {:id user-badge-id :user_id user_id} (u/get-db ctx))
                (map  (fn [b]
                        (-> b
                            (update :id str)
                            (update :issuer_image_file #(png-convert-url ctx %))
                            (assoc :issuer_email "")
                            (assoc :issuer_description "")))))})

(defn user-badge-congratulations
  "Get badge by id, public route"
  [ctx badge-id user-id]
  {:congratulations (->> (select-user-badge-congratulations
                           {:user_badge_id badge-id :owner user-id} (u/get-db ctx))
                         (map (fn [c]
                                (-> c
                                    (update :profile_picture #(if (string/blank? %) % (str (u/get-site-url ctx) "/" %)))
                                    ))))})




(defn gallery-badge [ctx gallery_id badge_id]
  (let [badge (select-gallery-badge {:gallery_id gallery_id :badge_id badge_id} (u/get-db-1 ctx))]
    (some-> badge
            (assoc :content (->> (select-badge-content {:badge_id (:badge_id badge)} (u/get-db ctx))
                                 (map (fn [c]
                                        (assoc c :alignment (select-badge-content-alignments
                                                              {:badge_id (:badge_id badge)
                                                               :language (:language_code c)}
                                                              (u/get-db ctx)))))))
            (update :image_file #(png-convert-url ctx %))
            (update :issuer_image_file #(png-convert-url ctx %))
            (update :creator_image_file #(png-convert-url ctx %))
            )))
