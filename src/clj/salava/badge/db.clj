(ns salava.badge.db
  (:import (java.io StringReader))
  (:require [clojure.tools.logging :as log]
            [yesql.core :refer [defqueries]]
            [clojure.java.jdbc :as jdbc]
            [clojure.string :as string]
            [clojure.data.json :as json]
            [net.cgrand.enlive-html :as html]
            [schema.core :as s]
            [salava.badge.schemas :as schemas] ;cljc
            [salava.core.util :as u]))

(defqueries "sql/badge/main.sql")

(defn- content-id [data]
  (u/map-sha256 (assoc data :id "")))

(defn alt-markdown [^String input]
  (let [link-tags (-> (StringReader. input) (html/html-resource) (html/select [:head :link]))
        md-url (some #(when (and (= (:rel %) "alternate") (= (:type %) "text/x-markdown")) (:href %))
                     (map :attrs link-tags))]
    (try (u/http-get md-url) (catch Exception _ ""))))

(defn- content-type [_ input]
  (cond
    (map? input)          :default
    (string/blank? input) :blank
    (and (string? input) (re-find #"^https?://" input)) :url
    (and (string? input) (re-find #"^\s*\{" input))     :json
    (and (string? input) (re-find #"<.+>" input))       :html))


;;

(defmulti save-criteria-content! content-type)

(defmethod save-criteria-content! :blank [_ _]
  (throw (Exception. "badge/MissingCriteriaContent")))

(defmethod save-criteria-content! :url [ctx input]
  (save-criteria-content! ctx (u/http-get input)))

(defmethod save-criteria-content! :html [ctx input]
  (save-criteria-content!
    ctx {:id ""
         :html_content input
         :markdown_content (alt-markdown input)}))

(defmethod save-criteria-content! :default [ctx input]
  (s/validate schemas/CriteriaContent input)
  (let [id (content-id input)]
    (insert-criteria-content! (assoc input :id id) (u/get-db ctx))
    {:criteria_content_id id}))

;;

(defmulti save-issuer-content! content-type)

(defmethod save-issuer-content! :blank [_ _]
  (throw (Exception. "badge/MissingIssuerContent")))

(defmethod save-issuer-content! :url [ctx input]
  (save-issuer-content! ctx (u/http-get input)))

(defmethod save-issuer-content! :json [ctx input]
  (let [data (json/read-str input :key-fn keyword)]
    (save-issuer-content!
      ctx {:id ""
           :name        (:name data)
           :image_file  (if-not (string/blank? (:image data)) (u/file-from-url ctx (:image data)))
           :description (:description data)
           :url   (:url data)
           :email (:email data)
           :revocation_list_url (:revocationList data)})))

(defmethod save-issuer-content! :default [ctx input]
  (s/validate schemas/IssuerContent input)
  (let [id (content-id input)]
    (insert-issuer-content! (assoc input :id id) (u/get-db ctx))
    {:issuer_content_id id}))

;;

(defmulti save-creator-content! content-type)

(defmethod save-creator-content! :blank [_ _]
  (throw (Exception. "badge/MissingCreatorContent")))

(defmethod save-creator-content! :url [ctx input]
  (save-creator-content! ctx (with-meta (u/http-get input) {:json-url input})))

(defmethod save-creator-content! :json [ctx input]
  (let [data (json/read-str input :key-fn keyword)]
    (save-creator-content!
      ctx {:id ""
           :name        (:name data)
           :image_file  (if-not (string/blank? (:image data)) (u/file-from-url ctx (:image data)))
           :description (:description data)
           :url   (:url data)
           :email (:email data)
           :json_url ((meta input) :json-url "")})))

(defmethod save-creator-content! :default [ctx input]
  (s/validate schemas/CreatorContent input)
  (let [id (content-id input)]
    (insert-creator-content! (assoc input :id id) (u/get-db ctx))
    {:creator_content_id id}))

;;

(defmulti save-badge-content! content-type)

(defmethod save-badge-content! :blank [_ _]
  (throw (Exception. "badge/MissingBadgeContent")))

(defmethod save-badge-content! :url [ctx input]
  (save-badge-content! ctx (u/http-get input)))

(defmethod save-badge-content! :json [ctx input]
  (let [data (json/read-str input :key-fn keyword)]
    (merge
      (save-badge-content! ctx {:id ""
                                :name        (:name data)
                                :image_file  (u/file-from-url ctx (:image data))
                                :description (:description data)
                                :alignment   []
                                :tags        (:tags data)})
      (when (:issuer data)
        (save-issuer-content! ctx (:issuer data)))
      (when (:criteria data)
        (save-criteria-content! ctx (:criteria data)))
      (when (:extensions:OriginalCreator data)
        (save-creator-content! ctx (get-in data [:extensions:OriginalCreator :url]))))))

(defmethod save-badge-content! :default [ctx input]
  (s/validate schemas/BadgeContent input)
  (let [id (content-id input)]
    (jdbc/with-db-transaction  [t-con (:connection (u/get-db ctx))]
      (insert-badge-content! (assoc input :id id) {:connection t-con})
      (doseq [tag (:tags input)]
        (insert-badge-content-tag! {:badge_content_id id :tag tag} {:connection t-con}))
      (doseq [a (:alignment input)]
        (insert-badge-content-alignment! (assoc a :badge_content_id id) {:connection t-con})))
    {:badge_content_id id}))

