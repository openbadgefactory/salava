(ns salava.admin.ui.helper
  (:require
   [reagent.session :as session]
   [salava.core.ui.helper :refer [input-valid?]]
   [salava.admin.schemas :as schemas]))



(defn valid-item-type? [item]
  (input-valid? (:item-type schemas/Url-parser) item))

(defn valid-item-id? [item]
  (input-valid? (:item-id schemas/Url-parser) (js/parseInt item))
  )

(defn checker [url]
  (let [url-list (vec(re-seq #"\w+" (str url) ))
        type (get url-list 1)
        id  (get url-list 3)]
    {:item-type (if (= type "gallery") "badges" type)
     :item-id id}))

(defn admin? []
  (let [role (session/get-in [:user :role])]
    (= role "admin")))
