(ns salava.page.main
  (:require [yesql.core :refer [defqueries]]
            [salava.core.time :refer [unix-time]]
            [salava.core.i18n :refer [t]]
            [salava.core.helper :refer [dump]]
            [salava.core.util :refer [get-db]]
            [salava.badge.main :as b]))

(defqueries "sql/page/main.sql")

(def themes-available
  {:name " " :left-bg "" :right-pg ""})

(defn user-pages-all [ctx user-id]
  "Get all user pages"
  (select-user-pages {:user_id user-id} (get-db ctx)))

(defn create-empty-page! [ctx user-id]
  (:generated_key (insert-empty-page<! {:user_id user-id
                                        :name    (t :page/Untitled)} (get-db ctx))))

(defn page-blocks [ctx page-id]
  (let [badge-blocks (select-pages-badge-blocks {:page_id page-id} (get-db ctx))
        file-blocks (select-pages-file-blocks {:page_id page-id} (get-db ctx))
        heading-blocks (select-pages-heading-blocks {:page_id page-id} (get-db ctx))
        html-blocks (select-pages-html-blocks {:page_id page-id} (get-db ctx))
        blocks (concat badge-blocks file-blocks heading-blocks html-blocks)]
    (sort-by :block_order blocks)))

(defn badge-blocks-for-edit [ctx page-id]
  (let [blocks (select-pages-badge-blocks {:page_id page-id} (get-db ctx))]
    (map #(hash-map :id (:id %)
                    :type (:type %)
                    :block_order (:block_order %)
                    :badge {:id (:badge_id %)
                            :name (:name %)
                            :image_file (:image_file %)}) blocks)))

(defn page-blocks-for-edit [ctx page-id]
  (let [badge-blocks (badge-blocks-for-edit ctx page-id)
        file-blocks (select-pages-file-blocks {:page_id page-id} (get-db ctx))
        heading-blocks (select-pages-heading-blocks {:page_id page-id} (get-db ctx))
        html-blocks (select-pages-html-blocks {:page_id page-id} (get-db ctx))
        blocks (concat badge-blocks file-blocks heading-blocks html-blocks)]
    (sort-by :block_order blocks)))

(defn page-with-blocks [ctx page-id]
  (let [page (select-page {:id page-id} (into {:result-set-fn first} (get-db ctx)))
        blocks (page-blocks ctx page-id)]
    (assoc page :blocks blocks)))

(defn page-for-edit [ctx page-id]
  (let [page (select-keys (select-page {:id page-id} (into {:result-set-fn first} (get-db ctx))) [:id :user_id :name :description])
        blocks (page-blocks-for-edit ctx page-id)
        owner (:user_id page)
        badges (map #(select-keys % [:id :name :image_file]) (b/user-badges-all ctx owner))]
    {:page (assoc page :blocks blocks) :badges badges}))

(defn page-settings [ctx page-id]
  (select-page {:id page-id} (get-db ctx)))